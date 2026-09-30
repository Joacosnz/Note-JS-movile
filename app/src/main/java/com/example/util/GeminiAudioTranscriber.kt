package com.example.util

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64

object GeminiAudioTranscriber {
    private val client = OkHttpClient()

    // Este es un ejemplo de integración genérica con la API REST de Gemini.
    suspend fun transcribeAudio(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) throw Exception("El archivo de audio no existe.")

        val fileBytes = file.readBytes()
        val base64Audio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(fileBytes)
        } else {
            android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP)
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/mp4")
                                put("data", base64Audio)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Transcribe el siguiente audio. Responde únicamente con la transcripción.")
                        })
                    })
                })
            })
        }

        // Aquí se requiere una clave API válida de Gemini. Como mock para el desarrollo, 
        // puedes usar tu propia API key de Google AI Studio y el modelo "gemini-1.5-flash" (o el que corresponda).
        // El requerimiento dice que usemos "gemini-3.5-transcribe" conceptualmente.
        val apiKey = "DUMMY_API_KEY_OR_FETCH_FROM_BUILD_CONFIG" 
        val model = "gemini-3.5-transcribe" // gemini-1.5-flash-8b
        val url = "https://generativelanguage.googleapis.com/v1beta/models/\$model:generateContent?key=\$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        /* 
         * Nota: Si no tienes una API KEY válida, esto fallará con un HTTP 400/403.
         * Para probar en la app simulamos la respuesta si es "DUMMY_API_KEY".
         */
        if (apiKey.startsWith("DUMMY_API_KEY")) {
            delay(2000)
            return@withContext "Esta es una transcripción simulada del audio grabado, generada por el modelo $model."
        }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Error de red: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía")
            val jsonResponse = JSONObject(responseBody)
            
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "").trim()
                }
            }
            throw Exception("No se pudo obtener la transcripción del JSON.")
        }
    }
}
