package com.example.auth.microsoft

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.util.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gestor de autenticación para cuentas de Microsoft (Outlook, Office 365 y cuentas personales de Microsoft).
 * Administra el flujo de OAuth 2.0 mediante Firebase OAuthProvider ("microsoft.com") y el almacenamiento
 * seguro del token de acceso para llamadas a Microsoft Graph API.
 */
class MicrosoftAuthManager(private val context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _currentAccount = MutableStateFlow<MicrosoftAccountInfo?>(loadPersistedAccount())
    val currentAccount: StateFlow<MicrosoftAccountInfo?> = _currentAccount.asStateFlow()

    private val firebaseAuth: FirebaseAuth?
        get() = try {
            FirebaseInitializer.ensureInitialized(appContext)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth no disponible", e)
            null
        }

    fun isSignedIn(): Boolean {
        val account = _currentAccount.value
        return account != null && account.accessToken.isNotBlank()
    }

    fun getValidAccessToken(): String? {
        val account = _currentAccount.value ?: return null
        return if (account.accessToken.isNotBlank()) account.accessToken else null
    }

    /**
     * Inicia sesión con cuenta de Microsoft utilizando el proveedor oficial de OAuth de Firebase.
     * Solicita permisos de Microsoft Graph API: User.Read, Calendars.ReadWrite, Files.ReadWrite, offline_access.
     */
    suspend fun signInWithMicrosoft(activity: Activity): Result<MicrosoftAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.failure(
                IllegalStateException("Firebase Auth no está inicializado.")
            )

            val provider = OAuthProvider.newBuilder("microsoft.com").apply {
                scopes = listOf(
                    "openid",
                    "email",
                    "profile",
                    "User.Read",
                    "Calendars.ReadWrite",
                    "Files.ReadWrite",
                    "offline_access"
                )
                // Opcional: permitir tanto cuentas personales (Outlook/Hotmail) como corporativas (Office 365 / Azure AD)
                addCustomParameter("prompt", "select_account")
                addCustomParameter("tenant", "common")
            }

            val authResult = withContext(Dispatchers.Main) {
                auth.startActivityForSignInWithProvider(activity, provider.build()).await()
            }

            val user = authResult.user
            val credential = authResult.credential as? OAuthCredential
            val token = credential?.accessToken ?: ""

            val displayName = user?.displayName?.ifBlank { null }
                ?: user?.email?.substringBefore("@")
                ?: "Usuario Microsoft"
            val email = user?.email ?: ""
            val photoUrl = user?.photoUrl?.toString()

            // Si obtuvimos token de acceso directo desde la credencial de OAuth, verificamos perfil en Graph
            var finalDisplayName = displayName
            var finalEmail = email
            var finalUserId = user?.uid ?: ""

            if (token.isNotBlank()) {
                val profile = fetchGraphProfile(token)
                if (profile != null) {
                    if (profile.optString("displayName").isNotBlank()) {
                        finalDisplayName = profile.optString("displayName")
                    }
                    val mail = profile.optString("mail").ifBlank { profile.optString("userPrincipalName") }
                    if (mail.isNotBlank()) {
                        finalEmail = mail
                    }
                    if (profile.optString("id").isNotBlank()) {
                        finalUserId = profile.optString("id")
                    }
                }
            }

            val accountInfo = MicrosoftAccountInfo(
                userId = finalUserId,
                displayName = finalDisplayName,
                email = finalEmail,
                accessToken = token.ifBlank { "fb_auth_${user?.uid}" },
                expiresAt = System.currentTimeMillis() + (3600 * 1000L),
                avatarUrl = photoUrl,
                accountType = if (finalEmail.endsWith("@outlook.com", ignoreCase = true) ||
                    finalEmail.endsWith("@hotmail.com", ignoreCase = true) ||
                    finalEmail.endsWith("@live.com", ignoreCase = true)
                ) "OutlookPersonal" else "Office365"
            )

            saveAccount(accountInfo)
            Result.success(accountInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando sesión con Microsoft", e)
            Result.failure(e)
        }
    }

    /**
     * Conecta o actualiza una cuenta de Microsoft mediante un token de acceso o credenciales verificadas.
     */
    suspend fun connectAccountWithToken(token: String): Result<MicrosoftAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val profile = fetchGraphProfile(token)
                ?: return@withContext Result.failure(Exception("No se pudo obtener el perfil de Microsoft Graph con este token"))

            val displayName = profile.optString("displayName", "Usuario Microsoft")
            val email = profile.optString("mail").ifBlank { profile.optString("userPrincipalName") }
            val userId = profile.optString("id", System.currentTimeMillis().toString())

            val accountInfo = MicrosoftAccountInfo(
                userId = userId,
                displayName = displayName,
                email = email,
                accessToken = token,
                expiresAt = System.currentTimeMillis() + (3600 * 1000L),
                accountType = if (email.endsWith("@outlook.com", ignoreCase = true) ||
                    email.endsWith("@hotmail.com", ignoreCase = true)
                ) "OutlookPersonal" else "Office365"
            )

            saveAccount(accountInfo)
            Result.success(accountInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error vinculando cuenta con token", e)
            Result.failure(e)
        }
    }

    private fun fetchGraphProfile(token: String): JSONObject? {
        return try {
            val req = Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                JSONObject(body)
            } else {
                Log.w(TAG, "Error en /v1.0/me: HTTP ${resp.code}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al llamar a Microsoft Graph /me", e)
            null
        }
    }

    fun signOut() {
        prefs.edit().remove(KEY_ACCOUNT_DATA).apply()
        _currentAccount.value = null
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {
        }
        Log.i(TAG, "Sesión de Microsoft cerrada")
    }

    private fun saveAccount(account: MicrosoftAccountInfo) {
        prefs.edit().putString(KEY_ACCOUNT_DATA, account.toJson()).apply()
        _currentAccount.value = account
    }

    private fun loadPersistedAccount(): MicrosoftAccountInfo? {
        val raw = prefs.getString(KEY_ACCOUNT_DATA, null) ?: return null
        return MicrosoftAccountInfo.fromJson(raw)
    }

    companion object {
        private const val TAG = "MicrosoftAuthManager"
        private const val PREFS_NAME = "microsoft_auth_prefs"
        private const val KEY_ACCOUNT_DATA = "ms_account_data"
    }
}
