package com.example.auth.microsoft

import org.json.JSONObject

/**
 * Representa los detalles y credenciales de una cuenta de Microsoft (Outlook / Office 365)
 * autenticada mediante Microsoft Graph API y OAuth 2.0.
 */
data class MicrosoftAccountInfo(
    val userId: String,
    val displayName: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long = 0L,
    val avatarUrl: String? = null,
    val tenantId: String? = null,
    val accountType: String = "Office365" // "Office365", "OutlookPersonal", "WorkOrSchool"
) {
    val isExpired: Boolean
        get() = expiresAt > 0L && System.currentTimeMillis() >= (expiresAt - 60_000L) // 1 min buffer

    val summaryLabel: String
        get() = if (displayName.isNotBlank() && !displayName.equals(email, ignoreCase = true)) {
            "$displayName ($email)"
        } else {
            email.ifBlank { displayName.ifBlank { "Microsoft 365" } }
        }

    fun toJson(): String {
        return JSONObject().apply {
            put("userId", userId)
            put("displayName", displayName)
            put("email", email)
            put("accessToken", accessToken)
            put("refreshToken", refreshToken ?: "")
            put("expiresAt", expiresAt)
            put("avatarUrl", avatarUrl ?: "")
            put("tenantId", tenantId ?: "")
            put("accountType", accountType)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): MicrosoftAccountInfo? {
            if (jsonStr.isBlank()) return null
            return try {
                val obj = JSONObject(jsonStr)
                MicrosoftAccountInfo(
                    userId = obj.optString("userId"),
                    displayName = obj.optString("displayName", "Usuario Microsoft"),
                    email = obj.optString("email"),
                    accessToken = obj.optString("accessToken"),
                    refreshToken = obj.optString("refreshToken").ifBlank { null },
                    expiresAt = obj.optLong("expiresAt", 0L),
                    avatarUrl = obj.optString("avatarUrl").ifBlank { null },
                    tenantId = obj.optString("tenantId").ifBlank { null },
                    accountType = obj.optString("accountType", "Office365")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
