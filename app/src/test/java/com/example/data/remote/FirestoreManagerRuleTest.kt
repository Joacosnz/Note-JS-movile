package com.example.data.remote

import android.os.Looper
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.mapper.toFirestoreCreateMap
import com.example.data.remote.mapper.toFirestoreUpdateMap
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf

class FirestoreManagerRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var manager: FirestoreManager

    @Before
    fun setUp() {
        manager = FirestoreManager(firestore)
    }

    @Test
    fun saveAndFetchNotes_asAuthenticatedOwner_succeeds() {
        val uid = signInTestUser("owner@example.com")
        val note = NoteEntity(
            id = 42,
            remoteUuid = "note-uuid-42",
            userId = uid,
            title = "Nota de prueba",
            content = "Contenido sincronizado entre dispositivos",
            colorIndex = 2,
            isPinned = true,
            folder = "Trabajo"
        )

        val saveTask = firestore
            .collection("users")
            .document(uid)
            .collection("notes")
            .document(note.remoteUuid)
            .set(note.toFirestoreCreateMap())
        awaitTask(saveTask)

        val getSnap = awaitTask(
            firestore
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(note.remoteUuid)
                .get()
        )
        assertNotNull(getSnap)
        assertTrue(getSnap!!.exists())
        assertEquals("Nota de prueba", getSnap.getString("title"))
        assertEquals(uid, getSnap.getString("userId"))

        // Update path
        val updatedNote = note.copy(title = "Nota actualizada")
        awaitTask(
            firestore
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(note.remoteUuid)
                .update(updatedNote.toFirestoreUpdateMap())
        )

        // List path
        val listSnap = awaitTask(
            firestore
                .collection("users")
                .document(uid)
                .collection("notes")
                .get()
        )
        assertEquals(1, listSnap!!.documents.size)
        assertEquals("Nota actualizada", listSnap.documents.first().getString("title"))

        // Delete path
        awaitTask(
            firestore
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(note.remoteUuid)
                .delete()
        )
    }

    @Test
    fun writeNote_toAnotherUsersCollection_isDeniedByRules() {
        val aliceUid = signInTestUser("alice@example.com")
        val bobUid = "bob-different-uid"
        val note = NoteEntity(
            id = 99,
            remoteUuid = "note-uuid-99",
            userId = aliceUid,
            title = "Intrusión",
            content = "No permitido"
        )

        try {
            awaitTask(
                firestore
                    .collection("users")
                    .document(bobUid)
                    .collection("notes")
                    .document(note.remoteUuid)
                    .set(note.toFirestoreCreateMap())
            )
            fail("Expected PERMISSION_DENIED when writing to another user's notes collection")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun getNotesFlow_whenUnauthenticated_closesWithPermissionDenied() = runTest {
        signOutTestUser()
        val deferred = async(kotlinx.coroutines.Dispatchers.Default) {
            try {
                manager.getNotesFlow("some-uid").first()
                fail("Expected PERMISSION_DENIED for unauthenticated listener")
            } catch (e: FirebaseFirestoreException) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
            }
        }
        val deadline = System.currentTimeMillis() + 10_000L
        while (!deferred.isCompleted) {
            if (System.currentTimeMillis() > deadline) {
                deferred.cancel()
                fail("Timed out waiting for unauthenticated getNotesFlow to close with PERMISSION_DENIED")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        deferred.await()
    }
}
