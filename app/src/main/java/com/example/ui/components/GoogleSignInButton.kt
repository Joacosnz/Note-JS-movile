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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthManager
import com.example.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Estados visuales del botón de inicio de sesión con Google.
 */
sealed interface GoogleSignInState {
    data object Idle : GoogleSignInState
    data object Loading : GoogleSignInState
    data class Success(val user: FirebaseUser) : GoogleSignInState
    data class Error(val message: String) : GoogleSignInState
}

/**
 * Logotipo oficial de Google de 4 colores dibujado vectorialmente con Compose Canvas.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.44f
        val strokeW = w * 0.18f

        // Colores oficiales de la marca Google
        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        // Barra central horizontal
        val barPath = Path().apply {
            moveTo(cx, cy - strokeW / 2)
            lineTo(cx + r, cy - strokeW / 2)
            lineTo(cx + r, cy + strokeW / 2)
            lineTo(cx, cy + strokeW / 2)
            close()
        }
        drawPath(barPath, blue, style = Fill)

        val arcRect = Rect(cx - r, cy - r, cx + r, cy + r)

        // Segmento azul
        drawArc(
            color = blue,
            startAngle = -45f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW)
        )

        // Segmento verde
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW)
        )

        // Segmento amarillo
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW)
        )

        // Segmento rojo
        drawArc(
            color = red,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW)
        )
    }
}

/**
 * Componente reutilizable de botón de Google Sign-In en Jetpack Compose.
 *
 * Invoca el método [AuthManager.signIn], proporcionando retroalimentación visual completa
 * para cada uno de los estados:
 * - **Reposo (Idle)**: Isotipo vectorial de Google y texto configurable.
 * - **Carga (Loading)**: Indicador de progreso circular con etiqueta de conexión y prevención de doble toque.
 * - **Éxito (Success)**: Icono animado de confirmación verde y mensaje de bienvenida antes del callback.
 * - **Error (Error)**: Borde coloreado, icono de alerta y mensaje explicativo inline con opción de reintento.
 */
@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    authManager: AuthManager? = null,
    text: String = "Continuar con Google",
    enabled: Boolean = true,
    showInlineError: Boolean = true,
    isLoading: Boolean? = null,
    onSuccess: (FirebaseUser) -> Unit = {},
    onError: (String) -> Unit = {},
    onCancelled: () -> Unit = {}
) {
    val context = LocalContext.current
    val effectiveAuthManager = authManager ?: remember(context) { AuthManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var visualState by remember { mutableStateOf<GoogleSignInState>(GoogleSignInState.Idle) }

    // Sincronizar estado de carga externo si es proporcionado
    LaunchedEffect(isLoading) {
        if (isLoading == true && visualState !is GoogleSignInState.Loading) {
            visualState = GoogleSignInState.Loading
        } else if (isLoading == false && visualState is GoogleSignInState.Loading) {
            visualState = GoogleSignInState.Idle
        }
    }

    val isInteractive = enabled && visualState !is GoogleSignInState.Loading

    // Animación de color de borde según el estado
    val animatedBorderColor by animateColorAsState(
        targetValue = when (visualState) {
            is GoogleSignInState.Error -> MaterialTheme.colorScheme.error
            is GoogleSignInState.Success -> Color(0xFF34A853)
            is GoogleSignInState.Loading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            GoogleSignInState.Idle -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "google_sign_in_border_color"
    )

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = {
                if (!isInteractive) return@OutlinedButton

                val activity = context.findActivity()
                if (activity == null) {
                    val errorMsg = "No se pudo obtener la actividad actual para iniciar sesión"
                    visualState = GoogleSignInState.Error(errorMsg)
                    onError(errorMsg)
                    return@OutlinedButton
                }

                visualState = GoogleSignInState.Loading

                coroutineScope.launch {
                    when (val result = effectiveAuthManager.signIn(activity)) {
                        is AuthResult.Success -> {
                            visualState = GoogleSignInState.Success(result.user)
                            delay(600) // Breve confirmación visual antes de transferir foco
                            onSuccess(result.user)
                        }
                        is AuthResult.Cancelled -> {
                            visualState = GoogleSignInState.Idle
                            onCancelled()
                        }
                        is AuthResult.Error -> {
                            val msg = result.message.ifBlank { "Error durante el inicio de sesión con Google" }
                            visualState = GoogleSignInState.Error(msg)
                            onError(msg)
                        }
                    }
                }
            },
            enabled = isInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(width = 1.dp, color = animatedBorderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = when (visualState) {
                    is GoogleSignInState.Success -> Color(0xFF34A853).copy(alpha = 0.08f)
                    is GoogleSignInState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
                    else -> MaterialTheme.colorScheme.surface
                },
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            AnimatedContent(
                targetState = visualState,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                },
                label = "google_sign_in_content_state"
            ) { state ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    when (state) {
                        is GoogleSignInState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("google_sign_in_loading_indicator"),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Conectando con Google...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is GoogleSignInState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Sesión iniciada con éxito",
                                tint = Color(0xFF34A853),
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("google_sign_in_success_icon")
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val name = state.user.displayName ?: state.user.email ?: "Sesión iniciada"
                            Text(
                                text = "¡Hola, $name!",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color(0xFF1E8E3E)
                            )
                        }
                        is GoogleSignInState.Error -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error de inicio de sesión",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("google_sign_in_error_icon")
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Reintentar inicio de sesión",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        GoogleSignInState.Idle -> {
                            GoogleLogoIcon(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Retroalimentación explicativa de error
        if (showInlineError && visualState is GoogleSignInState.Error) {
            val errorMsg = (visualState as GoogleSignInState.Error).message
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 6.dp)
                        .testTag("google_sign_in_error_text")
                )
            }
        }
    }
}
