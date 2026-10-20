package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class FirebaseStorageManager {

    private val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            null
        }

    /**
     * Sube el archivo local (ya sea content://, file:// o ruta absoluta) a Firebase Storage 
     * en la ruta users/{userId}/notes_media/{fileName} y retorna la URL de descarga.
     */
    suspend fun uploadFile(userId: String, localUriString: String, fileName: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || localUriString.isBlank()) return@withContext null

        val uri: Uri = if (localUriString.startsWith("content://") || localUriString.startsWith("file://")) {
            Uri.parse(localUriString)
        } else {
            val file = File(localUriString)
            if (!file.exists()) return@withContext null
            Uri.fromFile(file)
        }

        val storageInstance = storage ?: return@withContext null
        try {
            val safeFileName = if (fileName.isNotBlank()) fileName else Uri.parse(localUriString).lastPathSegment ?: "unknown_file"
            val ref = storageInstance.reference
                .child("users")
                .child(userId)
                .child("notes_media")
                .child(safeFileName)

            Log.d("StorageSync", "Subiendo archivo: $localUriString")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d("StorageSync", "Archivo subido con éxito: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("StorageSync", "Error al subir el archivo $localUriString", e)
            null
        }
    }

    /**
     * Descarga el archivo desde [remoteUrl] al almacenamiento local en [destinationLocalPath] si no existe.
     */
    suspend fun downloadFile(remoteUrl: String, destinationLocalPath: String): Boolean = withContext(Dispatchers.IO) {
        if (remoteUrl.isBlank() || destinationLocalPath.isBlank()) return@withContext false
        val destFile = File(destinationLocalPath)
        if (destFile.exists() && destFile.length() > 0) {
            return@withContext true
        }

        val parentDir = destFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val storageInstance = storage ?: return@withContext false
        try {
            Log.d("StorageSync", "Descargando archivo desde: $remoteUrl a $destinationLocalPath")
            val ref = storageInstance.getReferenceFromUrl(remoteUrl)
            ref.getFile(destFile).await()
            val success = destFile.exists() && destFile.length() > 0
            if (success) {
                Log.d("StorageSync", "Archivo descargado exitosamente en $destinationLocalPath")
            } else {
                Log.e("StorageSync", "Error: El archivo descargado no existe o está vacío en $destinationLocalPath")
            }
            success
        } catch (e: Exception) {
            Log.e("StorageSync", "Error al descargar el archivo desde $remoteUrl", e)
            false
        }
    }
}

