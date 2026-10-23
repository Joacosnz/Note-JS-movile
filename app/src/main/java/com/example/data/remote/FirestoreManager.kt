package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.R
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.mapper.toFirestoreCreateMap
import com.example.data.remote.mapper.toFirestoreMap
import com.example.data.remote.mapper.toFirestoreUpdateMap
import com.example.model.SyncState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.remote.mapper.toNoteEntity
import java.io.File
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreManager(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(createFirestore(context))

    companion object {
        private fun createFirestore(context: Context): FirebaseFirestore {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            val databaseId = appContext.getString(R.string.firestore_database_id)
            return FirebaseFirestore.getInstance(databaseId)
        }
    }

    private var notesListener: ListenerRegistration? = null

    private val _syncState = MutableStateFlow(SyncState.SYNCED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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

        saveLocalCloudMirror(context, userId, note)

        val noteId = note.remoteUuid.ifBlank { return false }
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        _syncState.value = SyncState.SYNCING
        return try {
            val normalizedNote = note.copy(userId = userId, remoteUuid = noteId)
            if (normalizedNote.updatedAtServer > 0L) {
                try {
                    docRef.update(normalizedNote.toFirestoreUpdateMap()).await()
                } catch (updateEx: Exception) {
                    if (updateEx is CancellationException) throw updateEx
                    docRef.set(normalizedNote.toFirestoreCreateMap()).await()
                }
            } else {
                try {
                    docRef.set(normalizedNote.toFirestoreCreateMap()).await()
                } catch (createEx: Exception) {
                    if (createEx is CancellationException) throw createEx
                    docRef.update(normalizedNote.toFirestoreUpdateMap()).await()
                }
            }
            _syncState.value = SyncState.SYNCED
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            _syncState.value = SyncState.OFFLINE
            false
        }
    }

    suspend fun deleteNote(userId: String, remoteUuid: String, context: Context? = null): Boolean {
        if (remoteUuid.isBlank()) return true
        deleteLocalCloudMirror(context, userId, remoteUuid)
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(remoteUuid)
        return try {
            docRef.delete().await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            false
        }
    }

    suspend fun addNote(userId: String, note: NoteEntity, context: Context? = null): NoteEntity {
        val noteId = note.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val normalizedNote = note.copy(userId = userId, remoteUuid = noteId, isSynced = true)
        saveLocalCloudMirror(context, userId, normalizedNote)

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        _syncState.value = SyncState.SYNCING
        try {
            docRef.set(normalizedNote.toFirestoreCreateMap()).await()
            _syncState.value = SyncState.SYNCED
            return normalizedNote
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            _syncState.value = SyncState.OFFLINE
            throw e
        }
    }

    suspend fun updateNote(userId: String, note: NoteEntity, context: Context? = null): NoteEntity {
        val noteId = note.remoteUuid
        require(noteId.isNotBlank()) { "Cannot update a note without a valid remoteUuid" }
        val normalizedNote = note.copy(userId = userId, remoteUuid = noteId, isSynced = true)
        saveLocalCloudMirror(context, userId, normalizedNote)

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        _syncState.value = SyncState.SYNCING
        try {
            docRef.update(normalizedNote.toFirestoreUpdateMap()).await()
            _syncState.value = SyncState.SYNCED
            return normalizedNote
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            _syncState.value = SyncState.OFFLINE
            throw e
        }
    }

    suspend fun readNote(userId: String, remoteUuid: String): NoteEntity? {
        val data = getRemoteNoteById(userId, remoteUuid) ?: return null
        val mutable = data.toMutableMap()
        if ((mutable["remoteUuid"] as? String).isNullOrBlank()) {
            mutable["remoteUuid"] = remoteUuid
        }
        return mutable.toNoteEntity(fallbackUserId = userId)
    }

    suspend fun getRemoteNoteById(userId: String, remoteUuid: String): Map<String, Any>? {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(remoteUuid)
        return try {
            val snapshot = docRef.get().await()
            snapshot.data
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun fetchNotesOnce(userId: String): List<Map<String, Any>> {
        val collectionRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
        return try {
            val snapshot = collectionRef.get().await()
            _syncState.value = SyncState.SYNCED
            snapshot.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    if ((this["remoteUuid"] as? String).isNullOrBlank()) {
                        this["remoteUuid"] = doc.id
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$userId/notes")
            throw e
        }
    }

    fun getNotesFlow(userId: String, context: Context? = null): Flow<List<Map<String, Any>>> = callbackFlow {
        if (context != null && !checkNetworkAvailable(context)) {
            _syncState.value = SyncState.OFFLINE
        }

        clearListeners()

        val path = "users/$userId/notes"
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    _syncState.value = SyncState.OFFLINE
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _syncState.value = if (context != null && !checkNetworkAvailable(context)) {
                        SyncState.OFFLINE
                    } else {
                        SyncState.SYNCED
                    }
                    val notes = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toMutableMap()?.apply {
                            if ((this["remoteUuid"] as? String).isNullOrBlank()) {
                                this["remoteUuid"] = doc.id
                            }
                        }
                    }
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
