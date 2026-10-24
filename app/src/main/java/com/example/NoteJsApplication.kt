package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import com.example.util.FirebaseInitializer
import com.example.util.LauncherIconManager
<<<<<<< Updated upstream
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
=======
>>>>>>> Stashed changes

class NoteJsApplication : Application() {

    private val configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                LauncherIconManager.syncLauncherIcon(
                    context = context,
                    newConfig = resources.configuration
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
<<<<<<< Updated upstream
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.fromResource(this)
                if (options != null) {
                    FirebaseApp.initializeApp(this, options)
                } else {
                    val dummyOptions = FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:321abc")
                        .setProjectId("dummy-project")
                        .setApiKey("dummy-api-key")
                        .build()
                    FirebaseApp.initializeApp(this, dummyOptions)
                }
            }
=======
            FirebaseInitializer.ensureInitialized(this)
>>>>>>> Stashed changes
        } catch (_: Throwable) {
        }
        LauncherIconManager.syncLauncherIcon(this, newConfig = resources.configuration)
        try {
            val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(configChangeReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(configChangeReceiver, filter)
            }
        } catch (_: Throwable) {
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LauncherIconManager.syncLauncherIcon(this, newConfig = newConfig)
    }
}
