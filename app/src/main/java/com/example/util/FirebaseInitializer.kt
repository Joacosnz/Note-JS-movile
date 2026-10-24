package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    fun ensureInitialized(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        val existingApps = try { FirebaseApp.getApps(appContext) } catch (_: Exception) { emptyList() }
        if (existingApps.isNotEmpty()) {
            return try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        }

        return try {
            val initialized = FirebaseApp.initializeApp(appContext)
            if (initialized != null) {
                return initialized
            }
            val resourceOptions = FirebaseOptions.fromResource(appContext)
            if (resourceOptions != null) {
                FirebaseApp.initializeApp(appContext, resourceOptions)
            } else {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setProjectId("isentropic-tape-465216-c1")
                    .setApplicationId("1:318447528237:android:34d1174d2c870ef15e8ba4")
                    .setApiKey("AIzaSyBzdXYpv5DdO9sapIdLcAhUopI8Cw_abk0")
                    .setStorageBucket("isentropic-tape-465216-c1.firebasestorage.app")
                    .setGcmSenderId("318447528237")
                    .build()
                FirebaseApp.initializeApp(appContext, fallbackOptions)
            }
        } catch (e: Exception) {
            Log.e("FirebaseInitializer", "FirebaseApp initialization failed", e)
            null
        }
    }
}
