package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.util.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker Offline-First encargado de drenar automáticamente la cola de cambios locales en Room
 * (`isSynced = 0` y `pendingDelete = 1`) hacia Firebase Firestore y Cloud Storage en cuanto
 * el sistema operativo detecta que la conexión a Internet se ha restablecido.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            FirebaseInitializer.ensureInitialized(applicationContext)
            val currentUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (_: Exception) {
                null
            }

            val uid = currentUser?.uid
            if (uid.isNullOrBlank() || uid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                // El usuario opera en sesión anónima local (Room); los cambios permanecen en cola local
                // hasta que vincule voluntariamente su cuenta de Google.
                Log.d(TAG, "Sesión local activa sin cuenta vinculada; cambios resguardados en Room.")
                return@withContext Result.success()
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val noteDao = database.noteDao()
            val firestoreManager = FirestoreManager(applicationContext)
            val repository = NoteRepository(
                noteDao = noteDao,
                folderDao = database.folderDao(),
                signatureDao = database.signatureDao(),
                firestoreManager = firestoreManager,
                firebaseStorageManager = FirebaseStorageManager(),
                context = applicationContext,
                reminderDao = database.reminderDao()
            )

            Log.i(TAG, "Ejecutando sincronización offline-first automática para UID=$uid")
            repository.syncPendingNotesSuspending(uid, forcePushAll = false)

            val remainingPending = noteDao.getPendingSyncCountNow(uid)
            if (remainingPending > 0) {
                Log.w(TAG, "Quedan $remainingPending operaciones pendientes en Room; reprogramando reintento.")
                Result.retry()
            } else {
                Log.i(TAG, "Cola offline de Room sincronizada exitosamente.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante OfflineSyncWorker, se reintentará al estabilizar la red", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "OfflineSyncWorker"
        const val UNIQUE_ONE_TIME_WORK = "OfflineNotesImmediateSyncWork"
        const val UNIQUE_PERIODIC_WORK = "OfflineNotesPeriodicSyncWork"

        /**
         * Encola una tarea en WorkManager que se ejecutará inmediatamente si hay red disponible,
         * o quedará esperando a nivel de sistema operativo hasta que se restablezca la conexión.
         */
        fun enqueueWhenConnected(context: Context?) {
            val appContext = context?.applicationContext ?: return
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_ONE_TIME_WORK,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo encolar OfflineSyncWorker en WorkManager: ${e.message}")
            }
        }

        /**
         * Programa una verificación periódica de respaldo para garantizar la consistencia
         * eventual entre Room y la nube.
         */
        fun schedulePeriodicSync(context: Context?) {
            val appContext = context?.applicationContext ?: return
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo programar sincronización periódica: ${e.message}")
            }
        }
    }
}
