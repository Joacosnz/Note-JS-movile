package com.example.util

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
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

    suspend fun transcribeAudio(
        file: File, 
        userApiKey: String = "",
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists()) throw Exception("El archivo de audio no existe.")

        var apiKey = userApiKey.trim()
        
        if (apiKey.isBlank() && context != null) {
            apiKey = SettingsManager(context).getGeminiApiKey().trim()
        }
        
        if (apiKey.isBlank()) {
            apiKey = runCatching {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as? String ?: ""
            }.getOrDefault("")
        }

        if (apiKey.isBlank()) {
            throw Exception("No se ha configurado la API Key de Gemini. Agrega tu clave en los Ajustes de la app.")
        }

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
                            put("text", "Transcribe el siguiente audio en español. Responde ÚNICAMENTE con el texto transcrito, sin comentarios ni aclaraciones.")
                        })
                    })
                })
            })
        }

        val model = "gemini-1.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw Exception("Error API Gemini (${response.code}): $errBody")
            }

            val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía de Gemini")
            val jsonResponse = JSONObject(responseBody)
            
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val transcribedText = parts.getJSONObject(0).optString("text", "").trim()
                    if (transcribedText.isNotBlank()) {
                        return@withContext transcribedText
                    }
                }
            }
            throw Exception("No se pudo obtener la transcripción de la respuesta de Gemini.")
        }
    }
}
