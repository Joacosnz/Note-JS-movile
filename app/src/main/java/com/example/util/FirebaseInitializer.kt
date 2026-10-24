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
            FirebaseApp.initializeApp(appContext)
        } catch (e: Exception) {
            Log.w("FirebaseInitializer", "Standard FirebaseApp.initializeApp failed, attempting fallback options initialization", e)
            try {
                val options = FirebaseOptions.Builder()
                    .setProjectId("gen-lang-client-0927426378")
                    .setApplicationId("1:777612360426:android:edea042dd74e6e60be7593")
                    .setApiKey("AIzaSyB_vrmvhyviUX0fmaCLv1T6SRL6QT4lqPc")
                    .setStorageBucket("gen-lang-client-0927426378.firebasestorage.app")
                    .setGcmSenderId("777612360426")
                    .build()
                FirebaseApp.initializeApp(appContext, options)
            } catch (fallbackEx: Exception) {
                Log.e("FirebaseInitializer", "Fallback FirebaseApp initialization failed", fallbackEx)
                null
            }
        }
    }
}
