package com.example.model

/**
 * Tipo de registro que presenta conflicto de sincronización entre plataformas.
 */
enum class ConflictRecordType(val displayLabel: String) {
    NOTE("Nota"),
    REMINDER("Recordatorio"),
    CALENDAR_EVENT("Evento de Calendario")
}

/**
 * Decisión manual tomada por el usuario para resolver un conflicto entre Google y Microsoft.
 */
enum class ConflictResolutionChoice {
    KEEP_GOOGLE,
    KEEP_MICROSOFT,
    KEEP_BOTH
}

/**
 * Representa un conflicto detectado cuando una misma nota, recordatorio o evento
 * ha sido modificado tanto en la plataforma de Google (Firestore / Drive / Google Calendar)
 * como en la plataforma de Microsoft (OneDrive / Outlook Calendar).
 */
data class SyncConflictItem(
    val id: String,
    val localId: Long = 0L,
    val remoteUuid: String,
    val recordType: ConflictRecordType,
    val googleTitle: String,
    val googleContent: String,
    val googleUpdatedAt: Long,
    val microsoftTitle: String,
    val microsoftContent: String,
    val microsoftUpdatedAt: Long,
    val googleEventStartMillis: Long = 0L,
    val microsoftEventStartMillis: Long = 0L,
    val microsoftGraphEventId: String? = null
)
