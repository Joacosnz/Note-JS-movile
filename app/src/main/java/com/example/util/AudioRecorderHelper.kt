package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

object AudioRecorderHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    /**
     * Inicia la grabación de audio en alta calidad para acompañar el dictado nativo.
     * Guarda el archivo físico (.m4a) en context.filesDir y retorna su ruta.
     */
    fun startRecording(context: Context, fileName: String = "audio_${System.currentTimeMillis()}.m4a"): String {
        stopRecording() // Limpiar cualquier grabación previa activa

        val outputFile = if (fileName.startsWith("/")) {
            File(fileName)
        } else {
            File(context.filesDir, fileName)
        }
        outputFile.parentFile?.mkdirs()

        val path = outputFile.absolutePath
        currentFilePath = path

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setAudioChannels(1)
            setOutputFile(path)
            try {
                prepare()
                start()
            } catch (_: Exception) {
                try {
                    reset()
                    setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)
                    setAudioChannels(1)
                    setOutputFile(path)
                    prepare()
                    start()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    release()
                    mediaRecorder = null
                    currentFilePath = null
                    throw e2
                }
            }
        }

        mediaRecorder = recorder
        return path
    }

    /**
     * Detiene la grabación actual y libera el MediaRecorder correctamente.
     * Retorna la ruta absoluta del archivo grabado.
     */
    fun stopRecording(): String? {
        val path = currentFilePath
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            currentFilePath = null
        }
        return path
    }

    /**
     * Libera los recursos del MediaRecorder.
     */
    fun release() {
        stopRecording()
    }
}
