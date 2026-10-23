@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.CardDisplayMode
import com.example.util.ExtractedWebLink
import com.example.util.WebLinkMetadata
import com.example.util.WebLinkMetadataFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RichLinkPreviewList(
    text: String,
    amber: Color,
    isDark: Boolean,
    onConvertToInlineLink: ((ExtractedWebLink, String) -> Unit)? = null,
    externalRefreshTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val links = remember(text) { WebLinkMetadataFetcher.extractWebLinks(text) }
    if (links.isEmpty()) return

    var modeRefreshTrigger by remember { mutableIntStateOf(0) }

    val visibleLinks = remember(links, modeRefreshTrigger, externalRefreshTrigger) {
        links.filter { WebLinkMetadataFetcher.getDisplayMode(context, it.url) != CardDisplayMode.HIDDEN }
    }
    val hiddenLinks = remember(links, modeRefreshTrigger, externalRefreshTrigger) {
        links.filter { WebLinkMetadataFetcher.getDisplayMode(context, it.url) == CardDisplayMode.HIDDEN }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleLinks.forEach { link ->
            RichLinkPreviewCard(
                extractedLink = link,
                amber = amber,
                isDark = isDark,
                onModeChanged = { modeRefreshTrigger++ },
                onConvertToInlineLink = onConvertToInlineLink
            )
        }

        if (hiddenLinks.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                hiddenLinks.forEach { hidden ->
                    val domain = remember(hidden.url) { WebLinkMetadataFetcher.extractDomain(hidden.url) }
                    Surface(
                        onClick = {
                            WebLinkMetadataFetcher.setDisplayMode(context, hidden.url, CardDisplayMode.COMPACT)
                            modeRefreshTrigger++
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.7f) else Color(0xFFE2E8F0).copy(alpha = 0.7f),
                        border = BorderStroke(0.8.dp, amber.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Mostrar tarjeta",
                                tint = amber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Mostrar tarjeta: $domain",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichLinkPreviewCard(
    extractedLink: ExtractedWebLink,
    amber: Color,
    isDark: Boolean,
    onModeChanged: () -> Unit = {},
    onConvertToInlineLink: ((ExtractedWebLink, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val url = extractedLink.url
    var displayMode by remember(url) {
        mutableStateOf(WebLinkMetadataFetcher.getDisplayMode(context, url))
    }
    var metadata by remember(url) {
        mutableStateOf(
            WebLinkMetadataFetcher.getCachedMetadata(context, url)
                ?: WebLinkMetadataFetcher.buildFallbackMetadata(url, extractedLink.inlineTitle)
        )
    }
    var isLoading by remember(url) {
        mutableStateOf(WebLinkMetadataFetcher.getCachedMetadata(context, url) == null)
    }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        val cached = WebLinkMetadataFetcher.getCachedMetadata(context, url)
        if (cached != null) {
            metadata = cached
            isLoading = false
        } else {
            isLoading = true
            val fetched = WebLinkMetadataFetcher.fetchMetadata(context, url, forceRefresh = false)
            metadata = fetched
            isLoading = false
        }
    }

    val effectiveTitle = extractedLink.inlineTitle?.takeIf { it.isNotBlank() } ?: metadata.title
    val cardBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    openUrlInBrowser(context, url)
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
            .testTag("rich_link_card_${metadata.domain}"),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (displayMode == CardDisplayMode.COMPACT) {
                // VISTA COMPACTA (Miniatura lateral estilo Apple Notes pequeño)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Miniatura lateral o insignia del sitio
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        amber.copy(alpha = 0.25f),
                                        amber.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!metadata.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = metadata.imageUrl,
                                contentDescription = effectiveTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (metadata.faviconUrl.isNotBlank()) {
                            AsyncImage(
                                model = metadata.faviconUrl,
                                contentDescription = metadata.domain,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (metadata.faviconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = metadata.faviconUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Text(
                                text = metadata.siteName.ifBlank { metadata.domain }.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = amber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = amber
                                )
                            }
                        }

                        Text(
                            text = effectiveTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = metadata.description.ifBlank { metadata.domain },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val nextMode = CardDisplayMode.LARGE
                                displayMode = nextMode
                                WebLinkMetadataFetcher.setDisplayMode(context, url, nextMode)
                                onModeChanged()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoSizeSelectLarge,
                                contentDescription = "Cambiar a vista grande",
                                tint = subtitleColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones de enlace",
                                tint = subtitleColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // VISTA GRANDE (Tarjeta Enriquecida con imagen Open Graph destacada)
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!metadata.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .background(
                                    if (isDark) Color(0xFF14171F) else Color(0xFFE2E8F0)
                                )
                        ) {
                            AsyncImage(
                                model = metadata.imageUrl,
                                contentDescription = effectiveTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Gradiente sutil en la base e insignia de dominio flotante
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (metadata.faviconUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = metadata.faviconUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = amber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = metadata.domain,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            val nextMode = CardDisplayMode.COMPACT
                                            displayMode = nextMode
                                            WebLinkMetadataFetcher.setDisplayMode(context, url, nextMode)
                                            onModeChanged()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ViewCompact,
                                            contentDescription = "Cambiar a vista compacta",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable { showMenu = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opciones de enlace",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Encabezado atmosférico cuando la página no define og:image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            amber.copy(alpha = if (isDark) 0.22f else 0.16f),
                                            amber.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF2C3240) else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (metadata.faviconUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = metadata.faviconUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = amber,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = metadata.siteName.ifBlank { metadata.domain }.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.7.sp
                                            ),
                                            color = amber
                                        )
                                        Text(
                                            text = metadata.domain,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = subtitleColor
                                        )
                                    }
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = amber
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val nextMode = CardDisplayMode.COMPACT
                                            displayMode = nextMode
                                            WebLinkMetadataFetcher.setDisplayMode(context, url, nextMode)
                                            onModeChanged()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewCompact,
                                            contentDescription = "Vista compacta",
                                            tint = subtitleColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opciones de enlace",
                                            tint = subtitleColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cuerpo de texto de la tarjeta grande
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = effectiveTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            ),
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (metadata.description.isNotBlank() && metadata.description != metadata.url) {
                            Text(
                                text = metadata.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = subtitleColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = subtitleColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir enlace",
                                tint = amber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Menú contextual de la tarjeta (Apple Notes / Keep Hybrid)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Abrir en el navegador") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = amber)
                    },
                    onClick = {
                        showMenu = false
                        openUrlInBrowser(context, url)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            if (displayMode == CardDisplayMode.COMPACT) "Ver tarjeta grande (Rich Link)"
                            else "Ver tarjeta compacta (Miniatura)"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (displayMode == CardDisplayMode.COMPACT) Icons.Default.PhotoSizeSelectLarge else Icons.Default.ViewCompact,
                            contentDescription = null,
                            tint = amber
                        )
                    },
                    onClick = {
                        showMenu = false
                        val nextMode = if (displayMode == CardDisplayMode.COMPACT) CardDisplayMode.LARGE else CardDisplayMode.COMPACT
                        displayMode = nextMode
                        WebLinkMetadataFetcher.setDisplayMode(context, url, nextMode)
                        onModeChanged()
                    }
                )

                if (onConvertToInlineLink != null && extractedLink.inlineTitle == null) {
                    DropdownMenuItem(
                        text = { Text("Reemplazar URL por título en el texto") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = amber)
                        },
                        onClick = {
                            showMenu = false
                            val cleanTitle = effectiveTitle.replace("[", "(").replace("]", ")")
                            onConvertToInlineLink(extractedLink, cleanTitle)
                        }
                    )
                }

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("Copiar dirección del enlace") },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("URL", url))
                        Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Compartir enlace") },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, effectiveTitle)
                            putExtra(Intent.EXTRA_TEXT, "$effectiveTitle\n$url")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir enlace"))
                    }
                )

                DropdownMenuItem(
                    text = { Text("Actualizar vista previa") },
                    leadingIcon = {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        coroutineScope.launch {
                            isLoading = true
                            metadata = WebLinkMetadataFetcher.fetchMetadata(context, url, forceRefresh = true)
                            isLoading = false
                        }
                    }
                )

                DropdownMenuItem(
                    text = { Text("Ocultar tarjeta de vista previa") },
                    leadingIcon = {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        showMenu = false
                        displayMode = CardDisplayMode.HIDDEN
                        WebLinkMetadataFetcher.setDisplayMode(context, url, CardDisplayMode.HIDDEN)
                        onModeChanged()
                    }
                )
            }
        }
    }
}

