package com.example.data.remote

import android.net.Uri
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
     * Sube el archivo físico local a Firebase Storage en la ruta users/{userId}/notes_media/{fileName}
     * y retorna la URL de descarga.
     */
    suspend fun uploadFile(userId: String, localPath: String, fileName: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || localPath.isBlank()) return@withContext null
        val file = File(localPath)
        if (!file.exists()) return@withContext null

        val storageInstance = storage ?: return@withContext null
        try {
            val safeFileName = if (fileName.isNotBlank()) fileName else file.name
            val ref = storageInstance.reference
                .child("users")
                .child(userId)
                .child("notes_media")
                .child(safeFileName)

            val uri = Uri.fromFile(file)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
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
            val ref = storageInstance.getReferenceFromUrl(remoteUrl)
            ref.getFile(destFile).await()
            destFile.exists() && destFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
