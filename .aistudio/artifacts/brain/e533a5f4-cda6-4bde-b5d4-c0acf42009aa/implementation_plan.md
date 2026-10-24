# Centro Unificado de Recordatorios y Calendario

Este plan detalla la evolución del sistema actual de recordatorios (basado únicamente en alertas dentro de notas) hacia un **Centro Independiente de Recordatorios y Calendario**, inspirado en la arquitectura convergente de iOS 18, iPadOS 18 y macOS Sequoia, con soporte offline-first y sincronización instantánea en tiempo real en la nube.

---

### User Review & Critical Decisions

A partir de tus respuestas, estas son las decisiones clave que guían el diseño:

1. **Entidades Independientes con Vinculación Opcional a Notas:** Los recordatorios y eventos ya no dependerán de crear una nota previa. Serán elementos independientes con su propio ciclo de vida, listas, subtareas y prioridades, pero incluirán un selector opcional para vincular cualquier nota existente y abrirla con un toque.
2. **Arquitectura Espacial Multi-Escala Completa:** Se implementarán las cuatro vistas temporales integradas: **Día** (bandeja "Todo el día" + línea de tiempo de 24 horas), **Multi-día** (vista táctica de 2 a 3 días en vertical), **Semana** (planificador de 5 a 7 columnas con reprogramación interactiva) y **Mes con Zoom Semántico** (gesto de pellizco vertical y selector rápido con 4 niveles de densidad: *Compacto*, *Apilado*, *Detalles* y *Detalles Expandidos*).
3. **Sincronización Instantánea en Tiempo Real (Firestore + Local):** Todos los recordatorios y eventos se almacenarán localmente para acceso inmediato sin conexión y se sincronizarán en tiempo real entre dispositivos bajo la cuenta vinculada, programando alarmas exactas del sistema en segundo plano.

---

### 1. Overview & Core Concept

El nuevo módulo transforma la pestaña de **Recordatorios** en un espacio de productividad temporal donde conviven dos elementos con semánticas visuales claras y complementarias:

* **Eventos de Calendario (Compromisos de tiempo fijo):** Bloques con hora de inicio y fin definidas (ej. reuniones, clases, bloques de estudio) que ocupan un área proporcional a su duración en la cuadrícula horaria.
* **Recordatorios Programados (Tareas accionables):** Elementos con un círculo de completado interactivo que representan acciones por realizar en una fecha o minuto específico, con soporte para prioridades (`!`, `!!`, `!!!`), subtareas jerárquicas, recurrencia y listas temáticas.

Ambos elementos coexisten en la misma línea de tiempo para facilitar el **time-blocking** (organizar qué hacer y cuándo hacerlo en una sola pantalla), manteniendo además una vista de **Listas Inteligentes** (Hoy, Programados, Todos, Con prioridad y Completados) para gestionar tareas pendientes o sin fecha asignada.

---

### 2. User Experience & Visual Design

#### Taxonomía Visual y Diferenciación Semántica
* **Eventos:** Se muestran como contenedores rectangulares con fondo translúcido y barra lateral sólida del color de su categoría de calendario (Personal, Trabajo, Estudio, etc.). Al tocarlos se despliega su ficha de detalles, ubicación, notas adicionales y nota vinculada.
* **Recordatorios:** Se distinguen por su **círculo de selección abierto (`○`)** coloreado según su lista de origen y texto directo sin caja envolvente pesada. Tocar el círculo marca la tarea como completada al instante con una animación suave: el círculo se rellena (`●`), el título aplica tachado y el elemento atenúa su opacidad, permaneciendo visible en su franja horaria como registro de logro diario.

```
┌──────────────────────────────────────────────────────────────────────┐
│  TODO EL DÍA                                                         │
│  ┌─────────────────────────────────┐  ○ Pagar factura de internet    │
│  │ ▌ Aniversario / Día festivo     │  ○ Enviar informe trimestral !! │
│  └─────────────────────────────────┘                                 │
├────────┬─────────────────────────────────────────────────────────────┤
│ 09:00  │ ┌───────────────────────────────┐  ○ Revisar correo urgente │
│        │ │ ▌ Reunión de Diseño           │    🔗 Nota: Minuta Q4     │
│ 10:00  │ │   09:00 - 10:15 • Sala B      │                           │
│        │ └───────────────────────────────┘                           │
│ 11:00  │ ● Llamar al proveedor (Completado)                          │
└────────┴─────────────────────────────────────────────────────────────┘
```

#### Modos de Visualización del Calendario
1. **Vista Diaria (Micro-gestión y Time-Blocking):**
   * Cabecera superior fija **"Todo el día"** para eventos de jornada completa y recordatorios con fecha pero sin hora específica.
   * Cuadrícula vertical de 24 horas con indicador en vivo de la hora actual (línea roja con punto) y resolución automática de colisiones en columnas paralelas cuando un evento y un recordatorio coinciden a la misma hora.
2. **Vista Multi-Día (2–3 Días) y Vista Semanal:**
   * En orientación vertical, el modo **Multi-día** presenta una ventana continua de 3 días que conserva legibilidad completa para títulos y casillas de verificación.
   * El modo **Semana** muestra las columnas de la semana con bloques proporcionales y permite arrastrar o mover rápidamente un recordatorio a otra franja horaria o día.
