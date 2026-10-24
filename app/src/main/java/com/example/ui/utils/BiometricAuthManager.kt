package com.example.ui.utils

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    UNSUPPORTED
}

/**
 * Capa de autenticación biométrica basada en Android BiometricPrompt API
 * para proteger notas y el acceso a la aplicación mediante huella dactilar,
 * reconocimiento facial o credenciales de respaldo.
 */
object BiometricAuthManager {

    /**
     * Evalúa el estado de los sensores biométricos (Huella dactilar / Reconocimiento facial).
     */
    fun getBiometricStatus(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
                else -> BiometricStatus.UNSUPPORTED
            }
        } catch (_: Exception) {
            BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Devuelve una descripción legible para mostrar en la interfaz de Ajustes.
     */
    fun getBiometricStatusDescription(context: Context): String {
        return when (getBiometricStatus(context)) {
            BiometricStatus.AVAILABLE -> "Huella dactilar y desbloqueo facial activos (BiometricPrompt)"
            BiometricStatus.NONE_ENROLLED -> "Sensor detectado • Sin huella/rostro registrado (usa PIN de respaldo)"
            BiometricStatus.NO_HARDWARE -> "Sin sensor biométrico en el dispositivo • Respaldo por PIN activo"
            BiometricStatus.HARDWARE_UNAVAILABLE -> "Sensor biométrico ocupado temporalmente • Respaldo por PIN activo"
            BiometricStatus.UNSUPPORTED -> "Protección mediante PIN de seguridad activa"
        }
    }

    /**
     * Verifica si el hardware biométrico (huella o rostro) tiene datos registrados y listos para usar.
     */
    fun isBiometricHardwareReady(context: Context): Boolean {
        return getBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    /**
     * Verifica si la autenticación biométrica o las credenciales del dispositivo están disponibles y configuradas.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
                return true
            }
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            }
            biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Autentica al usuario mediante el diálogo nativo BiometricPrompt (huella dactilar o reconocimiento facial),
     * con soporte para botón de respaldo de PIN de la app o credencial del sistema.
     *
     * @param context Contexto de la aplicación o FragmentActivity.
     * @param title Título del diálogo BiometricPrompt.
     * @param subtitle Subtítulo descriptivo.
     * @param description Descripción opcional adicional.
     * @param negativeButtonText Texto del botón para usar el PIN de respaldo cuando se usa biometría pura.
     * @param onSuccess Callback ejecutado cuando la autenticación biométrica es exitosa.
     * @param onFallbackToPin Callback opcional ejecutado cuando el usuario elige "Usar PIN" o no hay biometría configurada.
     * @param onError Callback ejecutado con la descripción del error o cancelación.
     */
    fun authenticateWithBiometrics(
        context: Context,
        title: String = "Desbloquear con Biometría",
        subtitle: String = "Usa tu huella dactilar o reconocimiento facial para continuar",
        description: String? = "Tus notas privadas están protegidas con Android BiometricPrompt",
        negativeButtonText: String = "Usar PIN",
        onSuccess: () -> Unit,
        onFallbackToPin: (() -> Unit)? = null,
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findFragmentActivity() ?: run {
            if (onFallbackToPin != null) {
                onFallbackToPin()
            } else {
                onError("No se encontró FragmentActivity para mostrar BiometricPrompt.")
            }
            return
        }

        val hasBiometricSensorReady = isBiometricHardwareReady(activity)
        val hasAnyAuthenticator = isBiometricAvailable(activity)

        if (!hasBiometricSensorReady && !hasAnyAuthenticator) {
            if (onFallbackToPin != null) {
                onFallbackToPin()
            } else {
                onError("No hay datos biométricos ni bloqueo de pantalla configurados en este dispositivo.")
            }
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
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                        if (onFallbackToPin != null) {
                            onFallbackToPin()
                        } else {
                            onError(errString.toString())
                        }
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onError(errString.toString())
                    }
                    else -> {
                        if (onFallbackToPin != null) {
                            onFallbackToPin()
                        } else {
                            onError(errString.toString())
                        }
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setConfirmationRequired(false)

            if (!description.isNullOrBlank()) {
                promptBuilder.setDescription(description)
            }

            if (hasBiometricSensorReady && onFallbackToPin != null) {
                // Habilita tanto Huella Dactilar (BIOMETRIC_STRONG) como Desbloqueo Facial (BIOMETRIC_WEAK / STRONG)
                // e incluye botón directo para usar el PIN de respaldo de Note JS
                promptBuilder
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                    .setNegativeButtonText(negativeButtonText)
            } else {
                val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                } else {
                    BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                }
                promptBuilder.setAllowedAuthenticators(authenticators)
            }

            biometricPrompt.authenticate(promptBuilder.build())
        } catch (e: Exception) {
            if (onFallbackToPin != null) {
                onFallbackToPin()
            } else {
                onError(e.localizedMessage ?: "Error al iniciar la autenticación biométrica")
            }
        }
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
