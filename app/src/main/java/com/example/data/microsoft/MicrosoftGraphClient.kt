package com.example.data.microsoft

import android.content.Context
import android.util.Log
import com.example.auth.microsoft.MicrosoftAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class MicrosoftCalendarEvent(
    val id: String = "",
    val subject: String,
    val body: String = "",
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean = false,
    val webLink: String = "",
    val categories: List<String> = emptyList()
)

data class OneDriveItem(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTimeMillis: Long,
    val webUrl: String = "",
    val downloadUrl: String = ""
)

/**
 * Cliente HTTP para comunicarse directamente con la API REST de Microsoft Graph (v1.0).
 * Soporta la gestión completa de eventos de Outlook Calendar y copias de seguridad de archivos en OneDrive.
 */
class MicrosoftGraphClient(private val context: Context) {

    private val authManager = MicrosoftAuthManager(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun getAuthToken(): String? {
        return authManager.getValidAccessToken()
    }

    private fun buildRequest(url: String, method: String = "GET", body: okhttp3.RequestBody? = null): Request? {
        val token = getAuthToken() ?: return null
        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(body ?: "".toRequestBody("application/json".toMediaType()))
            "PATCH" -> builder.patch(body ?: "".toRequestBody("application/json".toMediaType()))
            "PUT" -> builder.put(body ?: "".toRequestBody("application/octet-stream".toMediaType()))
            "DELETE" -> builder.delete()
        }
        return builder.build()
    }

    // ==========================================
    // SECCIÓN: OUTLOOK CALENDAR (EVENTOS)
    // ==========================================

    /**
     * Lista eventos del calendario del usuario en un rango temporal dado.
     */
    suspend fun listCalendarEvents(startMillis: Long, endMillis: Long): List<MicrosoftCalendarEvent> = withContext(Dispatchers.IO) {
        val startIso = isoFormat.format(Date(startMillis))
        val endIso = isoFormat.format(Date(endMillis))
        val url = "https://graph.microsoft.com/v1.0/me/calendarView?startDateTime=${startIso}Z&endDateTime=${endIso}Z&\$top=100&\$select=id,subject,bodyPreview,start,end,isAllDay,webLink,categories"

        val req = buildRequest(url) ?: return@withContext emptyList()
        try {
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.w(TAG, "Error listando eventos de Outlook: HTTP ${resp.code}")
                return@withContext emptyList()
            }
            val bodyStr = resp.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyStr)
            val valueArr = json.optJSONArray("value") ?: JSONArray()
            val list = mutableListOf<MicrosoftCalendarEvent>()

