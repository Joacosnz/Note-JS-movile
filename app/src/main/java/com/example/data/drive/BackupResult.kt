package com.example.data.drive

import android.content.Intent
import java.util.Locale

enum class BackupFrequency(
    val code: String,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    NEVER(
        code = "NEVER",
        label = "Nunca",
        shortLabel = "Nunca",
        description = "Desactiva las copias de seguridad automáticas"
    ),
    MANUAL_ONLY(
        code = "MANUAL_ONLY",
        label = "Solo cuando toco \"Guardar\"",
        shortLabel = "Manual",
        description = "Solo se respalda cuando presionas el botón Guardar"
    ),
    DAILY(
        code = "DAILY",
        label = "Diariamente",
        shortLabel = "Diaria",
        description = "Se sube una copia automática todos los días"
    ),
    WEEKLY(
        code = "WEEKLY",
        label = "Semanalmente",
        shortLabel = "Semanal",
        description = "Se sube una copia automática cada 7 días"
    ),
    MONTHLY(
        code = "MONTHLY",
        label = "Mensualmente (1 vez al mes)",
        shortLabel = "Mensual",
        description = "Se sube una copia automática cada 30 días"
    );

    companion object {
        fun fromCode(code: String?): BackupFrequency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DAILY
        }
    }
}

enum class RestoreMode(val label: String, val description: String) {
    MERGE(
        label = "Combinar con mis notas actuales (Recomendado)",
        description = "Recupera las notas y recordatorios de esta copia sin borrar las notas nuevas que creaste después."
    ),
    REPLACE_ALL(
        label = "Reemplazar todo al estado exacto de la copia",
        description = "Sustituye toda la base de datos actual por esta copia de seguridad y reinicia la aplicación."
    )
}

data class BackupSnapshotInfo(
    val id: String,
    val fileName: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val notesCount: Int,
    val remindersCount: Int,
    val pdfsCount: Int = 0,
    val triggerType: String = "MANUAL",
    val accountEmail: String = "",
    val isUploadedToCloud: Boolean = false,
    val driveFileId: String? = null
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0L) return "0 KB"
            val kb = sizeBytes / 1024.0
            return if (kb < 1024.0) {
                String.format(Locale.US, "%.1f KB", kb)
            } else {
                val mb = kb / 1024.0
                String.format(Locale.US, "%.2f MB", mb)
            }
        }

    val triggerBadgeLabel: String
        get() = when (triggerType.uppercase(Locale.ROOT)) {
            "DAILY" -> "Diaria"
            "WEEKLY" -> "Semanal"
            "MONTHLY" -> "Mensual"
            "CLOUD" -> "Drive Cloud"
            else -> "Manual"
        }
}

sealed class BackupResult {
    object Success : BackupResult()
    data class AuthRequired(val intent: Intent) : BackupResult()
    data class Error(val exception: Exception, val message: String) : BackupResult()
}

sealed class RestoreResult {
    object Success : RestoreResult()
    object NotFound : RestoreResult()
    data class AuthRequired(val intent: Intent) : RestoreResult()
    data class Error(val exception: Exception, val message: String) : RestoreResult()
}
