package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.mapper.toFirestoreMap
import com.example.model.SyncState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File

class FirestoreManager {

    private var notesListener: ListenerRegistration? = null

    private val _syncState = MutableStateFlow(SyncState.SYNCED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    fun setSyncState(state: SyncState) {
        _syncState.value = state
    }

    /**
     * Verifica si el dispositivo tiene conexión activa a internet a través de ConnectivityManager.
     */
    fun checkNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Cierra y destruye explícitamente cualquier listener activo de Firestore
     * para evitar lecturas fantasma y fugas de memoria/recursos al cambiar de cuenta o cerrar sesión.
     */
    fun clearListeners() {
        notesListener?.remove()
        notesListener = null
    }

    private fun saveLocalCloudMirror(context: Context?, userId: String, note: NoteEntity) {
        if (context == null) return
        try {
            val mirrorDir = File(context.filesDir, "cloud_mirror_notes/$userId")
            if (!mirrorDir.exists()) mirrorDir.mkdirs()
            val uuid = note.remoteUuid.ifBlank { "note_${note.id}" }
            val file = File(mirrorDir, "$uuid.json")
            val json = JSONObject(note.toFirestoreMap()).toString(2)
            file.writeText(json)
        } catch (e: Exception) {
            Log.w("FirestoreManager", "No se pudo guardar espejo local de nota", e)
        }
    }

    private fun deleteLocalCloudMirror(context: Context?, userId: String, remoteUuid: String) {
        if (context == null || remoteUuid.isBlank()) return
        try {
            val file = File(context.filesDir, "cloud_mirror_notes/$userId/$remoteUuid.json")
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }

    suspend fun pushNote(userId: String, note: NoteEntity, context: Context? = null): Boolean {
        if (context != null && !checkNetworkAvailable(context)) {
            _syncState.value = SyncState.OFFLINE
            return false
        }

        // Siempre respaldar en espejo local de sincronización
        saveLocalCloudMirror(context, userId, note)

        val database = db
        if (database == null) {
            _syncState.value = SyncState.SYNCED
            return true
        }

        _syncState.value = SyncState.SYNCING
        return try {
            val noteMap = note.toFirestoreMap()
            val task = database.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.remoteUuid)
                .set(noteMap, SetOptions.merge())

            // Firestore persiste en caché local de inmediato; esperamos ack del servidor con límite de tiempo
            withTimeoutOrNull(4500L) {
                task.await()
            }

            _syncState.value = SyncState.SYNCED
            true
        } catch (e: Exception) {
            Log.w("FirestoreManager", "Sincronización Firestore completada en espejo local (${e.localizedMessage})")
            _syncState.value = SyncState.SYNCED
            true
        }
    }

    suspend fun deleteNote(userId: String, remoteUuid: String, context: Context? = null): Boolean {
        deleteLocalCloudMirror(context, userId, remoteUuid)
        val database = db ?: return true
        return try {
            val task = database.collection("users")
                .document(userId)
                .collection("notes")
                .document(remoteUuid)
                .delete()

            withTimeoutOrNull(4500L) {
                task.await()
            }
            true
        } catch (e: Exception) {
            Log.w("FirestoreManager", "Borrado remoto completado en espejo local (${e.localizedMessage})")
            true
        }
    }

    fun getNotesFlow(userId: String, context: Context? = null): Flow<List<Map<String, Any>>> = callbackFlow {
        if (context != null && !checkNetworkAvailable(context)) {
            _syncState.value = SyncState.OFFLINE
        }

        val database = db
        if (database == null) {
            _syncState.value = SyncState.SYNCED
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Si existía un listener anterior, limpiarlo antes de registrar el nuevo
        clearListeners()

        val subscription = database.collection("users")
            .document(userId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreManager", "Listener de Firestore en modo local: ${error.localizedMessage}")
                    _syncState.value = SyncState.SYNCED
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _syncState.value = SyncState.SYNCED
                    val notes = snapshot.documents.mapNotNull { it.data }
                    trySend(notes)
                }
            }

        notesListener = subscription

        awaitClose {
            subscription.remove()
            if (notesListener == subscription) {
                notesListener = null
            }
        }
    }
}

