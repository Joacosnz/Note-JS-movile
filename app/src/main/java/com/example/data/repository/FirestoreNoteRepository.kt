package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.mapper.toFirestoreCreateMap
import com.example.data.remote.mapper.toFirestoreUpdateMap
import com.example.data.remote.mapper.toNoteEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository class dedicated to handling Firestore CRUD operations for notes
 * in the user-scoped `notes` collection (`users/{userId}/notes`).
 */
class FirestoreNoteRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        throw IllegalStateException("FirebaseAuth is not initialized")
    }
) {
    constructor(context: Context) : this(
        firestore = createNamedFirestore(context),
        auth = FirebaseAuth.getInstance()
    )

    companion object {
        const val USERS_COLLECTION = "users"
        const val NOTES_COLLECTION = "notes"

        private fun createNamedFirestore(context: Context): FirebaseFirestore {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.fromResource(appContext)
                if (options != null) {
                    FirebaseApp.initializeApp(appContext, options)
                } else {
                    val dummyOptions = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:321abc")
                        .setProjectId("dummy-project")
                        .setApiKey("dummy-api-key")
                        .build()
                    FirebaseApp.initializeApp(appContext, dummyOptions)
                }
            }
            val databaseId = appContext.getString(R.string.firestore_database_id)
            return FirebaseFirestore.getInstance(databaseId)
        }
    }

    private fun requireUserId(explicitUserId: String? = null): String {
        val resolved = explicitUserId?.takeIf {
            it.isNotBlank() && it != NoteEntity.LOCAL_ANONYMOUS_USER_ID
        } ?: auth.currentUser?.uid
        return checkNotNull(resolved) {
            "User must be authenticated to perform Firestore note operations"
        }
    }

    private fun notesCollection(userId: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTES_COLLECTION)
    }

    private fun notesCollectionPath(userId: String): String {
        return "$USERS_COLLECTION/$userId/$NOTES_COLLECTION"
    }

    /**
     * Adds a new note document to the `notes` collection in Firestore.
     * Uses `FieldValue.serverTimestamp()` for `createdAt` and `updatedAt`.
     */
    suspend fun addNote(
        note: NoteEntity,
        userId: String? = null
    ): NoteEntity {
        val resolvedUid = requireUserId(userId ?: note.userId)
        val collection = notesCollection(resolvedUid)
        val noteId = note.remoteUuid.ifBlank {
            UUID.randomUUID().toString()
        }
        val docRef = collection.document(noteId)
        val now = System.currentTimeMillis()
        val normalizedNote = note.copy(
            remoteUuid = noteId,
            userId = resolvedUid,
            isSynced = true,
            pendingDelete = false,
            lastModifiedLocal = note.lastModifiedLocal.takeIf { it > 0L } ?: now,
            updatedAt = now
        )

        try {
            docRef.set(normalizedNote.toFirestoreCreateMap()).await()
            return normalizedNote
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Convenience overload to create and add a new note directly from basic fields.
     */
    suspend fun addNote(
        title: String,
        content: String,
        folder: String = "Notas",
        colorIndex: Int = 0,
        isPinned: Boolean = false,
        userId: String? = null
    ): NoteEntity {
        val resolvedUid = requireUserId(userId)
        val now = System.currentTimeMillis()
        val newNote = NoteEntity(
            remoteUuid = UUID.randomUUID().toString(),
            userId = resolvedUid,
            title = title,
            content = content,
            folder = folder,
            colorIndex = colorIndex,
            isPinned = isPinned,
            createdAt = now,
            updatedAt = now,
            lastModifiedLocal = now
        )
        return addNote(newNote, resolvedUid)
    }

    /**
     * Reads a single note document by its document ID (`remoteUuid`) from Firestore.
     * Returns `null` if the document does not exist.
     */
    suspend fun getNoteById(
        noteId: String,
        userId: String? = null
    ): NoteEntity? {
        require(noteId.isNotBlank()) { "noteId must not be blank" }
        val resolvedUid = requireUserId(userId)
        val docRef = notesCollection(resolvedUid).document(noteId)
        return try {
            val snapshot = docRef.get().await()
            snapshot.toNoteEntityOrNull(resolvedUid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    /**
     * Reads all note documents in the user's `notes` collection once.
     */
    suspend fun getAllNotes(
        userId: String? = null
    ): List<NoteEntity> {
        val resolvedUid = requireUserId(userId)
        val collection = notesCollection(resolvedUid)
        val path = notesCollectionPath(resolvedUid)
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toNoteEntityOrNull(resolvedUid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    /**
     * Observes real-time updates to all notes in the user's `notes` collection.
     */
    fun observeNotes(
        userId: String? = null
    ): Flow<List<NoteEntity>> = callbackFlow {
        val resolvedUid = try {
            requireUserId(userId)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val collection = notesCollection(resolvedUid)
        val path = notesCollectionPath(resolvedUid)

        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val notes = snapshot.documents.mapNotNull { doc ->
                    doc.toNoteEntityOrNull(resolvedUid)
                }
                trySend(notes)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Observes real-time updates to a single note document in the user's `notes` collection.
     */
    fun observeNoteById(
        noteId: String,
        userId: String? = null
    ): Flow<NoteEntity?> = callbackFlow {
        val resolvedUid = try {
            requireUserId(userId)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val docRef = notesCollection(resolvedUid).document(noteId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toNoteEntityOrNull(resolvedUid))
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Updates an existing note document in Firestore while preserving immutable fields
     * (`remoteUuid`, `userId`, `createdAt`) and refreshing `updatedAt` with server timestamp.
     */
    suspend fun updateNote(
        note: NoteEntity,
        userId: String? = null
    ): NoteEntity {
        val noteId = note.remoteUuid
        require(noteId.isNotBlank()) { "Cannot update a note without a valid remoteUuid" }
        val resolvedUid = requireUserId(userId ?: note.userId)
        val docRef = notesCollection(resolvedUid).document(noteId)
        val now = System.currentTimeMillis()
        val normalizedNote = note.copy(
            userId = resolvedUid,
            isSynced = true,
            lastModifiedLocal = note.lastModifiedLocal.takeIf { it > 0L } ?: now,
            updatedAt = now
        )

        try {
            docRef.update(normalizedNote.toFirestoreUpdateMap()).await()
            return normalizedNote
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Performs a partial update of specific fields on an existing note document in Firestore.
     */
    suspend fun updateNoteFields(
        noteId: String,
        title: String? = null,
        content: String? = null,
        folder: String? = null,
        tags: String? = null,
        colorIndex: Int? = null,
        isPinned: Boolean? = null,
        isArchived: Boolean? = null,
        isTrashed: Boolean? = null,
        userId: String? = null
    ) {
        require(noteId.isNotBlank()) { "noteId must not be blank" }
        val resolvedUid = requireUserId(userId)
        val docRef = notesCollection(resolvedUid).document(noteId)

        val updates = mutableMapOf<String, Any>(
            "lastModifiedLocal" to System.currentTimeMillis(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (title != null) updates["title"] = title.take(5000)
        if (content != null) updates["content"] = content.take(500000)
        if (folder != null) updates["folder"] = folder.take(200)
        if (tags != null) updates["tags"] = tags.take(5000)
        if (colorIndex != null) updates["colorIndex"] = colorIndex
        if (isPinned != null) updates["isPinned"] = isPinned
        if (isArchived != null) updates["isArchived"] = isArchived
        if (isTrashed != null) updates["isTrashed"] = isTrashed

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Deletes a note document by its ID (`remoteUuid`) from Firestore.
     */
    suspend fun deleteNote(
        noteId: String,
        userId: String? = null
    ) {
        require(noteId.isNotBlank()) { "noteId must not be blank" }
        val resolvedUid = requireUserId(userId)
        val docRef = notesCollection(resolvedUid).document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun DocumentSnapshot.toNoteEntityOrNull(fallbackUserId: String): NoteEntity? {
        if (!exists()) return null
        val rawData = data ?: return null
        val mutableData = rawData.toMutableMap()
        if ((mutableData["remoteUuid"] as? String).isNullOrBlank()) {
            mutableData["remoteUuid"] = id
        }
        return mutableData.toNoteEntity(fallbackUserId = fallbackUserId)
    }
}
