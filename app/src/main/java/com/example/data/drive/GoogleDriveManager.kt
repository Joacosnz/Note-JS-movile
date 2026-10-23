package com.example.data.drive

import android.accounts.Account
import android.content.Context
import com.example.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

/**
 * Gestor responsable de administrar la autenticación con Google y la conexión
 * con Note JS Cloud.
 */
class GoogleDriveManager(private val context: Context) {

    private val applicationContext: Context = context.applicationContext

    /**
     * Devuelve las opciones de Google Sign-In configuradas con el ID Token para Firebase Auth.
     * No exige scopes restringidos de Drive en el inicio de sesión principal para evitar bloqueos OAuth.
     */
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        val webClientId = try {
            applicationContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (webClientId.isNotBlank()) {
            try {
                builder.requestIdToken(webClientId)
            } catch (_: Exception) {
            }
        }
        return builder.build()
    }

    /**
     * Obtiene el cliente de GoogleSignInClient para iniciar el flujo de inicio de sesión.
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(applicationContext, getGoogleSignInOptions())
    }

    /**
     * Verifica si existe una cuenta previamente autenticada en el dispositivo.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getLastSignedInAccount(applicationContext)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Construye y retorna la instancia del cliente Drive de Note JS configurado con las credenciales de la cuenta.
     */
    fun getDriveService(account: GoogleSignInAccount): Drive {
        val accountObj = account.account ?: account.email?.let { Account(it, "com.google") }
        val scopes = listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            scopes
        ).apply {
            if (accountObj != null) {
                selectedAccount = accountObj
            } else if (!account.email.isNullOrEmpty()) {
                selectedAccountName = account.email
            }
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Note JS")
            .build()
    }

    /**
     * Sobrecarga para construir el cliente [Drive] directamente utilizando el nombre de la cuenta (email).
     */
    fun getDriveService(accountName: String): Drive {
        val scopes = listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            scopes
        ).apply {
            selectedAccountName = accountName
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Note JS")
            .build()
    }
}
