package com.example.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.remote.FirestoreManager
import com.example.data.repository.NoteRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        FirebaseApp.clearInstancesForTest()
        val options = FirebaseOptions.Builder()
            .setProjectId("demo-test-project")
            .setApplicationId("1:1234567890:android:abcdef")
            .setApiKey("fake-api-key")
            .build()
        FirebaseApp.initializeApp(application, options)

        database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = NoteRepository(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            signatureDao = database.signatureDao(),
            firestoreManager = FirestoreManager(application),
            context = application
        )

        viewModel = NotesViewModel(
            application = application,
            customRepository = repository
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
        database.close()
        FirebaseApp.clearInstancesForTest()
        Dispatchers.resetMain()
    }

    @Test
    fun insertNote_updatesNotesStateFlow() = runTest(testDispatcher) {
        val note = NoteEntity(
            title = "Nueva Nota",
            content = "Contenido persistido en Room"
        )

        val insertedId = viewModel.insertNoteSuspending(note)
        assertTrue(insertedId > 0)

        val activeNotes = viewModel.activeNotes.first { it.isNotEmpty() }
        val insertedNote = activeNotes.firstOrNull { it.id == insertedId }
        assertNotNull(insertedNote)
        assertEquals("Nueva Nota", insertedNote!!.title)
        assertEquals("Contenido persistido en Room", insertedNote.content)

        val allNotes = viewModel.allNotes.first { it.isNotEmpty() }
        val insertedInAll = allNotes.firstOrNull { it.id == insertedId }
        assertNotNull(insertedInAll)
    }

    @Test
    fun updateNote_modifiesPersistedNoteAndStateFlow() = runTest(testDispatcher) {
        val note = NoteEntity(title = "Título Inicial", content = "Contenido Inicial")
        val noteId = viewModel.insertNoteSuspending(note)

        val savedNote = database.noteDao().getNoteById(noteId)
        assertNotNull(savedNote)

        val updatedNote = savedNote!!.copy(
            title = "Título Modificado",
            content = "Contenido Actualizado"
        )
        viewModel.updateNoteSuspending(updatedNote)

        val retrievedFromDao = database.noteDao().getNoteById(noteId)
        assertNotNull(retrievedFromDao)
        assertEquals("Título Modificado", retrievedFromDao!!.title)
        assertEquals("Contenido Actualizado", retrievedFromDao.content)
    }

    @Test
    fun deleteNote_removesFromRoomAndStateFlow() = runTest(testDispatcher) {
        val note = NoteEntity(title = "Nota a eliminar", content = "Texto")
        val noteId = viewModel.insertNoteSuspending(note)

        val saved = database.noteDao().getNoteById(noteId)
        assertNotNull(saved)

        viewModel.deleteNoteSuspending(saved!!)

        val afterDelete = database.noteDao().getNoteById(noteId)
        assertNull(afterDelete)
    }

    @Test
    fun deleteNoteById_removesNoteSuccessfully() = runTest(testDispatcher) {
        val note = NoteEntity(title = "Nota por ID", content = "Contenido por ID")
        val noteId = viewModel.insertNoteSuspending(note)
        assertTrue(noteId > 0)
        assertNotNull(database.noteDao().getNoteById(noteId))

        viewModel.deleteNoteByIdSuspending(noteId)

        assertNull(database.noteDao().getNoteById(noteId))
    }
}
