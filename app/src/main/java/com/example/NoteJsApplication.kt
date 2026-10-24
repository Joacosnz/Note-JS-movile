package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.FirebaseInitializer

class NoteJsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Inicializando NoteJsApplication...")

        // 1. Inicialización segura de Firebase (Auth, Firestore, Cloud Storage)
        try {
            val app = FirebaseInitializer.ensureInitialized(this)
            if (app != null) {
                Log.i(TAG, "FirebaseApp inicializado correctamente: ${app.name}")
            } else {
                Log.w(TAG, "FirebaseApp no se pudo inicializar en este entorno; se operará en modo completamente local.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error no crítico durante la inicialización de Firebase", t)
        }

        // 2. Inicialización y migración previa de Room Database
        try {
            val db = AppDatabase.getDatabase(this)
            Log.i(TAG, "Base de datos Room cargada correctamente (versión ${db.openHelper.readableDatabase.version})")
        } catch (t: Throwable) {
            Log.e(TAG, "Error no crítico durante la inicialización de Room Database", t)
        }
    }

    companion object {
        private const val TAG = "NoteJsApplication"
    }
}
