package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InlineMathView(
    content: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDark) "white" else "black"
    val html = remember(content, isDark) {
        val parsedContent = content
            .replace(Regex("""\[math:(.*?)\]""")) { "\$" + it.groupValues[1] + "\$" }
            .replace("\n", "<br>")

        """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"
                onload="renderMathInElement(document.body, {
                    delimiters: [
                        {left: '${'$'}${'$'}', right: '${'$'}${'$'}', display: true},
                        {left: '${'$'}', right: '${'$'}', display: false}
                    ]
                });"></script>
            <style>
                body {
                    background-color: transparent;
                    color: $textColor;
                    font-family: sans-serif;
                    font-size: 18px; /* Debe coincidir con el tamaño de la tipografía normal de la app */
                    line-height: 1.5;
                    margin: 0;
                    padding: 4px 8px;
                    text-align: left;
                    word-wrap: break-word;
                }
            </style>
        </head>
        <body>
            $parsedContent
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier.clickable { onClick() }
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}
