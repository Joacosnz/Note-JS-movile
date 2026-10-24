package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.utils.BiometricAuthManager

@Composable
fun NoteLockDialog(
    isSettingPin: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onUnlockSuccess: (() -> Unit)? = null,
    expectedPin: String? = null,
    title: String? = null,
    subtitle: String? = null,
    autoTriggerBiometric: Boolean = true,
    isBiometricEnabled: Boolean = true
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isBiometricAvailable = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }

    val triggerBiometric = {
        if (!isSettingPin && isBiometricEnabled) {
            BiometricAuthManager.authenticateWithBiometrics(
                context = context,
                title = title ?: "Desbloquear con Biometría",
                subtitle = subtitle ?: "Verifica tu identidad con tu huella dactilar o reconocimiento facial",
                onSuccess = {
                    if (onUnlockSuccess != null) {
                        onUnlockSuccess()
                    } else {
                        onConfirm(expectedPin ?: "")
                    }
                },
                onFallbackToPin = {
                    // Permanece en el diálogo para ingresar el PIN manualmente
                },
                onError = { msg ->
                    if (!isBiometricAvailable && msg.isNotBlank()) {
                        errorMessage = "Biometría no disponible en este dispositivo. Usa tu PIN."
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!isSettingPin && autoTriggerBiometric && isBiometricEnabled && isBiometricAvailable) {
            triggerBiometric()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = if (!isSettingPin && isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title ?: if (isSettingPin) "Configurar código PIN" else "Protección Biométrica / PIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = subtitle ?: if (isSettingPin)
                        "Ingresa un código PIN de al menos 4 dígitos para proteger tus notas privadas."
                    else
                        "Desbloquea con tu huella dactilar, reconocimiento facial o ingresa tu PIN de seguridad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSettingPin && isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_biometric_prompt_unlock")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Desbloquear con huella o rostro",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Usar Huella o Reconocimiento Facial",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Código PIN de respaldo") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage != null,
                    trailingIcon = {
                        if (!isSettingPin && isBiometricEnabled) {
                            IconButton(
                                onClick = { triggerBiometric() },
                                modifier = Modifier.testTag("icon_btn_biometric_retry")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Autenticación biométrica",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length < 4) {
                        errorMessage = "El PIN debe tener al menos 4 dígitos"
                    } else if (!isSettingPin && !expectedPin.isNullOrBlank() && pinText != expectedPin) {
                        errorMessage = "PIN incorrecto. Usa tu huella/rostro o el PIN configurado."
                    } else {
                        if (!isSettingPin && onUnlockSuccess != null) {
                            onUnlockSuccess()
                        } else {
                            onConfirm(pinText)
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_pin_button")
            ) {
                Text(if (isSettingPin) "Guardar PIN" else "Desbloquear")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_pin_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
