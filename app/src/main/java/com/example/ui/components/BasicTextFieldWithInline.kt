package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset

@Composable
fun BasicTextFieldWithInline(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() },
    inlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            onTextLayout = {
                layoutResult = it
                onTextLayout(it)
            },
            cursorBrush = cursorBrush,
            decorationBox = decorationBox
        )

        // Overlay inline content
        layoutResult?.let { layout ->
            val text = layout.layoutInput.text
            text.getStringAnnotations("INLINE_CONTENT", 0, text.length).forEach { annotation ->
                val inlineContentItem = inlineContent[annotation.item]
                if (inlineContentItem != null) {
                    val boundingBox = layout.getBoundingBox(annotation.start)
                    with(density) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = boundingBox.left.toInt(),
                                        y = (boundingBox.top + (boundingBox.height - inlineContentItem.placeholder.height.toPx()) / 2).toInt()
                                    )
                                }
                                .size(
                                    width = inlineContentItem.placeholder.width.toDp(),
                                    height = inlineContentItem.placeholder.height.toDp()
                                )
                        ) {
                            inlineContentItem.children(annotation.item)
                        }
                    }
                }
            }
        }
    }
}