3. **Vista Mensual con Zoom de Densidad (Pinch-to-Zoom):**
   * Mediante el gesto de pellizco vertical (o un control deslizante rápido de densidad), las celdas del mes escalan dinámicamente entre 4 niveles:
     * **Nivel 1 — Compacto:** Indicadores de puntos de color bajo el número del día.
     * **Nivel 2 — Apilado (Mapa de calor):** Pequeñas barras horizontales de color proporcionales a la carga del día.
     * **Nivel 3 — Detalles:** Títulos abreviados de eventos y recordatorios con su círculo `○` visible dentro de cada día.
     * **Nivel 4 — Detalles Expandidos:** Celdas amplias tipo agenda con horas de inicio, subtareas resumidas y casillas interactivas para completar recordatorios directamente desde la vista mensual.

#### Modal de Creación Híbrida (`+`) y Flujos de Interacción
* **Selector Dual Superior:** Al pulsar el botón `+` o mantener presionada una franja horaria vacía en la cuadrícula, se abre una hoja inferior con el selector segmentado **[ Evento | Recordatorio ]** y la fecha/hora ya pre-rellenadas según el punto tocado.
* **Campos de Recordatorio:** Título, notas, fecha, hora opcional (interruptor de "Todo el día" o con hora exacta), repetición (diaria, semanal, mensual, personalizada), prioridad (`Ninguna`, `!`, `!!`, `!!!`), lista asignada (Personal, Trabajo, Compras, etc.), lista de subtareas interactivas y **Nota vinculada opcional**.
* **Campos de Evento:** Título, categoría/color de calendario, interruptor de todo el día, hora de inicio y fin, ubicación/enlace, alerta previa (a la hora, 10 min antes, 1 hora antes), repetición y **Nota vinculada opcional**.
* **Filtros de Visibilidad:** Un panel de filtros rápido permite encender o apagar la visibilidad de eventos, recordatorios completados o listas/categorías específicas.

---

### 3. Key Product Decisions & Trade-Offs

* **Modelo Unificado con Discriminador de Tipo (`REMINDER` vs `EVENT`):** En lugar de mantener motores desconectados, ambos tipos comparten el mismo motor de indexación temporal, recurrencia, sincronización en tiempo real y vinculación de notas, diferenciándose por sus propiedades específicas (hora de fin/duración para eventos vs. estado completado/prioridad/subtareas para recordatorios). Esto garantiza que las vistas de Día, Multi-día, Semana y Mes rendericen ambos elementos sin latencia ni duplicación de consultas.
* **Transición Limpia desde "Notas con Recordatorio":** Las notas seguirán siendo documentos de texto/dibujo/listas, pero en lugar de depender de la nota para gestionar el calendario, desde el editor de una nota también se podrá crear o ver el recordatorio/evento vinculado en el nuevo Centro de Calendario. Los recordatorios antiguos configurados en notas existentes seguirán mostrándose como elementos vinculados para que no se pierda ningún dato previo.
* **Zoom Fluido + Selector Accesible:** Además del gesto táctil de pellizco vertical en la vista mensual, se incluirá un botón selector rápido de densidad en la barra superior del calendario para garantizar accesibilidad con una sola mano.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                           │
│  ┌───────────────────────┐  ┌────────────────────────────────────────┐  │
│  │  Calendar Spatial Hub │  │  Smart Lists & Hybrid Creation Sheet   │  │
│  │  • Day / Multi-Day    │  │  • Segmented [Recordatorio | Evento]   │  │
│  │  • Week Grid          │  │  • Subtasks, Priority & Recurrence     │  │
│  │  • Month Pinch-Zoom   │  │  • Optional Note Link Picker           │  │
│  └───────────┬───────────┘  └───────────────────┬────────────────────┘  │
└──────────────┼──────────────────────────────────┼───────────────────────┘
               ▼                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Domain & State Layer (ViewModel)                        │
│  • Time-Slot Collision Layout Engine & Multi-Scale Zoom State           │
│  • Bidirectional Note Linking & Recurrence Expansion                    │
└──────────────┬──────────────────────────────────┬───────────────────────┘
               ▼                                  ▼
┌──────────────────────────────────┐  ┌───────────────────────────────────┐
│  Local Persistence & Alarms      │  │  Real-Time Cloud Sync (Firestore) │
│  • Offline-First Calendar/Task   │◄─┼─►• /users/{uid}/reminders/{id}    │
│    Table (Room Migration)        │  │  • Instant Snapshot Listener      │
│  • AlarmManager Exact Reminders  │  │  • Conflict-Free Last-Write-Wins  │
└──────────────────────────────────┘  └───────────────────────────────────┘
```

#### Esquema de Datos y Sincronización
1. **Persistencia Local (Offline-First):**
   * Nueva entidad dedicada para elementos del calendario y recordatorios con soporte para: identificador único local y remoto (`remoteUuid`), propietario (`userId`), tipo (`REMINDER` | `EVENT`), título, descripción, fecha/hora de inicio (`scheduledAt`), fecha/hora de fin (`endTimeMillis`), indicador de día completo (`isAllDay`), estado de completado (`isCompleted`, `completedAt`), prioridad (`0..3`), color y nombre de lista/categoría, regla de recurrencia, subtareas serializadas, identificador de nota vinculada opcional (`linkedNoteId`), marca de actualización (`updatedAt`) y estado de sincronización (`isSynced`, `isDeleted`).
   * Migración segura de la base de datos local sin pérdida de notas, carpetas ni firmas existentes.
2. **Sincronización en Tiempo Real con Cloud Firestore:**
   * Colección segura `/users/{userId}/reminders/{reminderId}` protegida con reglas de seguridad Zero-Trust (validación de propietario autenticado, tipos de datos y límites de tamaño).
   * Escucha activa en tiempo real (`SnapshotListener`) integrada en el ciclo de sincronización de la cuenta para que cualquier recordatorio o evento creado, reprogramado o completado en un dispositivo se refleje al instante en los demás dispositivos y reprograme las notificaciones locales automáticamente.
