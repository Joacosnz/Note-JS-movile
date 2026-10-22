package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import kotlinx.coroutines.delay

@Composable
fun MathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(block.latexCode.isEmpty()) }
    var copiedFeedback by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current
    val amber = Color(0xFFFFB800)
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
    val inputBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    val quickTemplates = remember {
        listOf(
            "Fracción" to "\\frac{a}{b}",
            "Raíz" to "\\sqrt{x}",
            "Potencia" to "x^{2}",
            "Subíndice" to "x_{i}",
            "Suma" to "\\sum_{i=1}^{n}",
            "Integral" to "\\int_{a}^{b}",
            "Límite" to "\\lim_{x \\to \\infty}",
            "π" to "\\pi",
            "±" to "\\pm",
            "∞" to "\\infty",
            "×" to "\\times",
            "·" to "\\cdot",
            "≠" to "\\neq",
            "≤" to "\\leq",
            "≥" to "\\geq",
            "→" to "\\to"
        )
    }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(1800)
            copiedFeedback = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .animateContentSize()
    ) {
        if (isEditing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, amber.copy(alpha = 0.6f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Header Toolbar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(amber.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = null,
                                    tint = amber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fórmula Matemática (LaTeX)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDeleteBlock,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { isEditing = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = amber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Listo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(inputBg, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (block.latexCode.isEmpty()) {
                            Text(
                                text = "Ej: \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                                style = TextStyle(
                                    color = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        BasicTextField(
                            value = block.latexCode,
                            onValueChange = { newCode ->
                                onUpdateBlock(block.copy(latexCode = newCode))
                            },
                            textStyle = TextStyle(
                                color = if (isDark) amber else Color(0xFFB45309),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(amber),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { isEditing = false }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Template Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickTemplates) { (label, template) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                                modifier = Modifier.clickable {
                                    val current = block.latexCode
                                    val updated = if (current.isEmpty()) {
                                        template
                                    } else if (current.endsWith(" ") || current.endsWith("{")) {
                                        "$current$template"
                                    } else {
                                        "$current $template"
                                    }
                                    onUpdateBlock(block.copy(latexCode = updated))
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-Time Live Preview Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) Color(0xFF141416) else Color(0xFFF1F2F6),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) amber.copy(alpha = 0.25f) else Color(0xFFD1D5DB)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VISTA PREVIA EN TIEMPO REAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (block.latexCode.isBlank()) {
                                Text(
                                    text = "Escribe una fórmula para previsualizar aquí...",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            } else {
                                NativeMathView(
                                    expression = block.latexCode,
                                    isDark = isDark,
                                    fontSize = 19.sp,
                                    color = if (isDark) amber else Color(0xFFB45309),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Toca \"Listo\" para fijar la fórmula en la nota.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        } else {
            // Rendered Formula View
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditing = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = copiedFeedback,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "¡Copiado!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(block.latexCode))
                                copiedFeedback = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copiar LaTeX",
                                    tint = if (copiedFeedback) Color(0xFF10B981) else if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LaTeX",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = amber.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { isEditing = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = if (isDark) amber else Color(0xFFB45309),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Editar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) amber else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NativeMathView(
                            expression = block.latexCode.ifBlank { "Fórmula vacía (toca para editar)" },
                            isDark = isDark,
                            fontSize = 20.sp,
                            color = if (isDark) amber else Color(0xFFB45309),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
