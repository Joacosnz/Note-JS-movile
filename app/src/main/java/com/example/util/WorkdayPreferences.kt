package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.workdayDataStore: DataStore<Preferences> by preferencesDataStore(name = "workday_preferences")

/**
 * Gestor DataStore Preferences para la configuración de días laborales del usuario.
 * Representación de los días de la semana de java.time.DayOfWeek (1=Lunes, 2=Martes, ..., 7=Domingo).
 */
object WorkdayPreferences {
    val KEY_WORKDAYS = stringSetPreferencesKey("key_workdays")

    // Días laborales por defecto: Lunes a Viernes (1..5)
    val DEFAULT_WORKDAYS: Set<String> = setOf("1", "2", "3", "4", "5")

    fun getWorkdaysFlow(context: Context): Flow<Set<Int>> {
        return context.workdayDataStore.data.map { prefs ->
            val rawSet = prefs[KEY_WORKDAYS] ?: DEFAULT_WORKDAYS
            val parsed = rawSet.mapNotNull { it.toIntOrNull() }.toSet()
            if (parsed.isEmpty()) setOf(1, 2, 3, 4, 5) else parsed
        }
    }

    suspend fun getWorkdays(context: Context): Set<Int> {
        return try {
            getWorkdaysFlow(context).first()
        } catch (_: Exception) {
            setOf(1, 2, 3, 4, 5)
        }
    }

    suspend fun setWorkdays(context: Context, days: Set<Int>) {
        try {
            context.workdayDataStore.edit { prefs ->
                prefs[KEY_WORKDAYS] = days.map { it.toString() }.toSet()
            }
        } catch (_: Exception) {
        }
    }
}
