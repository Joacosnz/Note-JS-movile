package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.NoteEntity
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
class NoteRoomPersistenceTest {

    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetNote_persistsTitleAndContentOffline() = runTest {
        val note = NoteEntity(
            title = "Ideas de Arquitectura",
            content = "1. Utilizar Room para persistencia offline\n2. Sincronización en segundo plano con Firestore"
        )

        val insertedId = noteDao.insert(note)
        assertTrue(insertedId > 0)

        val retrieved = noteDao.getNoteById(insertedId)
        assertNotNull(retrieved)
        assertEquals("Ideas de Arquitectura", retrieved!!.title)
        assertEquals("1. Utilizar Room para persistencia offline\n2. Sincronización en segundo plano con Firestore", retrieved.content)
    }

    @Test
    fun observeAllNotes_emitsOfflineNotesReactively() = runTest {
        val note1 = NoteEntity(title = "Nota 1", content = "Contenido 1")
        val note2 = NoteEntity(title = "Nota 2", content = "Contenido 2")

        noteDao.insert(note1)
        noteDao.insert(note2)

        val notesList = noteDao.getAllNotes().first()
        assertEquals(2, notesList.size)
        assertTrue(notesList.any { it.title == "Nota 1" && it.content == "Contenido 1" })
        assertTrue(notesList.any { it.title == "Nota 2" && it.content == "Contenido 2" })
    }

    @Test
    fun updateTitleAndContent_updatesOfflineStorage() = runTest {
        val note = NoteEntity(title = "Título Original", content = "Contenido Original")
        val id = noteDao.insert(note)

        noteDao.updateTitleAndContent(
            id = id,
            title = "Título Editado",
            content = "Contenido Modificado Offline"
        )

        val updated = noteDao.getNoteById(id)
        assertNotNull(updated)
        assertEquals("Título Editado", updated!!.title)
        assertEquals("Contenido Modificado Offline", updated.content)
    }

    @Test
    fun deleteNote_removesFromOfflineStorage() = runTest {
        val note = NoteEntity(title = "Para Borrar", content = "Texto temporal")
        val id = noteDao.insert(note)

        val existing = noteDao.getNoteById(id)
        assertNotNull(existing)

        noteDao.delete(existing!!)

        val deleted = noteDao.getNoteById(id)
        assertNull(deleted)
    }

    @Test
    fun searchByTitleOrContent_findsMatchingOfflineNotes() = runTest {
        noteDao.insert(NoteEntity(title = "Receta de Cocina", content = "Harina, azúcar y huevos"))
        noteDao.insert(NoteEntity(title = "Reunión de Equipo", content = "Revisar sprint backlog"))

        val searchByTitle = noteDao.searchByTitleOrContent("Cocina").first()
        assertEquals(1, searchByTitle.size)
        assertEquals("Receta de Cocina", searchByTitle[0].title)

        val searchByContent = noteDao.searchByTitleOrContent("backlog").first()
        assertEquals(1, searchByContent.size)
        assertEquals("Reunión de Equipo", searchByContent[0].title)
    }

    @Test
    fun retrieveAllNotes_orderedByCreationDate() = runTest {
        val olderNote = NoteEntity(
            title = "Nota Antigua",
            content = "Creada primero",
            createdAt = 1000L
        )
        val newerNote = NoteEntity(
            title = "Nota Reciente",
            content = "Creada después",
            createdAt = 5000L
        )
        val middleNote = NoteEntity(
            title = "Nota Intermedia",
            content = "Creada en medio",
            createdAt = 3000L
        )

        noteDao.insert(olderNote)
        noteDao.insert(newerNote)
        noteDao.insert(middleNote)

        val notesOrderedDesc = noteDao.getAllNotesOrderedByCreationDate().first()
        assertEquals(3, notesOrderedDesc.size)
        assertEquals("Nota Reciente", notesOrderedDesc[0].title)
        assertEquals("Nota Intermedia", notesOrderedDesc[1].title)
        assertEquals("Nota Antigua", notesOrderedDesc[2].title)

        val notesOrderedAsc = noteDao.getAllNotesByCreatedAtAsc().first()
        assertEquals("Nota Antigua", notesOrderedAsc[0].title)
        assertEquals("Nota Intermedia", notesOrderedAsc[1].title)
        assertEquals("Nota Reciente", notesOrderedAsc[2].title)
    }

    @Test
    fun deleteAllNotes_clearsAllStoredNotes() = runTest {
        noteDao.insert(NoteEntity(title = "N1", content = "C1"))
        noteDao.insert(NoteEntity(title = "N2", content = "C2"))

        assertEquals(2, noteDao.getAllNotes().first().size)
        noteDao.deleteAll()
        assertEquals(0, noteDao.getAllNotes().first().size)
    }
}
