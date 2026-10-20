package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.SortOption

private data class SortOptionDisplayInfo(
    val mainTitle: String,
    val badgeText: String,
    val icon: ImageVector,
    val isAscending: Boolean,
)

private data class SortGroup(
    val title: String,
    val icon: ImageVector,
    val options: List<SortOption>
)

private fun getSortGroups(): List<SortGroup> {
    return listOf(
        SortGroup(
            title = "POR MODIFICACIÓN",
            icon = Icons.Default.Update,
            options = listOf(SortOption.MODIFIED_DESC, SortOption.MODIFIED_ASC)
        ),
        SortGroup(
            title = "POR CREACIÓN",
            icon = Icons.Default.CalendarToday,
            options = listOf(SortOption.CREATED_DESC, SortOption.CREATED_ASC)
        ),
        SortGroup(
            title = "POR NOMBRE Y TÍTULO",
            icon = Icons.Default.SortByAlpha,
            options = listOf(SortOption.TITLE_ASC, SortOption.TITLE_DESC)
        )
    )
}

private fun SortOption.getDisplayInfo(): SortOptionDisplayInfo {
    return when (this) {
        SortOption.MODIFIED_DESC -> SortOptionDisplayInfo(
            mainTitle = "Última modificación",
            badgeText = "Recientes primero",
            icon = Icons.Default.Update,
            isAscending = false
        )
        SortOption.MODIFIED_ASC -> SortOptionDisplayInfo(
            mainTitle = "Última modificación",
            badgeText = "Antiguas primero",
            icon = Icons.Default.History,
            isAscending = true
        )
        SortOption.CREATED_DESC -> SortOptionDisplayInfo(
            mainTitle = "Fecha de creación",
            badgeText = "Más nuevas primero",
            icon = Icons.Default.CalendarToday,
            isAscending = false
        )
        SortOption.CREATED_ASC -> SortOptionDisplayInfo(
            mainTitle = "Fecha de creación",
            badgeText = "Más antiguas primero",
            icon = Icons.Default.DateRange,
            isAscending = true
        )
        SortOption.TITLE_ASC -> SortOptionDisplayInfo(
            mainTitle = "Título",
            badgeText = "A  →  Z",
            icon = Icons.Default.SortByAlpha,
            isAscending = true
        )
        SortOption.TITLE_DESC -> SortOptionDisplayInfo(
            mainTitle = "Título",
            badgeText = "Z  →  A",
            icon = Icons.Default.SortByAlpha,
            isAscending = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState: SheetState,
    currentOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = isAppInDarkTheme()
    val haptic = LocalHapticFeedback.current

    // Glassmorphism inspired Note JS dark / light palette
    val sheetBackgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardBackgroundColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    val accentColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetBackgroundColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)
            )
        },
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Ordenar notas por",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Elige la organización que prefieras para tu lista",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Categorized Groups
            getSortGroups().forEach { group ->
                // Group Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = group.icon,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        ),
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.45f)
                    )
                }

                // Group Surface Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBackgroundColor,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        group.options.forEachIndexed { index, option ->
                            val isSelected = currentOption == option
                            val displayInfo = option.getDisplayInfo()

                            val itemBgColor by animateColorAsState(
                                targetValue = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                                label = "itemBgColor"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBgColor)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOptionSelected(option)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("sort_option_${option.name}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left Section: Icon & Texts
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) accentColor.copy(alpha = 0.22f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = displayInfo.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = displayInfo.mainTitle,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(1.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (displayInfo.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (isSelected) accentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = displayInfo.badgeText,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                                ),
                                                color = if (isSelected) accentColor.copy(alpha = 0.85f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Right Section: Selection Checkmark Badge
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = Color.Black,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // Divider inside card
                            if (index < group.options.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 62.dp, end = 12.dp),
                                    thickness = 0.5.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E, name = "Sort Bottom Sheet - Dark")
@Composable
private fun SortBottomSheetDarkPreview() {
    SortBottomSheet(
        sheetState = rememberModalBottomSheetState(),
        currentOption = SortOption.MODIFIED_DESC,
        onOptionSelected = {},
        onDismiss = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Sort Bottom Sheet - Light")
@Composable
private fun SortBottomSheetLightPreview() {
    SortBottomSheet(
        sheetState = rememberModalBottomSheetState(),
        currentOption = SortOption.TITLE_ASC,
        onOptionSelected = {},
        onDismiss = {}
    )
}
