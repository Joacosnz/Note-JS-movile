package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object AppPreferences {
    val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("key_is_first_launch")

    fun getIsFirstLaunchFlow(context: Context): Flow<Boolean> {
        return context.appDataStore.data.map { prefs ->
            prefs[KEY_IS_FIRST_LAUNCH] ?: true
        }
    }

    suspend fun getIsFirstLaunch(context: Context): Boolean {
        return try {
            getIsFirstLaunchFlow(context).first()
        } catch (_: Exception) {
            true
        }
    }

    suspend fun setFirstLaunchCompleted(context: Context) {
        try {
            context.appDataStore.edit { prefs ->
                prefs[KEY_IS_FIRST_LAUNCH] = false
            }
        } catch (_: Exception) {
        }
    }
}
