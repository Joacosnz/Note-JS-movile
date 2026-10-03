package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sqrt

class TranscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _transcriptionText = MutableStateFlow("")
    val transcriptionText: StateFlow<String> = _transcriptionText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val sampleRate = 16000
    private val SILENCE_THRESHOLD_RMS = 0.01f

    // --- VARIABLES DE GRABACIÓN M4A ---
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var audioTrackIndex: Int = -1
    private var isMuxerStarted = false
    private var presentationTimeUs: Long = 0L
    private var audioChannel: Channel<ShortArray>? = null
    
    private val BIT_RATE = 64000
    private val TIMEOUT_US = 10000L

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2 

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, 
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            startAudioRecordingAndEncoding()
            audioRecord?.startRecording()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            val shortArray = ShortArray(bufferSize / 2)
            
            while (isActive) {
                val read = audioRecord?.read(shortArray, 0, shortArray.size) ?: 0
                if (read > 0) {
                    val copiedChunk = shortArray.copyOf(read) 
                    audioChannel?.trySend(copiedChunk)

                    val floatArray = FloatArray(read) { i -> shortArray[i] / 32768.0f }
                    val rms = calculateRMS(floatArray)
                    val normalizedLevel = (rms * 10f).coerceIn(0f, 1f)
                    _audioLevel.value = normalizedLevel
                }
            }
        }

        startSpeechRecognizer()
    }

    private fun startSpeechRecognizer() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {
                                val norm = ((rmsdB + 2f) / 10f).coerceIn(0f, 1f)
                                if (norm > _audioLevel.value) {
                                    _audioLevel.value = norm
                                }
                            }
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                if (_isListening.value) {
                                    viewModelScope.launch(Dispatchers.Main) {
                                        delay(400)
                                        if (_isListening.value) {
                                            startSpeechRecognizer()
                                        }
                                    }
                                }
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    val text = matches[0]
                                    if (text.isNotBlank()) {
                                        val current = _transcriptionText.value
                                        _transcriptionText.value = if (current.isBlank()) text else "$current $text"
                                    }
                                }
                                if (_isListening.value) {
                                    viewModelScope.launch(Dispatchers.Main) {
                                        if (_isListening.value) startSpeechRecognizer()
                                    }
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    val text = matches[0]
                                    if (text.isNotBlank()) {
                                        val current = _transcriptionText.value
                                        val lastSpaceIdx = current.lastIndexOf(' ')
                                        val base = if (lastSpaceIdx > 0 && current.length > 20) {
                                            current.substring(0, lastSpaceIdx + 1)
                                        } else if (current.isBlank()) {
                                            ""
                                        } else {
                                            current
                                        }
                                        _transcriptionText.value = if (base.isBlank()) text else "$base $text"
                                    }
                                }
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        val spanishLang = if (Locale.getDefault().language == "es") Locale.getDefault().toLanguageTag() else "es-ES"
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, spanishLang)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, spanishLang)
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("es-ES", "es-AR", "es-MX", "es"))
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra("android.speech.extra.PROFANITY_FILTER", false)
                        putExtra("android.speech.extras.PROFANITY_FILTER", false)
                        putExtra("android.speech.extra.HIDE_PARTIAL_OFFENSIVE_WORDS", false)
                        putExtra("android.speech.extra.DICTATION_MODE", true)
                        putExtra("profanity_filter", false)
                        putExtra("hide_partial_offensive_words", false)
                        putExtra("mask_offensive_words", false)
                        putExtra("suppress_profanity", false)
                        putExtra("filter_profanity", false)
                    }
                    speechRecognizer?.startListening(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopListening() {
        _isListening.value = false
        _audioLevel.value = 0f
        
        recordingJob?.cancel()
        recordingJob = null
        
        audioRecord?.apply {
            try {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        audioRecord = null

        viewModelScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                speechRecognizer = null
            }
        }
        
        audioChannel?.close()
        audioChannel = null
    }

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun updateTranscription(newText: String) {
        _transcriptionText.value = newText
    }

    fun clearTranscription() {
        _transcriptionText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    private fun calculateRMS(audioData: FloatArray): Float {
        var sumSquares = 0.0f
        for (sample in audioData) {
            sumSquares += (sample * sample)
        }
        return sqrt(sumSquares / audioData.size)
    }

    // --- FUNCIONES DE CODIFICACIÓN PARALELA ---

    private fun startAudioRecordingAndEncoding() {
        val outputFile = File(getApplication<Application>().filesDir, "transcription_${System.currentTimeMillis()}.m4a")
        
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            audioTrackIndex = -1
            isMuxerStarted = false
            presentationTimeUs = 0L
            
            audioChannel = Channel(Channel.UNLIMITED)

            viewModelScope.launch(Dispatchers.Default) {
                audioChannel?.consumeEach { chunk ->
                    encodeAudioChunk(chunk, isEndOfStream = false)
                }
                encodeAudioChunk(ShortArray(0), isEndOfStream = true)
                releaseEncoders()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encodeAudioChunk(audioChunk: ShortArray, isEndOfStream: Boolean) {
        val codec = mediaCodec ?: return
        
        try {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: return
                inputBuffer.clear()
                
                var flags = 0
                val byteCount = audioChunk.size * 2

                if (isEndOfStream) {
                    flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                } else {
                    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val shortBuffer = inputBuffer.asShortBuffer()
                    shortBuffer.put(audioChunk)
                }

                codec.queueInputBuffer(inputBufferIndex, 0, byteCount, presentationTimeUs, flags)
                presentationTimeUs += (audioChunk.size * 1_000_000L) / sampleRate
            }

            drainCodecOutput(codec, isEndOfStream)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drainCodecOutput(codec: MediaCodec, isEosSent: Boolean) {
        val bufferInfo = MediaCodec.BufferInfo()
        
        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!isEosSent) break
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    mediaMuxer?.let { muxer ->
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        isMuxerStarted = true
                    }
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && isMuxerStarted) {
                        outputBuffer?.position(bufferInfo.offset)
                        outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer?.writeSampleData(audioTrackIndex, outputBuffer!!, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    private fun releaseEncoders() {
        try {
            mediaCodec?.apply {
                stop()
                release()
            }
            mediaMuxer?.apply {
                if (isMuxerStarted) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaCodec = null
            mediaMuxer = null
            isMuxerStarted = false
        }
    }
}