            for (i in 0 until valueArr.length()) {
                val item = valueArr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val subject = item.optString("subject", "(Sin título)")
                val bodyPreview = item.optString("bodyPreview", "")
                val isAllDay = item.optBoolean("isAllDay", false)
                val webLink = item.optString("webLink", "")

                val startObj = item.optJSONObject("start")
                val endObj = item.optJSONObject("end")

                val sStr = startObj?.optString("dateTime") ?: ""
                val eStr = endObj?.optString("dateTime") ?: ""

                val sTime = parseIsoToMillis(sStr) ?: startMillis
                val eTime = parseIsoToMillis(eStr) ?: (sTime + 3600_000L)

                val cats = mutableListOf<String>()
                val catArr = item.optJSONArray("categories")
                if (catArr != null) {
                    for (c in 0 until catArr.length()) {
                        cats.add(catArr.optString(c))
                    }
                }

                list.add(
                    MicrosoftCalendarEvent(
                        id = id,
                        subject = subject,
                        body = bodyPreview,
                        startMillis = sTime,
                        endMillis = eTime,
                        isAllDay = isAllDay,
                        webLink = webLink,
                        categories = cats
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Excepción listando eventos de Outlook", e)
            emptyList()
        }
    }

    /**
     * Crea un nuevo evento en Outlook Calendar mediante Microsoft Graph.
     */
    suspend fun createCalendarEvent(
        subject: String,
        bodyContent: String,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/calendar/events"
        val payload = JSONObject().apply {
            put("subject", subject)
            put("body", JSONObject().apply {
                put("contentType", "text")
                put("content", bodyContent)
            })
            put("start", JSONObject().apply {
                put("dateTime", isoFormat.format(Date(startMillis)))
                put("timeZone", "UTC")
            })
            put("end", JSONObject().apply {
                put("dateTime", isoFormat.format(Date(if (endMillis > startMillis) endMillis else startMillis + 3600_000L)))
                put("timeZone", "UTC")
            })
            put("isAllDay", isAllDay)
            put("categories", JSONArray().apply {
                put("NoteJS")
            })
        }

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = buildRequest(url, method = "POST", body = body)
            ?: return@withContext Result.failure(Exception("No autenticado en Microsoft"))

        try {
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val respBody = resp.body?.string() ?: ""
                val respJson = JSONObject(respBody)
                val newId = respJson.optString("id")
                Result.success(newId)
            } else {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Log.w(TAG, "Error creando evento en Outlook: $err")
                Result.failure(Exception("Error Outlook Calendar: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción creando evento en Outlook", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza un evento existente en Outlook Calendar.
     */
    suspend fun updateCalendarEvent(
        eventId: String,
        subject: String,
        bodyContent: String,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/calendar/events/$eventId"
        val payload = JSONObject().apply {
            put("subject", subject)
            put("body", JSONObject().apply {
                put("contentType", "text")
                put("content", bodyContent)
            })
            put("start", JSONObject().apply {
                put("dateTime", isoFormat.format(Date(startMillis)))
                put("timeZone", "UTC")
            })
            put("end", JSONObject().apply {
                put("dateTime", isoFormat.format(Date(if (endMillis > startMillis) endMillis else startMillis + 3600_000L)))
                put("timeZone", "UTC")
            })
            put("isAllDay", isAllDay)
        }

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = buildRequest(url, method = "PATCH", body = body)
            ?: return@withContext Result.failure(Exception("No autenticado en Microsoft"))

        try {
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Result.failure(Exception("Error actualizando evento: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un evento de Outlook Calendar.
     */
    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/calendar/events/$eventId"
        val req = buildRequest(url, method = "DELETE")
            ?: return@withContext Result.failure(Exception("No autenticado en Microsoft"))

        try {
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful || resp.code == 404) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error eliminando evento de Outlook: HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: ONEDRIVE / ARCHIVOS Y COPIAS
    // ==========================================

    /**
     * Sube un archivo de copia de seguridad (ZIP o JSON) directamente a OneDrive en la carpeta /NoteJS_Backups/.
     */
    suspend fun uploadBackupFile(fileName: String, fileBytes: ByteArray): Result<OneDriveItem> = withContext(Dispatchers.IO) {
        val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
        val url = "https://graph.microsoft.com/v1.0/me/drive/root:/NoteJS_Backups/$encodedName:/content"

        val body = fileBytes.toRequestBody("application/zip".toMediaType())
        val req = buildRequest(url, method = "PUT", body = body)
            ?: return@withContext Result.failure(Exception("No autenticado en Microsoft"))

        try {
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val item = OneDriveItem(
                    id = json.optString("id"),
                    name = json.optString("name", fileName),
                    sizeBytes = json.optLong("size", fileBytes.size.toLong()),
                    modifiedTimeMillis = parseIsoToMillis(json.optString("lastModifiedDateTime")) ?: System.currentTimeMillis(),
                    webUrl = json.optString("webUrl")
                )
                Result.success(item)
            } else {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Log.w(TAG, "Error subiendo a OneDrive: $err")
                Result.failure(Exception("Error OneDrive: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción subiendo archivo a OneDrive", e)
            Result.failure(e)
        }
    }

    /**
     * Lista todas las copias de seguridad existentes dentro de la carpeta /NoteJS_Backups/ de OneDrive.
     */
    suspend fun listBackupFiles(): List<OneDriveItem> = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/drive/root:/NoteJS_Backups:/children?\$select=id,name,size,lastModifiedDateTime,webUrl"
        val req = buildRequest(url) ?: return@withContext emptyList()

        try {
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.w(TAG, "No se pudo listar la carpeta /NoteJS_Backups en OneDrive: HTTP ${resp.code}")
                return@withContext emptyList()
            }
            val bodyStr = resp.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyStr)
            val valArr = json.optJSONArray("value") ?: JSONArray()
            val list = mutableListOf<OneDriveItem>()

            for (i in 0 until valArr.length()) {
                val item = valArr.optJSONObject(i) ?: continue
                val name = item.optString("name")
                if (!name.endsWith(".zip", ignoreCase = true) && !name.endsWith(".json", ignoreCase = true)) continue

                list.add(
                    OneDriveItem(
                        id = item.optString("id"),
                        name = name,
                        sizeBytes = item.optLong("size", 0L),
                        modifiedTimeMillis = parseIsoToMillis(item.optString("lastModifiedDateTime")) ?: System.currentTimeMillis(),
                        webUrl = item.optString("webUrl")
                    )
                )
            }
            list.sortedByDescending { it.modifiedTimeMillis }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción listando copias de OneDrive", e)
            emptyList()
        }
    }

    /**
     * Descarga el contenido binario de un archivo de respaldo desde OneDrive por su ID de elemento.
     */
    suspend fun downloadBackupFile(itemId: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/$itemId/content"
        val req = buildRequest(url) ?: return@withContext null

        try {
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                resp.body?.bytes()
            } else {
                Log.w(TAG, "Error descargando archivo de OneDrive $itemId: HTTP ${resp.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción descargando de OneDrive", e)
            null
        }
    }

    /**
     * Elimina un archivo de copia de seguridad de OneDrive.
     */
    suspend fun deleteBackupFile(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/$itemId"
        val req = buildRequest(url, method = "DELETE") ?: return@withContext false

        try {
            val resp = httpClient.newCall(req).execute()
            resp.isSuccessful || resp.code == 404
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando elemento de OneDrive", e)
            false
        }
    }

    private fun parseIsoToMillis(isoString: String): Long? {
        if (isoString.isBlank()) return null
        return try {
            val clean = isoString.replace("Z", "").substringBefore(".")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(clean)?.time
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MicrosoftGraphClient"
    }
}
