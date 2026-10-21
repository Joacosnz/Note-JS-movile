package com.example.ui.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gestor unificado definitivo para autenticación biométrica y credenciales del sistema (PIN/Patrón/Rostro/Huella).
 */
object BiometricAuthManager {

    /**
     * Verifica si la autenticación biométrica o las credenciales del dispositivo están disponibles y configuradas.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Autentica al usuario mediante biometría o credencial del sistema.
     *
     * @param context Contexto de la aplicación o Activity.
     * @param title Título del diálogo nativo del sistema.
     * @param subtitle Subtítulo descriptivo.
     * @param onSuccess Callback ejecutado cuando la autenticación es exitosa.
     * @param onError Callback ejecutado con la descripción del error o cancelación.
     */
    fun authenticateWithBiometrics(
        context: Context,
        title: String = "Acceso a Carpeta Privada",
        subtitle: String = "Verifica tu identidad usando tu huella, rostro o PIN del dispositivo",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findFragmentActivity() ?: run {
            onError("No se encontró FragmentActivity para mostrar BiometricPrompt.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        // Nota: Según la especificación oficial de BiometricPrompt, cuando se incluye DEVICE_CREDENTIAL
        // en setAllowedAuthenticators, NO debe invocarse setNegativeButtonText().
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
