package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.example.util.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult
    data object Cancelled : AuthResult
    data class Error(val message: String, val throwable: Throwable? = null) : AuthResult
}

/**
 * Gestor de autenticación que coordina FirebaseAuth y Jetpack Credential Manager.
 * Utiliza el recurso `R.string.default_web_client_id` generado por el plugin de Google Services
 * para garantizar la coincidencia exacta de la audiencia (OAuth 2.0 Web Client ID) y evitar
 * errores de FirebaseAuthInvalidCredentialsException por mismatch de proyecto.
 */
class AuthManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    private val firebaseAuth: FirebaseAuth?
        get() = try {
            FirebaseInitializer.ensureInitialized(appContext)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth no disponible en este entorno", e)
            null
        }

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    /**
     * Obtiene de forma segura el Web Client ID configurado desde los recursos de la aplicación.
     * Este valor proviene directamente de `google-services.json` (oauth_client tipo 3).
     */
    fun getWebClientId(): String {
        val fromRes = try {
            val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            if (resId != 0) {
                appContext.getString(resId)
            } else {
                appContext.getString(R.string.default_web_client_id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo resolver default_web_client_id desde recursos: ${e.message}")
            ""
        }
        if (fromRes.isNotBlank()) {
            return fromRes
        }
        return "318447528237-ktfp85ramrfbq9fldrk5hbmhca2e9j4b.apps.googleusercontent.com"
    }

    /**
     * Flujo reactivo que emite el [FirebaseUser] actual cada vez que cambia el estado de sesión en Firebase Auth.
     */
    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Intenta iniciar sesión automáticamente de forma silenciosa con cuentas previamente autorizadas en el dispositivo.
     */
    suspend fun attemptAutoSignIn(activity: Activity): AuthResult {
        val existingUser = currentUser
        if (existingUser != null) {
            return AuthResult.Success(existingUser)
        }

        val webClientId = getWebClientId()
        if (webClientId.isBlank()) {
            val msg = "default_web_client_id no está disponible en los recursos. Verifica google-services.json."
            Log.e(TAG, msg)
            return AuthResult.Error(msg)
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val auth = firebaseAuth ?: return AuthResult.Error("Firebase Auth no está inicializado")
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    Log.i(TAG, "Inicio de sesión silencioso exitoso: ${user.uid}")
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Firebase Auth no devolvió un usuario válido")
                }
            } else {
                AuthResult.Error("Tipo de credencial no soportada: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Auto sign-in cancelado o no hay cuenta previamente autorizada")
            AuthResult.Cancelled
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w(TAG, "Credencial silenciosa inválida o caducada para Firebase: ${e.message}")
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            AuthResult.Error("Credencial de Google no válida para el proyecto actual", e)
        } catch (e: Exception) {
            Log.d(TAG, "No hay sesión silenciosa previa: ${e.message}")
            AuthResult.Error(e.localizedMessage ?: "No hay sesión silenciosa disponible", e)
        }
    }

    /**
     * Inicia sesión interactivamente con Google utilizando Credential Manager ([GetSignInWithGoogleOption])
     * y autentica el token resultante en FirebaseAuth con la audiencia correcta.
     */
    suspend fun signInWithCredentialManager(activity: Activity): AuthResult {
        val webClientId = getWebClientId()
        if (webClientId.isBlank()) {
            val msg = "default_web_client_id no está disponible en los recursos. Verifica google-services.json."
            Log.e(TAG, msg)
            return AuthResult.Error(msg)
        }

        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val auth = firebaseAuth ?: return AuthResult.Error("Firebase Auth no está inicializado")
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    Log.i(TAG, "Inicio de sesión interactivo exitoso: ${user.uid}")
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Firebase Auth no devolvió un usuario válido")
                }
            } else {
                AuthResult.Error("Tipo de credencial no soportada: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelado por el usuario: ${e.message}")
            AuthResult.Cancelled
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Token de Google malformado o no parseable", e)
            AuthResult.Error("Token de Google inválido", e)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Error de validación de credencial o audiencia en Firebase: ${e.message}", e)
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            AuthResult.Error("La credencial de Google no es válida para este proyecto Firebase. Vuelve a intentarlo.", e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error de Credential Manager", e)
            AuthResult.Error(e.errorMessage?.toString() ?: "No se pudo obtener la credencial de Google", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error autenticando con Firebase", e)
            AuthResult.Error(e.localizedMessage ?: "Error autenticando con Firebase", e)
        }
    }

    /**
     * Inicia sesión utilizando Credential Manager y Firebase Auth.
     */
    suspend fun signIn(activity: Activity): AuthResult = signInWithCredentialManager(activity)

    /**
     * Atajo para iniciar sesión y retornar el UID del usuario autenticado si es exitoso.
     */
    suspend fun signInWithGoogle(activity: Activity): String? {
        return when (val res = signIn(activity)) {
            is AuthResult.Success -> res.user.uid
            else -> null
        }
    }

    /**
     * Inicia sesión con un idToken de Google explícito si es requerido, manejando posibles desajustes
     * de credenciales de manera segura.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): String? {
        return try {
            val auth = firebaseAuth ?: return null
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            authResult.user?.uid
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w(TAG, "Audiencia o token de Google no autorizado para este proyecto Firebase: ${e.message}")
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error durante el inicio de sesión con token: ${e.message}")
            null
        }
    }

    /**
     * Cierra la sesión en FirebaseAuth y purga el estado de credenciales en CredentialManager.
     */
    suspend fun signOutAsync() {
        try {
            firebaseAuth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.i(TAG, "Sesión cerrada correctamente en Firebase y CredentialManager")
        } catch (e: Exception) {
            Log.w(TAG, "Error cerrando sesión en CredentialManager", e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out", e)
        }
    }

    companion object {
        private const val TAG = "AuthManager"
    }
}