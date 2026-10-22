package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    suspend fun pushNote(userId: String, note: NoteEntity, context: Context? = null): Boolean {
        if (context != null && !checkNetworkAvailable(context)) {
            _syncState.value = SyncState.OFFLINE
            return false
        }

        val database = db
        if (database == null) {
            _syncState.value = SyncState.OFFLINE
            return false
        }

        _syncState.value = SyncState.SYNCING
        return try {
            val noteMap = note.toFirestoreMap()

            database.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.remoteUuid)
                .set(noteMap, SetOptions.merge())
                .await()

            _syncState.value = SyncState.SYNCED
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = SyncState.OFFLINE
            false
        }
    }

    suspend fun deleteNote(userId: String, remoteUuid: String): Boolean {
        val database = db ?: return false
        return try {
            database.collection("users")
                .document(userId)
                .collection("notes")
                .document(remoteUuid)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getNotesFlow(userId: String, context: Context? = null): Flow<List<Map<String, Any>>> = callbackFlow {
        if (context != null && !checkNetworkAvailable(context)) {
            _syncState.value = SyncState.OFFLINE
        }

        val database = db
        if (database == null) {
            _syncState.value = SyncState.OFFLINE
            close(IllegalStateException("Firebase is not initialized"))
            return@callbackFlow
        }

        // Si existía un listener anterior, limpiarlo antes de registrar el nuevo
        clearListeners()

        val subscription = database.collection("users")
            .document(userId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncState.value = SyncState.OFFLINE
                    close(error)
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
