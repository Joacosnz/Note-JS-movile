package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.Note
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
        val note = Note(
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
        val note1 = Note(title = "Nota 1", content = "Contenido 1")
        val note2 = Note(title = "Nota 2", content = "Contenido 2")

        noteDao.insert(note1)
        noteDao.insert(note2)

        val notesList = noteDao.getAllNotes().first()
        assertEquals(2, notesList.size)
        assertTrue(notesList.any { it.title == "Nota 1" && it.content == "Contenido 1" })
        assertTrue(notesList.any { it.title == "Nota 2" && it.content == "Contenido 2" })
    }

    @Test
    fun updateTitleAndContent_updatesOfflineStorage() = runTest {
        val note = Note(title = "Título Original", content = "Contenido Original")
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
        val note = Note(title = "Para Borrar", content = "Texto temporal")
        val id = noteDao.insert(note)

        val existing = noteDao.getNoteById(id)
        assertNotNull(existing)

        noteDao.delete(existing!!)

        val deleted = noteDao.getNoteById(id)
        assertNull(deleted)
    }

    @Test
    fun searchByTitleOrContent_findsMatchingOfflineNotes() = runTest {
        noteDao.insert(Note(title = "Receta de Cocina", content = "Harina, azúcar y huevos"))
        noteDao.insert(Note(title = "Reunión de Equipo", content = "Revisar sprint backlog"))

        val searchByTitle = noteDao.searchByTitleOrContent("Cocina").first()
        assertEquals(1, searchByTitle.size)
        assertEquals("Receta de Cocina", searchByTitle[0].title)

        val searchByContent = noteDao.searchByTitleOrContent("backlog").first()
        assertEquals(1, searchByContent.size)
        assertEquals("Reunión de Equipo", searchByContent[0].title)
    }
}
