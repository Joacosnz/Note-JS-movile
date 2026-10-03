package com.example.data.drive

import android.content.Context
import com.example.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.Collections

/**
 * Gestor responsable de administrar la autenticación y la conexión
 * con la API de Google Drive utilizando el ámbito exclusivo de la app (appDataFolder).
 */
class GoogleDriveManager(private val context: Context) {

    private val applicationContext: Context = context.applicationContext

    /**
     * Devuelve las opciones de Google Sign-In configuradas con el scope DRIVE_APPDATA y el ID Token para Firebase.
     */
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        val webClientId = context.getString(R.string.default_web_client_id)
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }

    /**
     * Obtiene el cliente de GoogleSignInClient para iniciar el flujo de inicio de sesión.
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(applicationContext, getGoogleSignInOptions())
    }

    /**
     * Verifica si existe una cuenta previamente autenticada con los permisos solicitados.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            account
        } else {
            null
        }
    }

    /**
     * Construye y retorna la instancia del cliente Drive de Google configurado con las credenciales de la cuenta.
     *
     * @param account Cuenta de usuario obtenida tras la autenticación exitosa.
     * @return Cliente [Drive] listo para realizar peticiones a la API REST.
     */
    fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("AuraNotes")
            .build()
    }

    /**
     * Sobrecarga para construir el cliente [Drive] directamente utilizando el nombre de la cuenta (email).
     */
    fun getDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccountName = accountName
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("AuraNotes")
            .build()
    }
}
