package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.FirestoreManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteRepositorySyncTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.clearInstancesForTest()
        val options = FirebaseOptions.Builder()
            .setProjectId("demo-no-project")
            .setApplicationId("1:1234567890:android:abcdef")
            .setApiKey("fake-api-key-for-test")
            .build()
        FirebaseApp.initializeApp(context, options)

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val firestoreManager = FirestoreManager(context)
        repository = NoteRepository(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            signatureDao = database.signatureDao(),
            firestoreManager = firestoreManager,
            context = context
        )
    }

    @After
    fun tearDown() {
        database.close()
        FirebaseApp.clearInstancesForTest()
    }

    @Test
    fun processRemoteNotesList_insertsUpdatesAndDeletesNotesInRoomForOfflineAvailability() = runTest {
        val userId = "user-sync-123"

        // 1. Remote note created in Firestore -> should be inserted into local Room DB
        val initialRemoteSnapshot = listOf(
            mapOf<String, Any>(
                "remoteUuid" to "remote-note-1",
                "userId" to userId,
                "title" to "Nota desde Firestore",
                "content" to "[{\"type\":\"text\",\"id\":\"b1\",\"content\":\"Contenido inicial\",\"style\":\"BODY\"}]",
                "folder" to "Proyectos",
                "tags" to "cloud,offline",
                "colorIndex" to 3,
                "isPinned" to true,
                "lastModifiedLocal" to 1000L,
                "updatedAtServer" to 1000L
            ),
            mapOf<String, Any>(
                "remoteUuid" to "remote-note-2",
                "userId" to userId,
                "title" to "Segunda nota remota",
                "content" to "Texto secundario",
                "folder" to "Notas",
                "tags" to "",
                "colorIndex" to 0,
                "isPinned" to false,
                "lastModifiedLocal" to 1000L,
                "updatedAtServer" to 1000L
            )
        )

        repository.processRemoteNotesList(userId, initialRemoteSnapshot)

        val insertedNote1 = database.noteDao().getNoteByRemoteUuidAndUser("remote-note-1", userId)
        assertNotNull(insertedNote1)
        assertEquals("Nota desde Firestore", insertedNote1!!.title)
        assertEquals("Proyectos", insertedNote1.folder)
        assertEquals("cloud,offline", insertedNote1.tags)
        assertEquals(3, insertedNote1.colorIndex)
        assertTrue(insertedNote1.isPinned)
        assertTrue(insertedNote1.isSynced)

        val folders = database.folderDao().getAllFoldersList()
        assertTrue(folders.any { it.name == "Proyectos" })

        // Insert an unsynced local offline note (isSynced = false) to verify it is NOT deleted during remote reconciliation
        val offlineLocalNote = NoteEntity(
            remoteUuid = "local-offline-uuid",
            userId = userId,
            title = "Creada sin conexión",
            content = "Pendiente de subir",
            isSynced = false,
            pendingDelete = false,
            lastModifiedLocal = 1500L
        )
        database.noteDao().insertNote(offlineLocalNote)

        // 2. Remote note-1 is updated in Firestore and remote-note-2 is deleted in Firestore
        val updatedRemoteSnapshot = listOf(
            mapOf<String, Any>(
                "remoteUuid" to "remote-note-1",
                "userId" to userId,
                "title" to "Nota actualizada en Firestore",
                "content" to "[{\"type\":\"text\",\"id\":\"b1\",\"content\":\"Contenido modificado\",\"style\":\"BODY\"}]",
                "folder" to "Proyectos",
                "tags" to "cloud,offline,actualizado",
                "colorIndex" to 5,
                "isPinned" to false,
                "lastModifiedLocal" to 2000L,
                "updatedAtServer" to 2000L
            )
        )

        repository.processRemoteNotesList(userId, updatedRemoteSnapshot)

        // Verify remote-note-1 was updated in Room
        val updatedNote1 = database.noteDao().getNoteByRemoteUuidAndUser("remote-note-1", userId)
        assertNotNull(updatedNote1)
        assertEquals("Nota actualizada en Firestore", updatedNote1!!.title)
        assertEquals("cloud,offline,actualizado", updatedNote1.tags)
        assertEquals(5, updatedNote1.colorIndex)
        assertEquals(false, updatedNote1.isPinned)
        assertTrue(updatedNote1.searchableText.contains("Contenido modificado"))

        // Verify remote-note-2 (which was synced) was deleted from Room after disappearing from Firestore
        val deletedNote2 = database.noteDao().getNoteByRemoteUuidAndUser("remote-note-2", userId)
        assertNull(deletedNote2)

        // Verify the unsynced offline note is preserved in Room
        val preservedOfflineNote = database.noteDao().getNoteByRemoteUuidAndUser("local-offline-uuid", userId)
        assertNotNull(preservedOfflineNote)
        assertEquals("Creada sin conexión", preservedOfflineNote!!.title)

        // Verify activeNotes Flow emits the offline-available Room notes for the user
        val activeNotesForUser = database.noteDao().getActiveNotesForUser(userId).first()
        assertEquals(2, activeNotesForUser.size)
    }
}
