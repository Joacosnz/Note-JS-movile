package com.example.data.remote

import com.example.data.local.entity.NoteEntity
import com.example.data.remote.mapper.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private var notesListener: ListenerRegistration? = null

    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    /**
     * Cierra y destruye explícitamente cualquier listener activo de Firestore
     * para evitar lecturas fantasma y fugas de memoria/recursos al cambiar de cuenta o cerrar sesión.
     */
    fun clearListeners() {
        notesListener?.remove()
        notesListener = null
    }

    suspend fun pushNote(userId: String, note: NoteEntity): Boolean {
        val database = db ?: return false
        return try {
            val noteMap = note.toFirestoreMap()

            database.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.remoteUuid)
                .set(noteMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun getNotesFlow(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val database = db
        if (database == null) {
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
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
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
