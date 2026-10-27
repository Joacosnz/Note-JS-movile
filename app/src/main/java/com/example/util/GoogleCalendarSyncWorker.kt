package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker de WorkManager encargado de ejecutar la sincronización bidireccional
 * de Recordatorios y Eventos con Google Calendar en segundo plano.
 */
class GoogleCalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!GoogleCalendarSyncManager.isSyncEnabled(applicationContext) ||
            !GoogleCalendarSyncManager.hasCalendarPermissions(applicationContext)
        ) {
            return Result.success()
        }

        return try {
            val result = GoogleCalendarSyncManager.syncBidirectionalWithGoogleCalendar(applicationContext)
            if (result.success) {
                Log.d(TAG, "Sincronización periódica con Google Calendar completada: ${result.message}")
                Result.success()
            } else {
                Log.w(TAG, "Sincronización con Google Calendar no completada: ${result.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en GoogleCalendarSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GoogleCalSyncWorker"
        private const val PERIODIC_WORK_NAME = "NoteJsGoogleCalendarPeriodicSync"
        private const val ONE_TIME_WORK_NAME = "NoteJsGoogleCalendarImmediateSync"

        fun schedulePeriodicSync(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<GoogleCalendarSyncWorker>(
                    1, TimeUnit.HOURS
                ).build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo programar sincronización periódica de Google Calendar: ${e.message}")
            }
        }

        fun cancelPeriodicSync(context: Context) {
            try {
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelando sincronización periódica de Google Calendar: ${e.message}")
            }
        }

        fun enqueueImmediateSync(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<GoogleCalendarSyncWorker>().build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error encolando sincronización inmediata de Google Calendar: ${e.message}")
            }
        }
    }
}
