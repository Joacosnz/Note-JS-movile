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

object BiometricAuth {

    /**
     * Comprueba si la autenticación biométrica (Huella/Rostro) o las credenciales del dispositivo (PIN/Patrón)
     * están disponibles y configuradas en el dispositivo.
     */
    fun isBiometricOrCredentialAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Muestra el prompt de autenticación biométrica nativo de Android.
     * Permite autenticación con huella dactilar, rostro o credenciales del dispositivo (PIN/Patrón/Contraseña del sistema).
     *
     * Callbacks:
     * - [onSuccess]: Se ejecuta si el usuario se autentica con éxito.
     * - [onError]: Se ejecuta en caso de error grave de hardware o configuración.
     * - [onCancel]: Se ejecuta si el usuario cancela explícitamente el diálogo.
     */
    fun authenticate(
        context: Context,
        title: String = "Acceso a Carpeta Privada",
        subtitle: String = "Verifica tu identidad mediante huella, rostro o PIN del sistema",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onCancel: () -> Unit = {}
    ) {
        val activity = context.findFragmentActivity() ?: run {
            onError(-1, "No se encontró FragmentActivity para mostrar BiometricPrompt.")
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
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel()
                } else {
                    onError(errorCode, errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Intento fallido pero el prompt sigue activo
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        // IMPORTANTE: Según la especificación de Android BiometricPrompt, cuando se incluye DEVICE_CREDENTIAL
        // en setAllowedAuthenticators, NO se debe llamar a setNegativeButtonText().
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
