package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {

    const val NOTE_JS_DATABASE_NAME = "NoteJsDataBase"

    private fun buildDefaultOptions(appContext: Context): FirebaseOptions {
        return FirebaseOptions.fromResource(appContext)
            ?: FirebaseOptions.Builder()
                .setProjectId("isentropic-tape-465216-c1")
                .setApplicationId("1:318447528237:android:34d1174d2c870ef15e8ba4")
                .setApiKey("AIzaSyBzdXYpv5DdO9sapIdLcAhUopI8Cw_abk0")
                .setStorageBucket("isentropic-tape-465216-c1.firebasestorage.app")
                .setGcmSenderId("318447528237")
                .build()
    }

    /**
     * Inicializa tanto la instancia por defecto de FirebaseApp como la instancia nombrada
     * "NoteJsDataBase" para garantizar compatibilidad total con Remote Config y Firestore.
     */
    fun ensureInitialized(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        val defaultApp = ensureDefaultAppInitialized(appContext)
        ensureNamedAppInitialized(appContext, defaultApp?.options)
        return defaultApp
    }

    /**
     * Obtiene o inicializa la instancia de FirebaseApp renombrada como "NoteJsDataBase",
     * con fallback seguro a la instancia principal por defecto.
     */
    fun getNoteJsApp(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        val defaultApp = ensureDefaultAppInitialized(appContext)
        return ensureNamedAppInitialized(appContext, defaultApp?.options) ?: defaultApp
    }

    private fun ensureDefaultAppInitialized(appContext: Context): FirebaseApp? {
        val existingApps = try {
            FirebaseApp.getApps(appContext)
        } catch (_: Exception) {
            emptyList()
        }
        if (existingApps.any { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
            return try {
                FirebaseApp.getInstance()
            } catch (_: Exception) {
                null
            }
        }

        return try {
            val initialized = FirebaseApp.initializeApp(appContext)
            if (initialized != null) {
                initialized
            } else {
                val options = buildDefaultOptions(appContext)
                FirebaseApp.initializeApp(appContext, options)
            }
        } catch (e: Exception) {
            Log.e("FirebaseInitializer", "Default FirebaseApp initialization failed", e)
            null
        }
    }

    private fun ensureNamedAppInitialized(
        appContext: Context,
        baseOptions: FirebaseOptions?
    ): FirebaseApp? {
        val existingApps = try {
            FirebaseApp.getApps(appContext)
        } catch (_: Exception) {
            emptyList()
        }
        val existingNamed = existingApps.firstOrNull {
            it.name.equals(NOTE_JS_DATABASE_NAME, ignoreCase = true)
        }
        if (existingNamed != null) {
            return existingNamed
        }

        return try {
            val options = baseOptions ?: buildDefaultOptions(appContext)
            FirebaseApp.initializeApp(appContext, options, NOTE_JS_DATABASE_NAME)
        } catch (e: Exception) {
            Log.w("FirebaseInitializer", "Named FirebaseApp '$NOTE_JS_DATABASE_NAME' initialization fallback", e)
            try {
                FirebaseApp.getInstance(NOTE_JS_DATABASE_NAME)
            } catch (_: Exception) {
                null
            }
        }
    }
}
