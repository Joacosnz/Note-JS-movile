package com.example.ui.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    /**
     * Muestra el prompt de autenticación biométrica (Huella/Rostro) o PIN/Patrón del sistema.
     *
     * @param context Contexto de la aplicación o Activity.
     * @param onSuccess Callback ejecutado cuando la autenticación es exitosa.
     * @param onError Callback ejecutado cuando falla la autenticación o se cancela.
     */
    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit = {}
    ) {
        val activity = context.findFragmentActivity() ?: run {
            onError()
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
                onError()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // La autenticación falló pero el prompt sigue abierto intentando
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        // Configuración del PromptInfo con BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        // Nota: No se debe usar setNegativeButtonText cuando DEVICE_CREDENTIAL está habilitado.
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso a Carpeta Privada")
            .setSubtitle("Verifica tu identidad usando tu huella, rostro o PIN del dispositivo")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
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
