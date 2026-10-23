package com.example.data.repository

import android.os.Looper
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.entity.NoteEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf

class FirestoreNoteRepositoryTest : FirestoreEmulatorTestBase() {

    private lateinit var repository: FirestoreNoteRepository

    @Before
    fun setUp() {
        repository = FirestoreNoteRepository(firestore = firestore, auth = auth)
    }

    private fun <T> runSuspendWithMainLooper(timeoutMs: Long = 10_000L, block: suspend CoroutineScope.() -> T): T = runBlocking {
        val deferred = async(Dispatchers.Default) {
            block()
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!deferred.isCompleted) {
            if (System.currentTimeMillis() > deadline) {
                deferred.cancel()
                throw AssertionError("Operation timed out after ${timeoutMs}ms")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        deferred.await()
    }

    @Test
    fun crudOperations_addReadUpdateAndDeleteNote_succeedsForOwner() {
        val uid = signInTestUser("owner@example.com")

        // 1. Add note
        val created = runSuspendWithMainLooper {
            repository.addNote(
                title = "Primera nota en Firestore",
                content = "Contenido inicial de prueba",
                folder = "Proyectos",
                colorIndex = 3,
                isPinned = true
            )
        }
        assertTrue(created.remoteUuid.isNotBlank())
        assertEquals(uid, created.userId)
        assertEquals("Primera nota en Firestore", created.title)

        // 2. Read single note by ID
        val fetched = runSuspendWithMainLooper {
            repository.getNoteById(created.remoteUuid)
        }
        assertNotNull(fetched)
        assertEquals("Primera nota en Firestore", fetched!!.title)
        assertEquals("Contenido inicial de prueba", fetched.content)
        assertEquals("Proyectos", fetched.folder)
        assertEquals(3, fetched.colorIndex)
        assertTrue(fetched.isPinned)

        // 3. Update full note
        val updatedInput = fetched.copy(
            title = "Nota actualizada",
            content = "Contenido modificado",
            isPinned = false
        )
        runSuspendWithMainLooper {
            repository.updateNote(updatedInput)
        }

        val afterFullUpdate = runSuspendWithMainLooper {
            repository.getNoteById(created.remoteUuid)
        }
        assertNotNull(afterFullUpdate)
        assertEquals("Nota actualizada", afterFullUpdate!!.title)
        assertEquals("Contenido modificado", afterFullUpdate.content)
        assertEquals(false, afterFullUpdate.isPinned)

        // 4. Partial field update
        runSuspendWithMainLooper {
            repository.updateNoteFields(
                noteId = created.remoteUuid,
                title = "Título parcial",
                folder = "Archivo"
            )
        }

        // 5. Read all notes and observe real-time Flow
        val allNotes = runSuspendWithMainLooper {
            repository.getAllNotes()
        }
        assertEquals(1, allNotes.size)
        assertEquals("Título parcial", allNotes.first().title)
        assertEquals("Archivo", allNotes.first().folder)

        val observedNotes = runSuspendWithMainLooper {
            repository.observeNotes().first()
        }
        assertEquals(1, observedNotes.size)
        assertEquals(created.remoteUuid, observedNotes.first().remoteUuid)

        // 6. Delete note
        runSuspendWithMainLooper {
            repository.deleteNote(created.remoteUuid)
        }

        val afterDelete = runSuspendWithMainLooper {
            repository.getNoteById(created.remoteUuid)
        }
        assertNull(afterDelete)
    }

    @Test
    fun addNote_toAnotherUsersCollection_isDeniedBySecurityRules() {
        val aliceUid = signInTestUser("alice@example.com")
        val bobUid = "bob-uid-999"

        val note = NoteEntity(
            remoteUuid = "intrusion-note-1",
            userId = aliceUid,
            title = "No autorizado",
            content = "Intento de escritura cruzada"
        )

        try {
            runSuspendWithMainLooper {
                repository.addNote(note = note, userId = bobUid)
            }
            fail("Expected PERMISSION_DENIED when adding a note to another user's collection")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }
}
