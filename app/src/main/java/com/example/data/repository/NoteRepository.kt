package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insert(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun update(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun toggleArchive(note: NoteEntity) {
        val willBeArchived = !note.isArchived
        noteDao.updateNote(
            note.copy(
                isArchived = willBeArchived,
                isPinned = if (willBeArchived) false else note.isPinned,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveToTrash(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isTrashed = true,
                isPinned = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isTrashed = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun preseedIfEmpty() {
        val now = System.currentTimeMillis()
        val defaultNotes = listOf(
            NoteEntity(
                title = "✨ Bienvenido a AuraNotes",
                content = "Una experiencia de notas que fusiona lo mejor de Google Keep (rapidez, tarjetas de colores y FAB multimodal), Samsung Notes (lienzo con cuadrículas, lápices y ergonomía) y Apple Notes (Math Notes, listas y formato fluido).\n\n" +
                        "Toca '+' para crear notas rápidas, listas, dibujos o cálculos.\n" +
                        "Cambia entre vista en cuadrícula de 2 columnas o lista.\n" +
                        "Organiza con etiquetas (#Ideas, #Trabajo, #Estudio).\n" +
                        "Protege notas privadas con código PIN.",
                noteType = "TEXT",
                colorIndex = 0,
                paperStyle = "BLANK",
                isPinned = true,
                tags = "Ideas,Inicio",
                createdAt = now - (1000 * 60 * 30),
                updatedAt = now - (1000 * 60 * 30),
            ),
            NoteEntity(
                title = "🛒 Lista de compras y tareas",
                content = "Pendientes para la semana",
                noteType = "CHECKLIST",
                colorIndex = 0,
                paperStyle = "BLANK",
                isPinned = true,
                tags = "Personal",
                checklistData = """[
                    {"id":"1","text":"Café en grano de especialidad","isChecked":true},
                    {"id":"2","text":"Frutas frescas (manzanas, plátanos)","isChecked":false},
                    {"id":"3","text":"Cuaderno punteado A5","isChecked":true},
                    {"id":"4","text":"Plumas estilográficas de repuesto","isChecked":false},
                    {"id":"5","text":"Revisar sincronización de archivos","isChecked":false}
                ]""",
                createdAt = now - (1000 * 60 * 60 * 2),
                updatedAt = now - (1000 * 60 * 45),
            ),
            NoteEntity(
                title = "🧮 Math Notes: Presupuesto del proyecto",
                content = "Cálculo reactivo automático estilo Apple Notes:\n\n" +
                        "Diseño UI = 450\n" +
                        "Desarrollo Android = 850\n" +
                        "Infraestructura cloud = 150\n\n" +
                        "Total base: 450 + 850 + 150 = 1450\n" +
                        "IVA estimado: 1450 * 0.21 = 304.5\n" +
                        "Gran total: 1450 + 304.5 = 1754.5\n\n" +
                        "¡Escribe cualquier operación seguida de '=' para calcular en tiempo real!",
                noteType = "MATH",
                colorIndex = 0,
                paperStyle = "BLANK",
                isPinned = false,
                tags = "Trabajo,Cálculo",
                createdAt = now - (1000 * 60 * 120),
                updatedAt = now - (1000 * 60 * 10),
            ),
            NoteEntity(
                title = "🎨 Boceto de arquitectura",
                content = "Lienzo de dibujo vectorial con soporte para cuadrícula, pautado y puntos, inspirado en Samsung Notes y Apple Pencil.",
                noteType = "DRAWING",
                colorIndex = 0,
                paperStyle = "BLANK",
                isPinned = false,
                tags = "Diseño,Ideas",
                drawingData = "PRESET_SKETCH",
                createdAt = now - (1000 * 60 * 180),
                updatedAt = now - (1000 * 60 * 90),
            )
        )

        for (item in defaultNotes) {
            noteDao.insertNote(item)
        }
    }
}
