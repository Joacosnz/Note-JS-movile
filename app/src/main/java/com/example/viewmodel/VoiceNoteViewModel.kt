package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceNoteViewModel(application: Application) : AndroidViewModel(application), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    // Texto final ya procesado (antes de la pausa)
    private var accumulatedText = ""
    // Texto que se está detectando en tiempo real
    private var currentPartialText = ""

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val restartRunnable = Runnable {
        if (_isListening.value) {
            startSpeechRecognizerInternal()
        }
    }

    fun startListening() {
        if (_isListening.value) return

        accumulatedText = ""
        currentPartialText = ""
        _transcription.value = ""
        _isListening.value = true

        scheduleRestart(0)
    }

    private fun startSpeechRecognizerInternal() {
        if (!_isListening.value) return

        mainHandler.post {
            try {
                // Siempre destruir el reconocedor anterior para evitar bloqueos/estado sucio
                destroySpeechRecognizer()

                val context = getApplication<Application>()
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer = recognizer
                    recognizer.setRecognitionListener(this@VoiceNoteViewModel)

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        // Forzar idioma Español (es-ES / es-AR) para evitar que use el idioma por defecto del sistema (ej. inglés)
                        val spanishLang = if (Locale.getDefault().language == "es") Locale.getDefault().toLanguageTag() else "es-ES"
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, spanishLang)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, spanishLang)
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("es-ES", "es-AR", "es-MX", "es"))
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra("android.speech.extra.DICTATION_MODE", true)
                        putExtra("android.speech.extra.PROFANITY_FILTER", false)
                        putExtra("android.speech.extras.PROFANITY_FILTER", false)
                        putExtra("profanity_filter", false)
                    }

                    recognizer.startListening(intent)
                } else {
                    _isListening.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_isListening.value) {
                    scheduleRestart(500)
                }
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        if (_isListening.value) {
            if (delayMs <= 0) {
                mainHandler.post(restartRunnable)
            } else {
                mainHandler.postDelayed(restartRunnable, delayMs)
            }
        }
    }

    private fun destroySpeechRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            speechRecognizer = null
        }
    }

    fun stopListening() {
        _isListening.value = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            destroySpeechRecognizer()
        }
    }

    fun cancelRecording() {
        accumulatedText = ""
        currentPartialText = ""
        _transcription.value = ""
        stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    private fun updateTranscription() {
        val separator = if (accumulatedText.isNotEmpty() && currentPartialText.isNotEmpty()) " " else ""
        _transcription.value = accumulatedText + separator + currentPartialText
    }

    // --- RecognitionListener Callbacks ---

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            currentPartialText = matches[0]
            updateTranscription()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val newText = matches[0]
            if (newText.isNotBlank()) {
                accumulatedText = if (accumulatedText.isEmpty()) newText else "$accumulatedText $newText"
                currentPartialText = ""
                updateTranscription()
            }
        }

        if (_isListening.value) {
            scheduleRestart(150)
        }
    }

    override fun onError(error: Int) {
        // Solo detener si se denegaron los permisos
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            _isListening.value = false
            return
        }

        // Ante errores transitorios (pausa de voz, pequeño parpadeo de red, recognizer ocupado, etc.):
        // Reiniciar limpiamente si el usuario sigue en modo de escucha
        if (_isListening.value) {
            scheduleRestart(300)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
