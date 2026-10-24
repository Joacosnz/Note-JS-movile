package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme

/**
 * Renderizador oficial de ecuaciones matemáticas KaTeX para Jetpack Compose.
 * Ejecuta el motor KaTeX de manera 100% offline mediante recursos locales
 * (katex.min.js, katex.min.css y fuentes WOFF2 integradas en assets/katex).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXMathView(
    latex: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkTheme(),
    color: Color? = null,
    displayMode: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    if (latex.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Fórmula vacía",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val defaultAmber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val targetColor = color ?: defaultAmber
    val targetColorHex = String.format("#%06X", 0xFFFFFF and targetColor.toArgb())

    var webViewHeightDp by remember { mutableFloatStateOf(60f) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasError by remember { mutableStateOf(false) }

    // Interfaz JavaScript para recibir la altura exacta calculada por KaTeX
    val mathBridge = remember {
        object {
            @JavascriptInterface
            fun onRendered(heightPx: Float) {
                Handler(Looper.getMainLooper()).post {
                    val densityVal = density.density
                    val calculatedDp = (heightPx / densityVal).coerceIn(40f, 650f)
                    webViewHeightDp = calculatedDp + 16f
                }
            }
        }
    }

    fun escapeForJs(code: String): String {
        return code
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    fun dispatchFormulaUpdate(wv: WebView?) {
        if (wv != null && isPageLoaded) {
            val escapedCode = escapeForJs(latex)
            val jsCall = "javascript:updateFormula(\"$escapedCode\", $displayMode, \"$targetColorHex\");"
            wv.post {
                wv.evaluateJavascript(jsCall, null)
            }
        }
    }

    LaunchedEffect(latex, isDark, targetColorHex, displayMode, isPageLoaded) {
        dispatchFormulaUpdate(webViewRef)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("katex_math_container")
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            // Respaldo de seguridad en caso de fallo crítico en WebView
            NativeMathView(
                expression = latex,
                isDark = isDark,
                fontSize = 20.sp,
                color = targetColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(webViewHeightDp.dp)
                    .heightIn(min = 44.dp, max = 700.dp),
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            webViewRef = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = true

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = false
                            }

                            addJavascriptInterface(mathBridge, "AndroidMathBridge")

                            var downX = 0f
                            var downY = 0f
                            var isClick = false

                            setOnTouchListener { _, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        downX = event.x
                                        downY = event.y
                                        isClick = true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        if (Math.abs(event.x - downX) > 15f || Math.abs(event.y - downY) > 15f) {
                                            isClick = false
                                        }
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        if (isClick && onClick != null) {
                                            onClick()
                                            return@setOnTouchListener true
                                        }
                                    }
                                }
                                false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoaded = true
                                    dispatchFormulaUpdate(view)
                                }
                            }

                            loadUrl("file:///android_asset/katex/katex_template.html")
                        }
                    } catch (_: Throwable) {
                        hasError = true
                        WebView(ctx)
                    }
                },
                update = { wv ->
                    webViewRef = wv
                    dispatchFormulaUpdate(wv)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.destroy()
            } catch (_: Throwable) {}
            webViewRef = null
        }
    }
}
