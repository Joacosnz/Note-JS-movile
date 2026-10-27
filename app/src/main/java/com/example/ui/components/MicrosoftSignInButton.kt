package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.microsoft.MicrosoftAccountInfo
import com.example.auth.microsoft.MicrosoftAuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Logotipo oficial de 4 cuadrados de Microsoft dibujado vectorialmente con Compose Canvas.
 */
@Composable
fun MicrosoftLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val totalSize = size.minDimension
        val squareSize = totalSize * 0.44f
        val gap = totalSize * 0.12f

        // Cuadrado superior izquierdo: Rojo (#F25022)
        drawRect(
            color = Color(0xFFF25022),
            topLeft = Offset(0f, 0f),
            size = Size(squareSize, squareSize)
        )
        // Cuadrado superior derecho: Verde (#7FBA00)
        drawRect(
            color = Color(0xFF7FBA00),
            topLeft = Offset(squareSize + gap, 0f),
            size = Size(squareSize, squareSize)
        )
        // Cuadrado inferior izquierdo: Azul (#00A4EF)
        drawRect(
            color = Color(0xFF00A4EF),
            topLeft = Offset(0f, squareSize + gap),
            size = Size(squareSize, squareSize)
        )
        // Cuadrado inferior derecho: Amarillo (#FFB900)
        drawRect(
            color = Color(0xFFFFB900),
            topLeft = Offset(squareSize + gap, squareSize + gap),
            size = Size(squareSize, squareSize)
        )
    }
}

/**
 * Botón oficial y accesible de inicio de sesión con Microsoft (Outlook / Office 365).
 */
@Composable
fun MicrosoftSignInButton(
    authManager: MicrosoftAuthManager,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String = "Continuar con Microsoft (Outlook / 365)",
    onSuccess: (MicrosoftAccountInfo) -> Unit = {},
    onError: (String) -> Unit = {},
    onCancelled: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var localLoading by remember { mutableStateOf(false) }
    var successUser by remember { mutableStateOf<MicrosoftAccountInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val effectiveLoading = isLoading || localLoading

    val borderColor by animateColorAsState(
        targetValue = when {
            successUser != null -> Color(0xFF7FBA00)
            errorMessage != null -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "ms_border_color"
    )

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = {
                if (effectiveLoading || successUser != null) return@OutlinedButton
                errorMessage = null
                localLoading = true
                coroutineScope.launch {
                    val activity = context as? Activity
                    if (activity == null) {
                        localLoading = false
                        val err = "No se pudo obtener la ventana de la aplicación."
                        errorMessage = err
                        onError(err)
                        return@launch
                    }

                    val result = authManager.signInWithMicrosoft(activity)
                    localLoading = false
                    result.fold(
                        onSuccess = { acc ->
                            successUser = acc
                            onSuccess(acc)
                            delay(1200)
                            successUser = null
                        },
                        onFailure = { err ->
                            val msg = err.localizedMessage ?: "Error al autenticar con Microsoft"
                            if (msg.contains("cancelled", ignoreCase = true) || msg.contains("cancelado", ignoreCase = true)) {
                                onCancelled()
                            } else {
                                errorMessage = msg
                                onError(msg)
                            }
                        }
                    )
                }
            },
            enabled = !effectiveLoading,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_sign_in_microsoft")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = when {
                        effectiveLoading -> "loading"
                        successUser != null -> "success"
                        errorMessage != null -> "error"
                        else -> "idle"
                    },
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) + slideInVertically { it / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(140)) + slideOutVertically { -it / 2 })
                    },
                    label = "ms_btn_content"
                ) { state ->
                    when (state) {
                        "loading" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .testTag("ms_sign_in_progress"),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00A4EF)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Conectando con Microsoft...",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        "success" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Éxito",
                                    tint = Color(0xFF7FBA00),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Conectado",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF7FBA00)
                                )
                            }
                        }
                        "error" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reintentar Microsoft",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MicrosoftLogoIcon(modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }
}
