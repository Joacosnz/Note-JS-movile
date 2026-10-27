package com.example

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.example.data.local.AppDatabase
import com.example.util.FirebaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class MyApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Inicializando MyApplication...")

        // Thread-safe Firebase and Room initialization
        synchronized(this) {
            initializeFirebase()
            validateRoomDatabaseConnection()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    private fun initializeFirebase() {
        try {
            val app = FirebaseInitializer.ensureInitialized(this)
            if (app != null) {
                Log.i(TAG, "FirebaseApp inicializado exitosamente: ${app.name}")
            } else {
                Log.w(TAG, "FirebaseApp no se pudo inicializar; operando en modo local.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Excepción durante la inicialización de Firebase", t)
        }
    }

    private fun validateRoomDatabaseConnection() {
        try {
            val db = AppDatabase.getDatabase(this)
            applicationScope.launch {
                try {
                    val isDbOpen = db.openHelper.writableDatabase.isOpen
                    Log.i(TAG, "Conexión a Room Database validada exitosamente (isOpen=$isDbOpen, versión=${db.openHelper.readableDatabase.version})")
                } catch (t: Throwable) {
                    Log.e(TAG, "Fallo al validar la conexión de la base de datos Room", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Excepción al acceder a AppDatabase singleton", t)
        }
    }

    companion object {
        private const val TAG = "MyApplication"
    }
}
