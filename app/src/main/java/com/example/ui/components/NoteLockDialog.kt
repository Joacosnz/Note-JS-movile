package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.util.BiometricHelper

@Composable
fun NoteLockDialog(
    isSettingPin: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onUnlockSuccess: (() -> Unit)? = null
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    val triggerBiometric = {
        if (!isSettingPin && isBiometricAvailable) {
            BiometricHelper.authenticate(
                context = context,
                onSuccess = {
                    if (onUnlockSuccess != null) {
                        onUnlockSuccess()
                    } else {
                        onConfirm("")
                    }
                },
                onError = {
                    // Si el usuario cancela, falla la autenticación o presiona "Usar PIN",
                    // se mantiene en pantalla el diálogo para ingresar el PIN manualmente.
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!isSettingPin && isBiometricAvailable) {
            triggerBiometric()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(if (isSettingPin) "Configurar código PIN" else "Desbloquear nota")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSettingPin)
                        "Ingresa un código PIN de 4 dígitos para proteger esta nota."
                    else
                        "Esta nota está protegida. Ingresa el código PIN para ver su contenido.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Código PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage != null,
                    trailingIcon = {
                        if (!isSettingPin && isBiometricAvailable) {
                            IconButton(onClick = { triggerBiometric() }) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length >= 4) {
                        onConfirm(pinText)
                    } else {
                        errorMessage = "El PIN debe tener al menos 4 dígitos"
                    }
                },
                modifier = Modifier.testTag("confirm_pin_button")
            ) {
                Text(if (isSettingPin) "Guardar PIN" else "Desbloquear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
