package com.example.ui.components.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.ui.components.NativeMathView
import kotlinx.coroutines.delay

/**
 * Bloque de ecuación matemática idéntico a los Equation Blocks de Notion.
 * Características de Notion:
 * 1. Modo lectura limpio: fórmula centrada sin bordes pesados, con placeholder al estar vacía.
 * 2. Editor interactivo en tarjeta emergente con entrada multilínea monospace.
 * 3. Previsualización KaTeX en tiempo real con detección visual de errores de sintaxis.
 * 4. Paleta de acceso rápido categorizada con fórmulas y símbolos KaTeX comunes.
 * 5. Acciones rápidas: Copiar código LaTeX, Editar y Eliminar bloque.
 */
@Composable
fun NotionMathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    isDark: Boolean,
    amber: Color,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(block.latexCode.isEmpty()) }
    var copiedFeedback by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    val cardBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF7F7F9)
    val inputBg = if (isDark) Color(0xFF131316) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF2E2E34) else Color(0xFFE4E4E8)
    val textPrimary = if (isDark) Color(0xFFEEEEF0) else Color(0xFF1A1A1E)
    val textSecondary = if (isDark) Color(0xFF9E9EA8) else Color(0xFF6E6E78)

    // Categorías de fórmulas y símbolos KaTeX estilo Notion
    val categories = remember {
        listOf("Frecuentes", "Cálculo", "Griego", "Relaciones", "Matrices")
    }

    val templatesByCategory = remember {
        mapOf(
            0 to listOf(
                "Fracción" to "\\frac{a}{b}",
                "Raíz" to "\\sqrt{x}",
                "Potencia" to "x^{2}",
                "Subíndice" to "x_{i}",
                "±" to "\\pm",
                "×" to "\\times",
                "·" to "\\cdot",
                "÷" to "\\div"
            ),
            1 to listOf(
                "Sumatoria" to "\\sum_{i=1}^{n}",
                "Integral" to "\\int_{a}^{b} f(x) dx",
                "Límite" to "\\lim_{x \\to \\infty}",
                "Derivada" to "\\frac{df}{dx}",
                "Parcial" to "\\frac{\\partial y}{\\partial x}",
                "Infinito" to "\\infty",
                "Vector" to "\\vec{v}"
            ),
            2 to listOf(
                "π" to "\\pi",
                "α" to "\\alpha",
                "β" to "\\beta",
                "θ" to "\\theta",
                "λ" to "\\lambda",
                "Δ" to "\\Delta",
                "σ" to "\\sigma",
                "ω" to "\\omega",
                "μ" to "\\mu",
                "Ω" to "\\Omega"
            ),
            3 to listOf(
                "≤" to "\\leq",
                "≥" to "\\geq",
                "≠" to "\\neq",
                "≈" to "\\approx",
                "≡" to "\\equiv",
                "∈" to "\\in",
                "∉" to "\\notin",
                "⊂" to "\\subset",
                "∪" to "\\cup",
                "∩" to "\\cap",
                "→" to "\\to"
            ),
            4 to listOf(
                "Matriz 2x2" to "\\begin{matrix} a & b \\\\ c & d \\end{matrix}",
                "Paréntesis" to "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
                "Determinante" to "\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}",
                "Sistema" to "\\begin{cases} x + y = 1 \\\\ 2x - y = 0 \\end{cases}"
            )
        )
    }

    // Detector de errores de sintaxis KaTeX
    val syntaxError: String? = remember(block.latexCode) {
        val code = block.latexCode
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        val openBrackets = code.count { it == '[' }
        val closeBrackets = code.count { it == ']' }
        when {
            openBraces > closeBraces -> "Llave abierta sin cerrar: falta '}'"
            closeBraces > openBraces -> "Llave de cierre extra: '}' sin pareja"
            openBrackets > closeBrackets -> "Corchete abierto sin cerrar: falta ']'"
            closeBrackets > openBrackets -> "Corchete de cierre extra: ']' sin pareja"
            code.endsWith("\\") -> "Comando incompleto al final ('\\')"
            code.contains("\\begin{") && !code.contains("\\end{") -> "Entorno '\\begin' sin '\\end'"
            else -> null
        }
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
            // TARJETA DE EDICIÓN ESTILO NOTION
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notion_math_editor_card"),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, amber.copy(alpha = 0.7f)),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Cabecera Notion
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = amber.copy(alpha = 0.18f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Functions,
                                        contentDescription = "Ecuación matemática",
                                        tint = amber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Ecuación en bloque",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Compatible con KaTeX y LaTeX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = textSecondary
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteBlock()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = amber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Listo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caja de Entrada Monospace Multilínea
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(inputBg)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (syntaxError != null) Color(0xFFEF4444).copy(alpha = 0.7f) else borderColor
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        if (block.latexCode.isEmpty()) {
                            Text(
                                text = "Escribe una fórmula... Ej: \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} o E = mc^2",
                                style = TextStyle(
                                    color = textSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { isEditing = false }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notion_math_input_field")
                        )
                    }

                    // Notificación de error de sintaxis si existe
                    AnimatedVisibility(
                        visible = syntaxError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = syntaxError ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFEF4444)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pestañas de categorías de símbolos Notion
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories.indices.toList()) { catIdx ->
                            val isSelected = selectedCategoryIndex == catIdx
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategoryIndex = catIdx
                                },
                                label = {
                                    Text(
                                        text = categories[catIdx],
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amber.copy(alpha = 0.22f),
                                    selectedLabelColor = if (isDark) amber else Color(0xFFB45309),
                                    containerColor = if (isDark) Color(0xFF26262B) else Color(0xFFEAEAEE),
                                    labelColor = textSecondary
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips de plantillas de la categoría activa
                    val activeTemplates = templatesByCategory[selectedCategoryIndex].orEmpty()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeTemplates) { (label, template) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF2B2B30) else Color(0xFFE2E2E8),
                                border = BorderStroke(0.5.dp, borderColor),
                                modifier = Modifier.clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val current = block.latexCode
                                    val updated = when {
                                        current.isEmpty() -> template
                                        current.endsWith(" ") || current.endsWith("{") || current.endsWith("&") -> "$current$template"
                                        else -> "$current $template"
                                    }
                                    onUpdateBlock(block.copy(latexCode = updated))
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CAJA DE PREVISUALIZACIÓN EN TIEMPO REAL (KaTeX Live Preview)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF141416) else Color(0xFFEDEDF2))
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
                            .padding(12.dp)
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
                                    color = textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (block.latexCode.isBlank()) {
                                Text(
                                    text = "La ecuación renderizada aparecerá aquí...",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = textSecondary.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            } else {
                                NativeMathView(
                                    expression = block.latexCode,
                                    isDark = isDark,
                                    fontSize = 20.sp,
                                    color = if (isDark) amber else Color(0xFFB45309),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Toca \"Listo\" para incrustar la fórmula en tu nota.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    )
                }
            }
        } else {
            // MODO VISUALIZACIÓN LIMPIO ESTILO NOTION
            if (block.latexCode.isBlank()) {
                // Placeholder interactivo estilo Notion
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isEditing = true
                        }
                        .testTag("notion_math_empty_placeholder"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF222227) else Color(0xFFF2F2F6),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Añadir una ecuación matemática...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        )
                    }
                }
            } else {
                // Fórmula renderizada limpia y centrada
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isEditing = true
                        }
                        .testTag("notion_math_rendered_block"),
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Barra de herramientas sutil superior
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = null,
                                    tint = amber.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ecuación",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    color = if (isDark) Color(0xFF2A2A30) else Color(0xFFE6E6EC),
                                    modifier = Modifier.clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        clipboardManager.setText(AnnotatedString(block.latexCode))
                                        copiedFeedback = true
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copiar LaTeX",
                                            tint = if (copiedFeedback) Color(0xFF10B981) else textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copiar",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = amber.copy(alpha = 0.16f),
                                    modifier = Modifier.clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isEditing = true
                                    }
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
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fórmula renderizada centrada con tipografía matemática nítida
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NativeMathView(
                                expression = block.latexCode,
                                isDark = isDark,
                                fontSize = 22.sp,
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
}
