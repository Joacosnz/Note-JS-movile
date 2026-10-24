package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

enum class CalendarItemType(val code: String, val label: String) {
    REMINDER("REMINDER", "Recordatorio"),
    EVENT("EVENT", "Evento");

    companion object {
        fun fromCode(code: String?): CalendarItemType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: REMINDER
        }
    }
}

enum class RecurrenceRule(val code: String, val label: String) {
    NONE("NONE", "Nunca"),
    DAILY("DAILY", "Todos los días"),
    WEEKDAYS("WEEKDAYS", "Días laborables (L-V)"),
    WEEKLY("WEEKLY", "Cada semana"),
    MONTHLY("MONTHLY", "Cada mes"),
    YEARLY("YEARLY", "Cada año");

    companion object {
        fun fromCode(code: String?): RecurrenceRule {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NONE
        }
    }
}

data class ReminderSubtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

object ReminderSubtaskSerializer {
    fun serialize(subtasks: List<ReminderSubtask>): String {
        if (subtasks.isEmpty()) return "[]"
        val arr = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserialize(json: String?): List<ReminderSubtask> {
        if (json.isNullOrBlank() || !json.trim().startsWith("[")) return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<ReminderSubtask>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isNotEmpty()) {
                    result.add(
                        ReminderSubtask(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = title,
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class CalendarCategoryPreset(
    val name: String,
    val colorHex: Long
)

object CalendarCategoryPresets {
    val defaults = listOf(
        CalendarCategoryPreset("Personal", 0xFF007AFFL),
        CalendarCategoryPreset("Trabajo", 0xFFFF9500L),
        CalendarCategoryPreset("Estudio", 0xFFAF52DEL),
        CalendarCategoryPreset("Compras", 0xFF34C759L),
        CalendarCategoryPreset("Salud", 0xFFFF2D55L),
        CalendarCategoryPreset("Finanzas", 0xFF5AC8FAL),
        CalendarCategoryPreset("Proyectos", 0xFF5856D6L)
    )

    fun colorForList(name: String, fallback: Long = 0xFF007AFFL): Long {
        return defaults.firstOrNull { it.name.equals(name, ignoreCase = true) }?.colorHex ?: fallback
    }
}

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["remoteUuid"]),
        Index(value = ["userId", "pendingDelete", "scheduledAt"]),
        Index(value = ["linkedNoteId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "''")
    val remoteUuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'LOCAL_ANONYMOUS'")
    val userId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID,
    @ColumnInfo(defaultValue = "'REMINDER'")
    val itemType: String = CalendarItemType.REMINDER.code,
    val title: String,
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(defaultValue = "''")
    val location: String = "",
    val scheduledAt: Long,
    @ColumnInfo(defaultValue = "0")
    val endTimeMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val isAllDay: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val completedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val priority: Int = 0, // 0=Ninguna, 1=Baja (!), 2=Media (!!), 3=Alta (!!!)
    @ColumnInfo(defaultValue = "'Personal'")
    val listName: String = "Personal",
    @ColumnInfo(defaultValue = "4278221567")
    val colorHex: Long = 0xFF007AFFL,
    @ColumnInfo(defaultValue = "'NONE'")
    val recurrenceRule: String = RecurrenceRule.NONE.code,
    @ColumnInfo(defaultValue = "'[]'")
    val subtasksJson: String = "[]",
    val linkedNoteId: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val linkedNoteUuid: String = "",
    @ColumnInfo(defaultValue = "''")
    val linkedNoteTitle: String = "",
    @ColumnInfo(defaultValue = "0")
    val alertOffsetMinutes: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val pendingDelete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val lastModifiedLocal: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtServer: Long = 0L
) {
    val isEvent: Boolean
        get() = itemType.equals(CalendarItemType.EVENT.code, ignoreCase = true)

    val isReminder: Boolean
        get() = !isEvent

    val description: String
        @androidx.room.Ignore get() = notes

    val timestamp: Long
        @androidx.room.Ignore get() = scheduledAt

    fun toReminder(): Reminder = Reminder.fromReminderEntity(this)

    fun toCalendarEvent(): CalendarEvent = CalendarEvent.fromReminderEntity(this)

    val effectiveEndMillis: Long
        get() = if (isEvent) {
            if (endTimeMillis > scheduledAt) endTimeMillis else scheduledAt + 3_600_000L
        } else {
            if (endTimeMillis > scheduledAt) endTimeMillis else scheduledAt + 1_800_000L
        }

    val subtasks: List<ReminderSubtask>
        get() = ReminderSubtaskSerializer.deserialize(subtasksJson)

    fun computeNextOccurrenceMillis(): Long? {
        val rule = RecurrenceRule.fromCode(recurrenceRule)
        if (rule == RecurrenceRule.NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = scheduledAt }
        when (rule) {
            RecurrenceRule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceRule.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                } while (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
            }
            RecurrenceRule.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceRule.YEARLY -> cal.add(Calendar.YEAR, 1)
            RecurrenceRule.NONE -> return null
        }
        return cal.timeInMillis
    }

    /**
     * Checks if this item (or its recurrence projection) falls on the given calendar day [dayStartMillis, dayEndMillis].
     * If it is a recurring item, returns a projected copy with `scheduledAt` shifted to that day's hour/minute.
     */
    fun projectOntoDay(dayStartMillis: Long, dayEndMillis: Long): ReminderEntity? {
        if (scheduledAt in dayStartMillis..dayEndMillis) {
            return this
        }
        if (isEvent && !isAllDay && scheduledAt < dayEndMillis && effectiveEndMillis > dayStartMillis) {
            return this
        }
        val rule = RecurrenceRule.fromCode(recurrenceRule)
        if (rule == RecurrenceRule.NONE) return null

        val itemDayStart = startOfDayMillis(scheduledAt)
        val targetDayStart = startOfDayMillis(dayStartMillis)
        if (targetDayStart <= itemDayStart) return null
        if (isReminder && isCompleted) return null

        val sourceCal = Calendar.getInstance().apply { timeInMillis = scheduledAt }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = targetDayStart
            set(Calendar.HOUR_OF_DAY, sourceCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, sourceCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, sourceCal.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, 0)
        }

        val matches = when (rule) {
            RecurrenceRule.DAILY -> true
            RecurrenceRule.WEEKDAYS -> {
                val dow = targetCal.get(Calendar.DAY_OF_WEEK)
                dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
            }
            RecurrenceRule.WEEKLY -> {
                targetCal.get(Calendar.DAY_OF_WEEK) == sourceCal.get(Calendar.DAY_OF_WEEK)
            }
            RecurrenceRule.MONTHLY -> {
                targetCal.get(Calendar.DAY_OF_MONTH) == sourceCal.get(Calendar.DAY_OF_MONTH)
            }
            RecurrenceRule.YEARLY -> {
                targetCal.get(Calendar.DAY_OF_MONTH) == sourceCal.get(Calendar.DAY_OF_MONTH) &&
                    targetCal.get(Calendar.MONTH) == sourceCal.get(Calendar.MONTH)
            }
            RecurrenceRule.NONE -> false
        }

        if (!matches) return null
        val duration = (endTimeMillis - scheduledAt).coerceAtLeast(0L)
        val newStart = targetCal.timeInMillis
        return copy(
            scheduledAt = newStart,
            endTimeMillis = if (duration > 0L) newStart + duration else 0L
        )
    }

    companion object {
        fun startOfDayMillis(timestamp: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
