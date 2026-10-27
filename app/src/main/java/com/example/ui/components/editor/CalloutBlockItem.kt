@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onInlineMathClick
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.sanitizeTextFieldValue
import com.example.util.MathEvaluator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.RichLinkPreviewList
import com.example.ui.components.RichLinkQuickPreviewDialog
import com.example.ui.components.onWebLinkClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalloutBlockItem(
    block: NoteBlock.CalloutBox,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onBlockUpdated: (NoteBlock.CalloutBox) -> Unit,
    onDelete: () -> Unit,
    onNoteLinkClick: (Long) -> Unit,
    onInlineMathClick: (code: String, start: Int, end: Int) -> Unit,
    evaluateNoteLinkTrigger: (String, TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    amber: Color,
    paperTextColor: Color,
    paperHintColor: Color,
    isPaperDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showCalloutMenu by remember { mutableStateOf(false) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }
    var linkModeRefreshTrigger by remember { mutableIntStateOf(0) }

    val calloutBg = if (!block.backgroundColorHex.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(block.backgroundColorHex)) }.getOrDefault(
            if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        )
    } else {
        if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    }

    val calloutFontFamily = when (block.fontType) {
        "Serif" -> FontFamily.Serif
        "Mono" -> FontFamily.Monospace
        "Cursiva" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("block_callout_$index")
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCalloutMenu = true
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            color = calloutBg.copy(alpha = 0.15f), // Soft background tint
            border = BorderStroke(1.dp, calloutBg.copy(alpha = 0.4f)) // Subtle border
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Border/Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp) // Minimum height
                        .background(calloutBg, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Emoji (if present)
                if (block.emoji != null) {
                    Text(
                        text = block.emoji!!,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                    )
                }

                // Text
                BasicTextField(
                    value = tfv,
                    onValueChange = { newValue ->
                        val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, tfv)
                        val effective = solved ?: newValue
                        val sanitized = sanitizeTextFieldValue(effective)
                        evaluateNoteLinkTrigger(block.id, sanitized)
                        onValueChange(sanitized)
                        coroutineScope.launch {
                            val layout = textLayoutResult
                            val cursorOffset = sanitized.selection.max
                            if ((layout != null) && (cursorOffset in 0..layout.layoutInput.text.length)) {
                                val cursorRect = layout.getCursorRect(cursorOffset)
                                val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                bringIntoViewRequester.bringIntoView(expandedRect)
                            } else {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .weight(1f)
                        .onNoteLinkClick(textLayoutResult) { linkedId -> onNoteLinkClick(linkedId) }
                        .onWebLinkClick(textLayoutResult) { webUrl -> previewingWebUrl = webUrl }
                        .onInlineMathClick(textLayoutResult) { code, start, end -> onInlineMathClick(code, start, end) }
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            onFocusChanged(focusState.isFocused)
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    delay(100.milliseconds)
                                    val layout = textLayoutResult
                                    val cursorOffset = tfv.selection.max
                                    if ((layout != null) && (cursorOffset in 0..layout.layoutInput.text.length)) {
                                        val cursorRect = layout.getCursorRect(cursorOffset)
                                        val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                        bringIntoViewRequester.bringIntoView(expandedRect)
                                    } else {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        color = paperTextColor,
                        fontFamily = calloutFontFamily,
                        fontStyle = if (block.fontType == "Estándar") FontStyle.Italic else FontStyle.Normal
                    ),
                    cursorBrush = SolidColor(amber),
                    visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                    decorationBox = { innerTextField ->
                        if (block.content.isEmpty()) {
                            Text(
                                text = "Cuadro destacado / Nota importante...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    color = paperHintColor.copy(alpha = 0.5f),
                                    fontFamily = calloutFontFamily,
                                    fontStyle = if (block.fontType == "Estándar") FontStyle.Italic else FontStyle.Normal
                                )
                            )
                        }
                        innerTextField()
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    )
                )

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCalloutMenu = true
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_edit_callout_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Personalizar cuadro destacado",
                        tint = calloutBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        RichLinkPreviewList(
            text = tfv.text,
            amber = amber,
            isDark = isPaperDark,
            externalRefreshTrigger = linkModeRefreshTrigger,
            onConvertToInlineLink = { link, cleanTitle ->
                val raw = tfv.text
                val safeStart = link.startIndex.coerceIn(0, raw.length)
                val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                val markdownLink = "[$cleanTitle](${link.url})"
                val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                onValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
            },
            onInsertWebClip = { clipText ->
                val current = tfv.text.trimEnd()
                val updated = if (current.isEmpty()) clipText else "$current\n$clipText"
                onValueChange(TextFieldValue(updated, TextRange(updated.length)))
            }
        )

        previewingWebUrl?.let { targetUrl ->
            RichLinkQuickPreviewDialog(
                url = targetUrl,
                blockText = tfv.text,
                amber = amber,
                isDark = isPaperDark,
                onDismiss = { previewingWebUrl = null },
                onModeChanged = { linkModeRefreshTrigger++ },
                onConvertRawUrlToInlineTitle = { link, cleanTitle ->
                    val raw = tfv.text
                    val safeStart = link.startIndex.coerceIn(0, raw.length)
                    val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                    val markdownLink = "[$cleanTitle](${link.url})"
                    val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                    onValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
                },
                onInsertWebClip = { clipText ->
                    val current = tfv.text.trimEnd()
                    val updated = if (current.isEmpty()) clipText else "$current\n$clipText"
                    onValueChange(TextFieldValue(updated, TextRange(updated.length)))
                }
            )
        }
    }

    if (showCalloutMenu) {
        Dialog(
            onDismissRequest = { showCalloutMenu = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            var isSheetVisible by remember { mutableStateOf(false) }
            var showEmojiPicker by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isSheetVisible = true
            }

            val dismissWithAnimation: () -> Unit = {
                isSheetVisible = false
                coroutineScope.launch {
                    delay(160.milliseconds)
                    showCalloutMenu = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            ) {
                // Tocar afuera en la parte superior cierra el menú con animación
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            dismissWithAnimation()
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // PARTE SUPERIOR: Cuadro destacado en GRANDE que no ocupa el menú de edición
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Badge identificador
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, amber.copy(alpha = 0.55f)),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(amber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "VISTA PREVIA EN VIVO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amber,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Cuadro destacado EN GRANDE
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = calloutBg.copy(alpha = 0.55f),
                                        ambientColor = Color.Black.copy(alpha = 0.35f)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isPaperDark) {
                                    calloutBg.copy(alpha = 0.22f)
                                } else {
                                    calloutBg.copy(alpha = 0.26f)
                                },
                                border = BorderStroke(1.8.dp, calloutBg.copy(alpha = 0.70f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Barra vertical de acento lateral
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(calloutBg)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Ícono / Emoji destacado en grande
                                    if (!block.emoji.isNullOrBlank()) {
                                        Text(
                                            text = block.emoji!!,
                                            fontSize = 32.sp,
                                            modifier = Modifier.padding(top = 1.dp, end = 12.dp)
                                        )
                                    }

                                    // Texto con su tipografía en tiempo real
                                    val previewText = block.content.ifBlank { "Cuadro destacado / Nota importante..." }
                                    Text(
                                        text = previewText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 18.sp,
                                            lineHeight = 26.sp,
                                            color = if (block.content.isBlank()) paperHintColor.copy(alpha = 0.6f) else paperTextColor,
                                            fontFamily = calloutFontFamily,
                                            fontStyle = if (block.fontType == "Estándar") FontStyle.Italic else FontStyle.Normal
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(max = 160.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                    }

                    // PARTE INFERIOR: Menú desplegable de abajo hacia arriba
                    AnimatedVisibility(
                        visible = isSheetVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(150)
                        ) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* intercepta clics dentro del menú para no cerrar */ },
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = if (isPaperDark) Color(0xFF1E1E20) else Color(0xFFF9F9FB),
                            tonalElevation = 6.dp,
                            shadowElevation = 18.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 440.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                // Indicador de arrastre (drag handle)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Cabecera con título y botón de cerrar (X)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = amber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Personalizar Cuadro",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { dismissWithAnimation() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 1. BOTÓN DE CAMBIAR ÍCONO
                                Text(
                                    text = "Ícono o Emoji",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val quickEmojis = listOf("💡", "📌", "⚠️", "⭐", "🚀", "📝", "🔥", "🎯", "💬", "🔔")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Botón para quitar ícono
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (block.emoji == null) amber.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (block.emoji == null) amber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBlockUpdated(block.copy(emoji = null))
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = "🚫", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Sin ícono",
                                                fontSize = 12.sp,
                                                fontWeight = if (block.emoji == null) FontWeight.Bold else FontWeight.Normal,
                                                color = if (block.emoji == null) amber else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Emojis rápidos
                                    quickEmojis.forEach { emoji ->
                                        val isSelected = block.emoji == emoji
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) amber.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) amber else Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onBlockUpdated(block.copy(emoji = emoji))
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = emoji, fontSize = 20.sp)
                                            }
                                        }
                                    }

                                    // Botón para abrir selector completo de emojis nativo
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            showEmojiPicker = true
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = "➕", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Más...",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 2. BOTÓN DE CAMBIAR TIPOGRAFÍA
                                Text(
                                    text = "Tipografía",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val fonts = listOf(
                                    "Estándar" to FontFamily.Default,
                                    "Serif" to FontFamily.Serif,
                                    "Mono" to FontFamily.Monospace,
                                    "Cursiva" to FontFamily.Cursive
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    fonts.forEach { (name, family) ->
                                        val isSelected = block.fontType == name || (block.fontType.isNullOrBlank() && name == "Estándar")
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) amber.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                                color = if (isSelected) amber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onBlockUpdated(block.copy(fontType = name))
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = amber,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = name,
                                                    fontFamily = family,
                                                    fontStyle = if (name == "Estándar") FontStyle.Italic else FontStyle.Normal,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) amber else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 3. BOTÓN DE CAMBIAR COLORES
                                Text(
                                    text = "Color de resalte y fondo",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val colorOptions = listOf(
                                    "#FDE047", // Amarillo
                                    "#93C5FD", // Azul
                                    "#86EFAC", // Verde
                                    "#FCA5A5", // Rojo
                                    "#D8B4FE", // Morado
                                    "#FDBA74", // Naranja
                                    "#F9A8D4", // Rosa
                                    "#67E8F9", // Cian
                                    "#94A3B8", // Gris
                                    "#334155"  // Pizarra
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colorOptions.forEach { hex ->
                                        val isSelected = block.backgroundColorHex.equals(hex, ignoreCase = true)
                                        val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(amber)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) amber else Color.White.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onBlockUpdated(block.copy(backgroundColorHex = hex))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Seleccionado",
                                                    tint = Color.Black.copy(alpha = 0.75f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                // 4. BOTONES MEJORADOS DE ELIMINAR Y CERRAR / LISTO
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Botón Eliminar Mejorado
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dismissWithAnimation()
                                                onDelete()
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Eliminar",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            )
                                        }
                                    }

                                    // Botón Listo / Cerrar Mejorado
                                    Surface(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                dismissWithAnimation()
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = amber
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Listo",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            if (showEmojiPicker) {
                com.example.ui.components.NativeEmojiInputDialog(
                    onDismiss = { showEmojiPicker = false },
                    onEmojiSelected = { emoji ->
                        onBlockUpdated(block.copy(emoji = emoji))
                        showEmojiPicker = false
                    },
                    onRemoveIcon = {
                        onBlockUpdated(block.copy(emoji = null))
                        showEmojiPicker = false
                    }
                )
            }
        }
    }
}
