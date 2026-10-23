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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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

class AuthManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    private val firebaseAuth: FirebaseAuth?
        get() = try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthManager", "FirebaseAuth not available", e)
            null
        }

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    /**
     * Flujo reactivo que emite el [FirebaseUser] actual cada vez que cambia el estado de sesión.
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
     * Intenta iniciar sesión automáticamente de forma silenciosa con cuentas previamente autorizadas.
     */
    suspend fun attemptAutoSignIn(activity: Activity): AuthResult {
        val existingUser = currentUser
        if (existingUser != null) {
            return AuthResult.Success(existingUser)
        }

        val webClientId = try {
            appContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }

        if (webClientId.isBlank()) {
            return AuthResult.Error("Falta configurar default_web_client_id en google-services.json")
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
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Firebase Auth no devolvió un usuario válido")
                }
            } else {
                AuthResult.Error("Tipo de credencial no soportada: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthManager", "Auto sign-in cancelled or dismissed: ${e.message}", e)
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "No hay sesión silenciosa disponible", e)
        }
    }

    /**
     * Inicia sesión interactivamente con Google utilizando Credential Manager
     * ([GetSignInWithGoogleOption]) vinculada a Firebase Auth.
     */
    suspend fun signInWithCredentialManager(activity: Activity): AuthResult {
        val webClientId = try {
            appContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }

        if (webClientId.isBlank()) {
            return AuthResult.Error("Falta configurar default_web_client_id en google-services.json")
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
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Firebase Auth no devolvió un usuario válido")
                }
            } else {
                AuthResult.Error("Tipo de credencial no soportada: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthManager", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            AuthResult.Cancelled
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthManager", "Token de Google inválido", e)
            AuthResult.Error("Token de Google inválido", e)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Error de Credential Manager", e)
            AuthResult.Error(e.errorMessage?.toString() ?: "No se pudo obtener la credencial de Google", e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Error autenticando con Firebase", e)
            AuthResult.Error(e.localizedMessage ?: "Error autenticando con Firebase", e)
        }
    }

    suspend fun signInWithGoogle(activity: Activity): String? {
        return when (val res = signInWithCredentialManager(activity)) {
            is AuthResult.Success -> res.user.uid
            else -> null
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): String? {
        return try {
            val auth = firebaseAuth ?: return null
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            authResult.user?.uid
        } catch (e: Exception) {
            Log.e("AuthManager", "Error durante el inicio de sesión con token", e)
            null
        }
    }

    suspend fun signOutAsync() {
        try {
            firebaseAuth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("AuthManager", "Error cerrando sesión en CredentialManager", e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("AuthManager", "Error signing out", e)
        }
    }
}