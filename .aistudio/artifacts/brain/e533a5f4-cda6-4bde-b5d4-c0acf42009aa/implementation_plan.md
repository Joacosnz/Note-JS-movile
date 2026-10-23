# Integración Bidireccional de Recordatorios ↔ Checklists con Enlaces Profundos

Implementa un sistema unificado de gestión de tareas y recordatorios bidireccional en Note JS. Permite programar fecha y hora para cualquier ítem de lista dentro de una nota, sincronizar su estado de completado en tiempo real y gestionarlos desde una pantalla dedicada de Recordatorios y Tareas con enlaces profundos directos a la nota de origen.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario en la fase de clarificación:**
> - **Visualización y gestión centralizada**: Se incorpora una pantalla/pestaña dedicada de **Recordatorios y Tareas** accesible desde la navegación principal.
> - **Programación intuitiva en notas**: Cada ítem de checklist contará con un icono de reloj accesible para abrir el selector de fecha y hora.
> - **Sincronización instantánea de estado**: Marcar una tarea como completada en la vista de Recordatorios actualiza de inmediato el checklist dentro de la nota, y viceversa, sin desajustes de estado.

---

### 1. Overview & Core Concept

- **Qué hace**: Convierte los bloques de checklist de Note JS en elementos procesables de primer nivel. Cada ítem puede tener asignada una fecha y hora límite con recordatorio del sistema. Desde un panel centralizado de Recordatorios, el usuario puede filtrar por *Hoy*, *Programados* y *Completados*, marcar ítems y hacer clic en un enlace profundo para abrir la nota original exactamente en su contexto.
- **Público objetivo**: Usuarios que capturan notas de reuniones, planifican proyectos o administran listas de pendientes y necesitan que sus listas de verificación se conviertan en compromisos con recordatorio sin duplicar información.
- **Valor diferencial frente a Apple**: A diferencia de Apple Reminders y Notes (donde el enlace no sincroniza el estado de las casillas de verificación en la nota), Note JS mantiene **sincronía bidireccional reactiva**: al completar una tarea desde la notificación o la lista de recordatorios, la nota se actualiza automáticamente en Room y en Firestore.

---

### 2. User Experience & Visual Design

- **Flujo 1: Programar recordatorio en un ítem de checklist (Editor de Notas)**
  1. En `NoteEditorScreen`, cada ítem de checklist muestra un botón sutil con icono de reloj al lado del campo de texto.
  2. Al pulsar el reloj, se despliega un diálogo M3 (`DatePickerDialog` + `TimePickerDialog`) para seleccionar fecha y hora.
  3. Al confirmar, el ítem muestra un badge con la fecha y hora programada (`Ej. Hoy 18:00` o `25 Oct 09:30`).
  4. Si el ítem ya venció, el badge se destaca con color de acento/alerta. Al marcar la casilla `[x]`, el badge se atenúa con tachado suave.

- **Flujo 2: Pantalla dedicada de Recordatorios y Tareas (`RemindersScreen`)**
  1. Nueva pantalla accesible desde el menú inferior / barra de navegación (`Screen.Reminders`).
  2. Cabecera con selector de filtros rápidos:
     - ☀️ **Hoy** (tareas con vencimiento en la jornada).
     - 📅 **Programados** (tareas futuras ordenadas cronológicamente).
     - ✅ **Completados** (historial de tareas terminadas con opción de desmarcar).
  3. Tarjeta de tarea con:
     - Checkbox circular interactivo con animación háptica.
     - Texto de la tarea (con estilo tachado si está completada).
     - Indicador temporal y badge de urgencia/prioridad.
     - **Chip de enlace profundo**: `📄 [Título de la Nota]` con el color temático de la nota. Al pulsarlo, salta directamente a la nota en el editor (`Screen.Editor(noteId)`).

- **Flujo 3: Notificación del sistema y acción directa**
  - Al dispararse la alarma cronológica, la notificación de Android incluye botones de acción rápida: *"Completar"* (marca la tarea en la base de datos sin abrir la app) y *"Abrir Nota"* (navega directamente al editor).

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Estructura de datos de los ítems de checklist**
  - *Enfoque*: Extender el modelo `ChecklistItem` con campos opcionales serializables:
    ```kotlin
    data class ChecklistItem(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val isChecked: Boolean = false,
        val reminderTimestamp: Long? = null,
        val isReminderDone: Boolean = false,
        val priority: Int = 0
    )
    ```
  - *Por qué*: Al serializarse como JSON dentro de `note.checklistData`, **no rompe la estructura de la base de datos de Room** ni requiere migraciones destructivas de SQLite, garantizando 100% de compatibilidad hacia atrás con las notas existentes.
  - *Alternativas descartadas*: Crear una tabla SQLite separada de `reminders` forzaría migraciones complejas y potenciales pérdidas de sincronización entre el texto de la nota y la tabla externa.

- **Decisión 2: Sincronización bidireccional reactiva**
  - *Enfoque*: `NotesViewModel` expone un `StateFlow<List<UnifiedReminderItem>>` calculado reactivamente combinando todas las notas activas que poseen ítems de checklist con `reminderTimestamp != null` o recordatorios de nota.
  - *Por qué*: Cualquier cambio en una nota (editar texto, borrar ítem, marcar casilla) se refleja inmediatamente en la lista de recordatorios y en la persistencia local/nube.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Capas del Sistema                             │
└────────────────────────────────────────────────────────────────────────┘

    ┌────────────────────────┐         ┌────────────────────────┐
    │    NoteEditorScreen    │         │    RemindersScreen     │
    │  (ChecklistBlockItem)  │         │  (Hoy / Programados)   │
    └───────────┬────────────┘         └───────────┬────────────┘
                │                                  │
                │ Modifica ChecklistItem           │ Completa / Reagenda /
                │ con fecha/hora                   │ Deep Link a la nota
                ▼                                  ▼
    ┌───────────────────────────────────────────────────────────┐
    │                      NotesViewModel                       │
    │  - toggleChecklistItem(noteId, itemId)                    │
    │  - setItemReminder(noteId, itemId, timestamp)             │
    │  - unifiedRemindersFlow: StateFlow<List<ReminderItem>>     │
    └─────────────┬───────────────────────────────┬─────────────┘
                  │                               │
                  ▼                               ▼
    ┌──────────────────────────┐    ┌───────────────────────────┐
    │    ReminderScheduler     │    │      NoteRepository       │
    │  (AlarmManager / Work)   │    │     & Room (NoteDao)      │
    │  - scheduleNotification  │    │  - Actualiza checklistData │
    │  - cancelNotification    │    │  - Sincroniza Firestore   │
    └──────────────────────────┘    └───────────────────────────┘
```

- **Mapeo de eventos e interacción**:
  1. `setItemReminder(noteId, itemId, timestamp)`: Actualiza el JSON de `checklistData` de la nota correspondiente, programa la alarma mediante `ReminderScheduler` y persiste en Room/Firestore.
  2. `toggleReminderCompleted(noteId, itemId)`: Invierte `isChecked` y `isReminderDone`. Si se desmarca, reactiva el recordatorio si la fecha es futura.
  3. `navigateToNote(noteId)`: Cambia `currentScreen` a `Screen.Editor(noteId)`, abriendo la nota de forma instantánea.
