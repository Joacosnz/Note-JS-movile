package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun InlineMathView(
    content: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        NativeMathView(
            expression = content,
            isDark = isDark,
            fontSize = fontSize,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
