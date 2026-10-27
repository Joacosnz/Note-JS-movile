package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.util.AppUpdateConfigState

/**
 * Diálogo modal de alta prioridad gestionado remotamente mediante Firebase Remote Config (NoteJsDataBase).
 * Bloquea el flujo en caso de actualización obligatoria para usuarios con versiones antiguas,
 * o permite posponer si la actualización es opcional.
 */
@Composable
fun MandatoryUpdateDialog(
    state: AppUpdateConfigState,
    isDark: Boolean,
    onUpdateNowClick: () -> Unit,
    onLaterClick: () -> Unit,
    onCloseSimulationClick: () -> Unit
) {
    val isStrictBlocking = (state.isMandatoryUpdate || state.isSimulationPreview) && !state.isOptionalUpdateAvailable
    val allowDismiss = !state.isMandatoryUpdate || state.isSimulationPreview

    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val accentRed = Color(0xFFE53935)
    val primaryAccent = if (isStrictBlocking) amber else Color(0xFF4285F4)

    val infiniteTransition = rememberInfiniteTransition(label = "update_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = {
            if (state.isSimulationPreview) {
                onCloseSimulationClick()
            } else if (!state.isMandatoryUpdate) {
                onLaterClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = allowDismiss,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 460.dp)
                .testTag("mandatory_update_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = primaryAccent.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Badge superior con origen Remote Config (NoteJsDataBase)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(primaryAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isStrictBlocking) Icons.Default.WarningAmber else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (state.isSimulationPreview) {
                            "${state.firebaseDatabaseName} • Vista Previa Obligatoria"
                        } else if (isStrictBlocking) {
                            "${state.firebaseDatabaseName} • Actualización Obligatoria"
                        } else {
                            "${state.firebaseDatabaseName} • Actualización Recomendada"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Icono central animado con atmósfera radial
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = 0.28f),
                                    primaryAccent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = primaryAccent.copy(alpha = 0.45f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Actualización de aplicación",
                        tint = primaryAccent,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Título principal
                Text(
                    text = state.updateTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("mandatory_update_title")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Comparativa de versión instalada vs versión requerida
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Tu versión",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "v${state.currentVersionName}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentRed
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Actualizar a",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Requerida",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.minRequiredVersionName.startsWith("v", ignoreCase = true) ||
                                    state.minRequiredVersionName.startsWith("Código")
                                ) {
                                    state.minRequiredVersionName
                                } else {
                                    "v${state.minRequiredVersionName}"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF34A853)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mensaje descriptivo remoto
                Text(
                    text = state.updateMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("mandatory_update_message")
                )

                if (state.releaseNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Novedades incluidas en esta versión:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        state.releaseNotes.take(4).forEach { noteItem ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = noteItem,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Botón principal de acción (Ir a Google Play Store / URL de descarga)
                Button(
                    onClick = onUpdateNowClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent,
                        contentColor = if (primaryAccent == amber) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("btn_mandatory_update_now")
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Actualizar Ahora",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }

                // Si es actualización opcional, mostrar botón para posponer
                if (!state.isMandatoryUpdate && !state.isSimulationPreview) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onLaterClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_optional_update_later")
                    ) {
                        Text(
                            text = "Más tarde",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Si se abrió desde la opción de vista previa en Ajustes, permitir cerrarla fácilmente
                if (state.isSimulationPreview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onCloseSimulationClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_close_update_simulation")
                    ) {
                        Text(
                            text = "Cerrar vista previa de prueba",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
