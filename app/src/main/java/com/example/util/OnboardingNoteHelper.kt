package com.example.util

import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.model.TableCell
import com.example.model.TextStyleType

object OnboardingNoteHelper {

    fun createWelcomeNote(): NoteEntity {
        val blocks = mutableListOf<NoteBlock>()

        // 1. Header Banner Callout
        blocks.add(
            NoteBlock.CalloutBox(
                content = "💡 ¡Bienvenido a Note JS! Esta es tu nota interactiva de presentación. Puedes marcar casillas de tareas, desplegar u ocultar subtítulos H2, editar celdas en la tabla técnica inferior y explorar todo el poder del editor.",
                backgroundColorHex = "#FEF3C7" // Warm amber accent
            )
        )

        // 2. Main Title
        blocks.add(
            NoteBlock.Text(
                content = "📱 Dossier Completo de Capacidades: Note JS",
                style = TextStyleType.TITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Text(
                content = "Note JS es una suite de productividad personal y profesional para Android construida con arquitectura Offline-First, Jetpack Compose (Material 3), Room Database, Firebase Firestore y Jetpack Glance. Combina un editor de notas modular por bloques con un sistema integral de calendario y recordatorios interactivos.",
                style = TextStyleType.BODY
            )
        )

        // 3. Section 1: Block Editor
        blocks.add(
            NoteBlock.Text(
                content = "1. ✍️ Editor Avanzado de Notas por Bloques",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Checklist(
                content = "Párrafos con texto enriquecido, detección de URLs y previsualización inteligente",
                isChecked = true
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Subtítulos plegables (H2): Toca la flecha a la izquierda del título para colapsar o expandir secciones",
                isChecked = true
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Listas de tareas (checklists) con tachado automático y respuesta háptica al pulsar",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Resolución matemática en línea: Escribe operaciones como 1500 * 1.21 = para calcular al vuelo",
                isChecked = false
            )
        )

        blocks.add(
            NoteBlock.CalloutBox(
                content = "🧮 Ejemplo de Math Solver: (250 * 4) + 150 = 1150. Escribe expresiones con paréntesis, porcentajes y operaciones estándar dentro de tus notas.",
                backgroundColorHex = "#E0F2FE"
            )
        )

        // 4. Section 2: Multimedia & Canvas
        blocks.add(
            NoteBlock.Text(
                content = "2. 🎙️ Multimedia, Escaneo y Canvas Digital",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Checklist(
                content = "Grabadora de notas de voz con telemetría de onda en vivo y reproductor integrado Media3",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Dictado por voz (Speech-to-Text) integrado para escribir sin tocar el teclado",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Escáner de documentos con OCR inteligente (Google ML Kit) para extraer texto editable de fotos",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Lienzo de dibujo digital vectorial con trazos, resaltador, selector de color y exportación PNG",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Gestión de firmas caligráficas reutilizables para estampar en notas y contratos",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Lector y anotador de PDFs con renderizado de alta definición y dibujo sobre páginas",
                isChecked = false
            )
        )

        // 5. Section 3: Calendar & Reminders
        blocks.add(
            NoteBlock.Text(
                content = "3. 🗓️ Sistema Integral de Calendario y Recordatorios",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Checklist(
                content = "Recordatorios puntuales con fecha, hora, prioridad (!, !!, !!!) y lista de subtareas internas",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Eventos de calendario con horarios, marcador de 'Todo el día', categorías y ubicación",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Vista Diaria con línea de tiempo de 24 horas y zoom ajustable de 42 dp a 140 dp",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Vista Semanal y Multi-Día con columnas continuas y navegación sincronizada",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Vista Mensual con zoom semántico de 4 densidades: Compacto, Apilado, Detalles y Expandido",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Alarmas de alta prioridad programadas con AlarmManager y notificación con botón 'Completar'",
                isChecked = false
            )
        )

        // 6. Section 4: Home Widgets
        blocks.add(
            NoteBlock.Text(
                content = "4. 🧩 Widgets Interactivos de Inicio (Jetpack Glance)",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Checklist(
                content = "Widget de Calendario & Recordatorios: Marca recordatorios como completados directamente desde la pantalla de inicio",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Widget de Notas Recientes: Vista rápida de tus notas y acceso directo a nueva creación",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Widget de Nota Individual anclada para listas de compras o notas de acceso inmediato",
                isChecked = false
            )
        )

        // 7. Section 5: Security & Cloud
        blocks.add(
            NoteBlock.Text(
                content = "5. 🔒 Seguridad, Privacidad y Sincronización en la Nube",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        blocks.add(
            NoteBlock.Checklist(
                content = "Bloqueo biométrico global por huella/rostro con PIN de respaldo para máxima privacidad",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Bloqueo confidencial independiente para notas individuales con contraseña propia",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Almacenamiento Offline-First con Room Database (SQLite) y búsqueda instantánea FTS4",
                isChecked = true
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Sincronización en la nube mediante Firebase Firestore y copia de seguridad en Google Drive",
                isChecked = false
            )
        )

        // 8. Section 6: Technical Summary Table
        blocks.add(
            NoteBlock.Text(
                content = "6. 📊 Ficha Técnica del Proyecto",
                style = TextStyleType.SUBTITLE,
                isCollapsed = false
            )
        )

        val tableRows = 6
        val tableCols = 3
        val matrix = mutableListOf<MutableList<TableCell>>()

        // Header Row
        matrix.add(
            mutableListOf(
                TableCell(text = "Componente", backgroundColorHex = "#E2E8F0"),
                TableCell(text = "Tecnología / Motor", backgroundColorHex = "#E2E8F0"),
                TableCell(text = "Estado", backgroundColorHex = "#E2E8F0")
            )
        )

        // Row 1
        matrix.add(
            mutableListOf(
                TableCell(text = "Persistencia Local", backgroundColorHex = null),
                TableCell(text = "Room Database + FTS4", backgroundColorHex = null),
                TableCell(text = "✅ 100% Offline-First", backgroundColorHex = "#DCFCE7")
            )
        )

        // Row 2
        matrix.add(
            mutableListOf(
                TableCell(text = "UI & Temas", backgroundColorHex = null),
                TableCell(text = "Jetpack Compose + Material 3", backgroundColorHex = null),
                TableCell(text = "✅ Modo Claro / Oscuro", backgroundColorHex = "#DCFCE7")
            )
        )

        // Row 3
        matrix.add(
            mutableListOf(
                TableCell(text = "Calendario & Zoom", backgroundColorHex = null),
                TableCell(text = "Líneas de tiempo + Gestos táctiles", backgroundColorHex = null),
                TableCell(text = "✅ 4 Densidades Zoom", backgroundColorHex = "#DCFCE7")
            )
        )

        // Row 4
        matrix.add(
            mutableListOf(
                TableCell(text = "Widgets de Escritorio", backgroundColorHex = null),
                TableCell(text = "Jetpack Glance + Actions", backgroundColorHex = null),
                TableCell(text = "✅ Marcado Directo", backgroundColorHex = "#DCFCE7")
            )
        )

        // Row 5
        matrix.add(
            mutableListOf(
                TableCell(text = "Multimedia & OCR", backgroundColorHex = null),
                TableCell(text = "Google ML Kit + Media3 + PdfBox", backgroundColorHex = null),
                TableCell(text = "✅ Integrado", backgroundColorHex = "#DCFCE7")
            )
        )

        blocks.add(
            NoteBlock.Table(
                rows = tableRows,
                cols = tableCols,
                matrix = matrix,
                hasHeaderRow = true,
                hasZebraStripes = true
            )
        )

        // 9. Concluding Callout Box
        blocks.add(
            NoteBlock.CalloutBox(
                content = "🎉 ¡Todo configurado! Puedes explorar la barra lateral para acceder a tus Carpetas, abrir la vista de Calendario o agregar widgets en tu pantalla de inicio. #Bienvenida #Guía #NoteJS #Productividad",
                backgroundColorHex = "#DCFCE7" // Subtle light green
            )
        )

        val serializedContent = NoteBlockSerializer.serialize(blocks)
        val searchableSnippet = "Bienvenido a Note JS Guía Maestra y Capacidades Editor por Bloques Multimedia OCR Lienzo Calendario Recordatorios Widgets Glance Seguridad Room Firestore"

        return NoteEntity(
            title = "🚀 Bienvenido a Note JS: Guía Maestra y Capacidades",
            content = serializedContent,
            noteType = "TEXT",
            colorIndex = 1, // Warm cheerful theme color
            paperStyle = "BLANK",
            isPinned = true,
            isArchived = false,
            isTrashed = false,
            isLocked = false,
            tags = "Bienvenida,Guía,NoteJS",
            folder = "Notas",
            searchableText = searchableSnippet,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastModifiedLocal = System.currentTimeMillis()
        )
    }
}
