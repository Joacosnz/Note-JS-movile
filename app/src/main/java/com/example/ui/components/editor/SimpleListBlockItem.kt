package com.example.ui.components.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun SimpleListBlockItem(
    block: NoteBlock.SimpleList,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onNext: () -> Unit,
    onNoteLinkClick: (Long) -> Unit,
    onInlineMathClick: (code: String, start: Int, end: Int) -> Unit,
    evaluateNoteLinkTrigger: (String, TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    amber: Color,
    paperTextColor: Color,
    paperHintColor: Color,
    isPaperDark: Boolean,
    customFontSizeSp: Int,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isHighlightActive: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("block_simple_list_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (block.isNumbered) "${block.number}." else "•",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = amber,
                fontSize = 17.sp
            ),
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
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
                    if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                .onInlineMathClick(textLayoutResult) { code, start, end -> onInlineMathClick(code, start, end) }
                .bringIntoViewRequester(bringIntoViewRequester)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(100)
                            val layout = textLayoutResult
                            val cursorOffset = tfv.selection.max
                            if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                fontSize = customFontSizeSp.sp,
                fontWeight = if (isBoldActive) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal,
                background = if (isHighlightActive) amber.copy(alpha = 0.35f) else Color.Transparent,
                color = paperTextColor
            ),
            cursorBrush = SolidColor(amber),
            visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
            decorationBox = { innerTextField ->
                if (block.content.isEmpty()) {
                    Text(
                        text = "Elemento de lista...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = paperHintColor.copy(alpha = 0.5f))
                    )
                }
                innerTextField()
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() }
            )
        )
    }
}