@Composable
fun CompactRichLinkNoteCardPreview(
    url: String,
    inlineTitle: String? = null,
    amber: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var metadata by remember(url) {
        mutableStateOf(
            WebLinkMetadataFetcher.getCachedMetadata(context, url)
                ?: WebLinkMetadataFetcher.buildFallbackMetadata(url, inlineTitle)
        )
    }

    LaunchedEffect(url) {
        val cached = WebLinkMetadataFetcher.getCachedMetadata(context, url)
        if (cached != null) {
            metadata = cached
        } else {
            metadata = WebLinkMetadataFetcher.fetchMetadata(context, url, forceRefresh = false)
        }
    }

    val effectiveTitle = inlineTitle?.takeIf { it.isNotBlank() } ?: metadata.title

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF252934).copy(alpha = 0.85f) else Color(0xFFF1F5F9),
        border = BorderStroke(0.8.dp, amber.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!metadata.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = metadata.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            } else if (metadata.faviconUrl.isNotBlank()) {
                AsyncImage(
                    model = metadata.faviconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effectiveTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadata.domain,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = amber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = amber.copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun InsertWebLinkDialog(
    amber: Color,
    isDark: Boolean,
    initialSelectedText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (url: String, customTitle: String?, displayMode: CardDisplayMode) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf(initialSelectedText) }
    var selectedMode by remember { mutableStateOf(CardDisplayMode.LARGE) }
    var previewMetadata by remember { mutableStateOf<WebLinkMetadata?>(null) }
    var isFetchingPreview by remember { mutableStateOf(false) }

    val normalizedUrl = remember(urlInput) {
        val trimmed = urlInput.trim()
        if (trimmed.isBlank()) ""
        else if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) trimmed
        else "https://$trimmed"
    }

    val isValidDomain = remember(normalizedUrl) {
        normalizedUrl.length > 10 && normalizedUrl.substringAfter("://").contains(".")
    }

    LaunchedEffect(normalizedUrl) {
        if (!isValidDomain) {
            previewMetadata = null
            isFetchingPreview = false
            return@LaunchedEffect
        }
        delay(350)
        isFetchingPreview = true
        val fetched = WebLinkMetadataFetcher.fetchMetadata(context, normalizedUrl)
        previewMetadata = fetched
        if (titleInput.isBlank() && fetched.isLoadedFromRemote) {
            titleInput = fetched.title
        }
        isFetchingPreview = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Insertar Enlace Enriquecido",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Dirección web (URL)") },
                    placeholder = { Text("https://ejemplo.com/articulo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_web_link_url")
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Título en línea (opcional)") },
                    placeholder = { Text("Se extrae automáticamente de la web") },
                    singleLine = true,
                    trailingIcon = {
                        if (titleInput.isNotEmpty()) {
                            IconButton(onClick = { titleInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_web_link_title")
                )

                Text(
                    text = "Estilo de tarjeta de vista previa",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == CardDisplayMode.LARGE,
                        onClick = { selectedMode = CardDisplayMode.LARGE },
                        label = { Text("Grande") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhotoSizeSelectLarge,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )

                    FilterChip(
                        selected = selectedMode == CardDisplayMode.COMPACT,
                        onClick = { selectedMode = CardDisplayMode.COMPACT },
                        label = { Text("Compacta") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ViewCompact,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )

                    FilterChip(
                        selected = selectedMode == CardDisplayMode.HIDDEN,
                        onClick = { selectedMode = CardDisplayMode.HIDDEN },
                        label = { Text("Solo texto") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )
                }

                AnimatedVisibility(
                    visible = isFetchingPreview || previewMetadata != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E222B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, amber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isFetchingPreview) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = amber
                                )
                                Text(
                                    text = "Extrayendo metadatos Open Graph...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val meta = previewMetadata
                                if (meta != null) {
                                    if (!meta.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = meta.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = amber,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = titleInput.ifBlank { meta.title },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = meta.domain,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = amber,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValidDomain,
                onClick = {
                    WebLinkMetadataFetcher.setDisplayMode(context, normalizedUrl, selectedMode)
                    onConfirm(
                        normalizedUrl,
                        titleInput.trim().takeIf { it.isNotEmpty() },
                        selectedMode
                    )
                }
            ) {
                Text("Insertar", color = if (isValidDomain) amber else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

internal fun openUrlInBrowser(context: Context, rawUrl: String) {
    try {
        val normalized = WebLinkMetadataFetcher.normalizeUrl(rawUrl)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el enlace: $rawUrl", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RichLinkQuickPreviewDialog(
    url: String,
    blockText: String = "",
    amber: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onModeChanged: () -> Unit = {},
    onConvertRawUrlToInlineTitle: ((ExtractedWebLink, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val normalizedUrl = remember(url) { WebLinkMetadataFetcher.normalizeUrl(url) }

    val matchingExtractedLink = remember(blockText, normalizedUrl) {
        WebLinkMetadataFetcher.extractWebLinks(blockText).firstOrNull {
            WebLinkMetadataFetcher.normalizeUrl(it.url).equals(normalizedUrl, ignoreCase = true)
        } ?: ExtractedWebLink(
            url = normalizedUrl,
            rawMatch = normalizedUrl,
            inlineTitle = null,
            startIndex = 0,
            endIndex = 0
        )
    }

    var metadata by remember(normalizedUrl) {
        mutableStateOf(
            WebLinkMetadataFetcher.getCachedMetadata(context, normalizedUrl)
                ?: WebLinkMetadataFetcher.buildFallbackMetadata(normalizedUrl, matchingExtractedLink.inlineTitle)
        )
    }
    var isLoading by remember(normalizedUrl) {
        mutableStateOf(WebLinkMetadataFetcher.getCachedMetadata(context, normalizedUrl) == null)
    }
    var currentMode by remember(normalizedUrl) {
        mutableStateOf(WebLinkMetadataFetcher.getDisplayMode(context, normalizedUrl))
    }

    LaunchedEffect(normalizedUrl) {
        val cached = WebLinkMetadataFetcher.getCachedMetadata(context, normalizedUrl)
        if (cached != null) {
            metadata = cached
            isLoading = false
        } else {
            isLoading = true
            metadata = WebLinkMetadataFetcher.fetchMetadata(context, normalizedUrl, forceRefresh = false)
            isLoading = false
        }
    }

    val effectiveTitle = matchingExtractedLink.inlineTitle?.takeIf { it.isNotBlank() } ?: metadata.title
    val cardBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF8FAFC)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Vista Previa del Enlace",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta de previsualización enriquecida interactiva
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, amber.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            openUrlInBrowser(context, normalizedUrl)
                        }
                        .testTag("quick_preview_rich_link_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!metadata.imageUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .background(if (isDark) Color(0xFF14171F) else Color(0xFFE2E8F0))
                            ) {
                                AsyncImage(
                                    model = metadata.imageUrl,
                                    contentDescription = effectiveTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.3f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.55f)
                                                )
                                            )
                                        )
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (metadata.faviconUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = metadata.faviconUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                        Text(
                                            text = metadata.domain,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(amber.copy(alpha = if (isDark) 0.18f else 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (metadata.faviconUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = metadata.faviconUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = amber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = metadata.siteName.ifBlank { metadata.domain }.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = amber,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.8.dp,
                                        color = amber
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = effectiveTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = titleColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (metadata.description.isNotBlank() && metadata.description != metadata.url) {
                                Text(
                                    text = metadata.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = subtitleColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = normalizedUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = amber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Selector de estilo de tarjeta en la nota
                Text(
                    text = "Visualización en la nota",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentMode == CardDisplayMode.LARGE,
                        onClick = {
                            currentMode = CardDisplayMode.LARGE
                            WebLinkMetadataFetcher.setDisplayMode(context, normalizedUrl, CardDisplayMode.LARGE)
                            onModeChanged()
                        },
                        label = { Text("Grande", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.PhotoSizeSelectLarge, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )
                    FilterChip(
                        selected = currentMode == CardDisplayMode.COMPACT,
                        onClick = {
                            currentMode = CardDisplayMode.COMPACT
                            WebLinkMetadataFetcher.setDisplayMode(context, normalizedUrl, CardDisplayMode.COMPACT)
                            onModeChanged()
                        },
                        label = { Text("Compacta", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.ViewCompact, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )
                    FilterChip(
                        selected = currentMode == CardDisplayMode.HIDDEN,
                        onClick = {
                            currentMode = CardDisplayMode.HIDDEN
                            WebLinkMetadataFetcher.setDisplayMode(context, normalizedUrl, CardDisplayMode.HIDDEN)
                            onModeChanged()
                        },
                        label = { Text("Oculta", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.2f),
                            selectedLabelColor = amber,
                            selectedLeadingIconColor = amber
                        )
                    )
                }

                // Acciones rápidas (Convertir en título limpio, copiar, compartir, recargar)
                if (onConvertRawUrlToInlineTitle != null && matchingExtractedLink.inlineTitle == null && matchingExtractedLink.endIndex > matchingExtractedLink.startIndex) {
                    Surface(
                        onClick = {
                            val cleanTitle = effectiveTitle.replace("[", "(").replace("]", ")")
                            onConvertRawUrlToInlineTitle(matchingExtractedLink, cleanTitle)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = amber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, amber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Convertir URL en título limpio: \"$effectiveTitle\"",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = amber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("URL", normalizedUrl))
                            Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar")
                    }

                    TextButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, effectiveTitle)
                                putExtra(Intent.EXTRA_TEXT, "$effectiveTitle\n$normalizedUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir enlace"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir")
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                metadata = WebLinkMetadataFetcher.fetchMetadata(context, normalizedUrl, forceRefresh = true)
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar metadatos", tint = amber)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    openUrlInBrowser(context, normalizedUrl)
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir en navegador", color = amber, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
