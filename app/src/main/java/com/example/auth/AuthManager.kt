package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signInWithGoogle(activity: Activity): String? {
        try {
            // El ID del cliente web se genera automáticamente si google-services.json está configurado.
            // Si esto falla, asegúrate de tener el cliente web de OAuth en Google Cloud.
            val webClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
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
            
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    return signInWithGoogleIdToken(googleIdTokenCredential.idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("AuthManager", "Respuesta de token inválida", e)
                }
            } else {
                Log.e("AuthManager", "Tipo de credencial inesperada")
            }
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Excepción obteniendo credencial", e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Error durante el inicio de sesión", e)
        }
        return null
    }
    
    suspend fun signInWithGoogleIdToken(idToken: String): String? {
        return try {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(authCredential).await()
            authResult.user?.uid
        } catch (e: Exception) {
            Log.e("AuthManager", "Error durante el inicio de sesión con token", e)
            null
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}