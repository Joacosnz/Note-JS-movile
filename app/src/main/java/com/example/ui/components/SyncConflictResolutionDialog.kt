package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConflictResolutionChoice
import com.example.model.SyncConflictItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo sencillo y claro que aparece durante la sincronización cuando una misma nota,
 * recordatorio o evento fue actualizado tanto en Google como en Microsoft, permitiendo
 * que el usuario elija qué versión conservar o guardar ambas.
 */
@Composable
fun SyncConflictResolutionDialog(
    conflicts: List<SyncConflictItem>,
    accentColor: Color,
    onResolveConflict: (SyncConflictItem, ConflictResolutionChoice, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (conflicts.isEmpty()) return

    val currentConflict = conflicts.first()
    val totalCount = conflicts.size
    var applyToAll by remember(totalCount) { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "ES"))
    }

    val googleTimeText = remember(currentConflict.googleUpdatedAt) {
        if (currentConflict.googleUpdatedAt > 0L) {
            dateFormatter.format(Date(currentConflict.googleUpdatedAt))
        } else {
            "Reciente"
        }
    }

    val microsoftTimeText = remember(currentConflict.microsoftUpdatedAt) {
        if (currentConflict.microsoftUpdatedAt > 0L) {
            dateFormatter.format(Date(currentConflict.microsoftUpdatedAt))
        } else {
            "Reciente"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Conflicto de sincronización",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conflicto de sincronización",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalCount > 1) {
                                "${currentConflict.recordType.displayLabel} • Pendientes: $totalCount"
                            } else {
                                currentConflict.recordType.displayLabel
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Este elemento fue modificado tanto en Google como en Microsoft. Elige qué versión deseas conservar:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tarjeta de la versión de Google
                PlatformVersionComparisonCard(
                    platformName = "Google (Drive / Calendar)",
                    badgeColor = Color(0xFF4285F4),
                    isNewest = currentConflict.googleUpdatedAt >= currentConflict.microsoftUpdatedAt,
                    title = currentConflict.googleTitle.ifBlank { "(Sin título)" },
                    content = currentConflict.googleContent.ifBlank { "Sin contenido adicional" },
                    timestampText = googleTimeText,
                    logoComposable = { GoogleLogoIcon(modifier = Modifier.size(18.dp)) },
                    onSelectClick = {
                        onResolveConflict(
                            currentConflict,
                            ConflictResolutionChoice.KEEP_GOOGLE,
                            applyToAll
                        )
                    },
                    testTag = "card_conflict_version_google"
                )

                // Tarjeta de la versión de Microsoft
                PlatformVersionComparisonCard(
                    platformName = "Microsoft (OneDrive / Outlook)",
                    badgeColor = Color(0xFF0078D4),
                    isNewest = currentConflict.microsoftUpdatedAt > currentConflict.googleUpdatedAt,
                    title = currentConflict.microsoftTitle.ifBlank { "(Sin título)" },
                    content = currentConflict.microsoftContent.ifBlank { "Sin contenido adicional" },
                    timestampText = microsoftTimeText,
                    logoComposable = { MicrosoftLogoIcon(modifier = Modifier.size(18.dp)) },
                    onSelectClick = {
                        onResolveConflict(
                            currentConflict,
                            ConflictResolutionChoice.KEEP_MICROSOFT,
                            applyToAll
                        )
                    },
                    testTag = "card_conflict_version_microsoft"
                )

                // Opción de aplicar a todos si hay más de 1 conflicto
                if (totalCount > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { applyToAll = !applyToAll }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = applyToAll,
                            onCheckedChange = { applyToAll = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor),
                            modifier = Modifier.testTag("checkbox_conflict_apply_all")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aplicar esta decisión a los $totalCount conflictos pendientes",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Botones de acción rápida
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onResolveConflict(
                                    currentConflict,
                                    ConflictResolutionChoice.KEEP_GOOGLE,
                                    applyToAll
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_resolve_keep_google")
                        ) {
                            Text(
                                text = "Usar Google",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                onResolveConflict(
                                    currentConflict,
                                    ConflictResolutionChoice.KEEP_MICROSOFT,
                                    applyToAll
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0078D4),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_resolve_keep_microsoft")
                        ) {
                            Text(
                                text = "Usar Microsoft",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onResolveConflict(
                                currentConflict,
                                ConflictResolutionChoice.KEEP_BOTH,
                                applyToAll
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_resolve_keep_both")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conservar ambas versiones (crear copia)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_dismiss_conflict_dialog")
            ) {
                Text(
                    text = "Posponer",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun PlatformVersionComparisonCard(
    platformName: String,
    badgeColor: Color,
    isNewest: Boolean,
    title: String,
    content: String,
    timestampText: String,
    logoComposable: @Composable () -> Unit,
    onSelectClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isNewest) 1.5.dp else 1.dp,
            color = if (isNewest) badgeColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelectClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    logoComposable()
                    Text(
                        text = platformName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isNewest) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Más reciente",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeColor
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Modificado: $timestampText",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
