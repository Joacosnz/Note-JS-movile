package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.FirebaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Inicializando MainApplication...")

        // Thread-safe Firebase and Room initialization
        synchronized(this) {
            initializeFirebase()
            validateRoomDatabaseConnection()
        }
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
        private const val TAG = "MainApplication"
    }
}
