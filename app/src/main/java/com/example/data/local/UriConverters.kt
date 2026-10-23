package com.example.data.local

import android.net.Uri
import androidx.room.TypeConverter
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.NoteMediaAttachment
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UriConverters {
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }

    @TypeConverter
    fun fromAttachmentsList(value: List<NoteMediaAttachment>): String {
        val arr = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type.name)
            obj.put("displayName", item.displayName)
            obj.put("mimeType", item.mimeType)
            if (item.localUri != null) obj.put("localUri", item.localUri)
            if (item.remoteUrl != null) obj.put("remoteUrl", item.remoteUrl)
            if (item.storagePath != null) obj.put("storagePath", item.storagePath)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("pageCount", item.pageCount)
            arr.put(obj)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toAttachmentsList(value: String): List<NoteMediaAttachment> {
        if (value.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(value)
            val list = mutableListOf<NoteMediaAttachment>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val typeName = obj.optString("type", "IMAGE")
                val type = runCatching { MediaType.valueOf(typeName) }.getOrDefault(MediaType.IMAGE)
                list.add(
                    NoteMediaAttachment(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = type,
                        displayName = obj.optString("displayName", ""),
                        mimeType = obj.optString("mimeType", "application/octet-stream"),
                        localUri = obj.optString("localUri", "").ifBlank { null },
                        remoteUrl = obj.optString("remoteUrl", "").ifBlank { null },
                        storagePath = obj.optString("storagePath", "").ifBlank { null },
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        pageCount = obj.optInt("pageCount", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}


