package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entity.WebLinkCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class CardDisplayMode {
    LARGE,
    COMPACT,
    MINIMAL,
    HIDDEN
}

data class WebLinkMetadata(
    val url: String,
    val domain: String,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val siteName: String = "",
    val faviconUrl: String = "",
    val localFaviconPath: String? = null,
    val articleExcerpt: String = "",
    val fetchedAt: Long = System.currentTimeMillis(),
    val isLoadedFromRemote: Boolean = false
) {
    val effectiveImageModel: Any?
        get() {
            val local = localImagePath?.takeIf { it.isNotBlank() }?.let { File(it) }
            if (local != null && local.exists() && local.length() > 0L) {
                return local
            }
            return imageUrl?.takeIf { it.isNotBlank() }
        }

    val effectiveFaviconModel: Any?
        get() {
            val local = localFaviconPath?.takeIf { it.isNotBlank() }?.let { File(it) }
            if (local != null && local.exists() && local.length() > 0L) {
                return local
            }
            return faviconUrl.takeIf { it.isNotBlank() }
        }

    val hasOfflineImage: Boolean
        get() {
            val local = localImagePath?.takeIf { it.isNotBlank() }?.let { File(it) }
            return local != null && local.exists() && local.length() > 0L
        }

    val isOfflineReady: Boolean
        get() = isLoadedFromRemote || hasOfflineImage || articleExcerpt.isNotBlank()
}

data class ExtractedWebLink(
    val url: String,
    val rawMatch: String,
    val inlineTitle: String?,
    val startIndex: Int,
    val endIndex: Int
)

object WebLinkMetadataFetcher {

    private const val PREFS_NAME = "notejs_rich_links_cache"
    private const val KEY_PREFIX_META = "meta_"
    private const val KEY_PREFIX_MODE = "mode_"
    private const val CACHE_DIR_NAME = "web_link_cache"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Matches [Custom Title](https://example.com/... or www.example.com/... or domain.com/...)
    val MARKDOWN_WEB_LINK_REGEX = Regex(
        """\[([^\]]+)\]\(((?:https?://|www\.|(?:[a-zA-Z0-9-]+\.)+(?:com|org|net|edu|gov|io|dev|app|ai|co|es|mx|ar|cl|pe|me|info|tv|gg|xyz|ly|gl|be)\b)[^\s\)]*)\)""",
        RegexOption.IGNORE_CASE
    )

    // Matches standalone https://..., http://..., www...., or bare domain URLs not inside (...) of a markdown link or email
    val RAW_WEB_URL_REGEX = Regex(
        """(?<!\]\()(?<![/@\w])(?:\bhttps?://[^\s<>"{}|\\^`\[\]\)]+|\bwww\.[^\s<>"{}|\\^`\[\]\)]+|\b(?:[a-zA-Z0-9-]+\.)+(?:com|org|net|edu|gov|io|dev|app|ai|co|es|mx|ar|cl|pe|me|info|tv|gg|xyz|ly|gl|be)\b(?:/[^\s<>"{}|\\^`\[\]\)]*)?)""",
        RegexOption.IGNORE_CASE
    )

    private val memoryCache = ConcurrentHashMap<String, WebLinkMetadata>()
    private val displayModeCache = ConcurrentHashMap<String, CardDisplayMode>()
    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('.', ',', ';', ':', '!', '?')
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val normalized = normalizeUrl(url)
            val host = Uri.parse(normalized).host ?: URI(normalized).host ?: normalized
            host.removePrefix("www.").lowercase()
        } catch (_: Exception) {
            url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        }
    }

    fun defaultFaviconUrl(domain: String): String {
        val cleanDomain = domain.trim()
        return if (cleanDomain.isNotEmpty()) {
            "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128"
        } else {
            ""
        }
    }

    fun extractWebLinks(text: String): List<ExtractedWebLink> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<ExtractedWebLink>()
        val occupiedRanges = mutableListOf<IntRange>()

        for (match in MARKDOWN_WEB_LINK_REGEX.findAll(text)) {
            val inlineTitle = match.groupValues[1].trim()
            val rawUrl = match.groupValues[2].trim()
            val normalized = normalizeUrl(rawUrl)
            occupiedRanges.add(match.range)
            results.add(
                ExtractedWebLink(
                    url = normalized,
                    rawMatch = match.value,
                    inlineTitle = inlineTitle.takeIf { it.isNotEmpty() },
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        for (match in RAW_WEB_URL_REGEX.findAll(text)) {
            val range = match.range
            if (occupiedRanges.any { range.first >= it.first && range.last <= it.last }) {
                continue
            }
            val cleanedRaw = match.value.trimEnd('.', ',', ';', '!', '?', ')', ']')
            if (cleanedRaw.length < 5 || !cleanedRaw.contains('.')) continue
            val normalized = normalizeUrl(cleanedRaw)
            results.add(
                ExtractedWebLink(
                    url = normalized,
                    rawMatch = cleanedRaw,
                    inlineTitle = null,
                    startIndex = range.first,
                    endIndex = range.first + cleanedRaw.length
                )
            )
        }

        return results.sortedBy { it.startIndex }.distinctBy { it.url }
    }

    fun getDisplayMode(context: Context, url: String): CardDisplayMode {
        val normalized = normalizeUrl(url)
        displayModeCache[normalized]?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PREFIX_MODE + normalized, null)
        val mode = runCatching {
            if (saved != null) CardDisplayMode.valueOf(saved) else CardDisplayMode.LARGE
        }.getOrDefault(CardDisplayMode.LARGE)
        displayModeCache[normalized] = mode
        return mode
    }

    fun setDisplayMode(context: Context, url: String, mode: CardDisplayMode) {
        val normalized = normalizeUrl(url)
        displayModeCache[normalized] = mode
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFIX_MODE + normalized, mode.name).apply()
        ioScope.launch {
            runCatching {
                AppDatabase.getDatabase(context).webLinkCacheDao().updateDisplayMode(normalized, mode.name)
            }
        }
    }

    fun getCachedMetadata(context: Context, url: String): WebLinkMetadata? {
        val normalized = normalizeUrl(url)
        memoryCache[normalized]?.let { return it }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PREFIX_META + normalized, null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val meta = WebLinkMetadata(
                url = obj.optString("url", normalized),
                domain = obj.optString("domain", extractDomain(normalized)),
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() },
                localImagePath = obj.optString("localImagePath", "").takeIf { it.isNotBlank() },
                siteName = obj.optString("siteName", ""),
                faviconUrl = obj.optString("faviconUrl", defaultFaviconUrl(extractDomain(normalized))),
                localFaviconPath = obj.optString("localFaviconPath", "").takeIf { it.isNotBlank() },
                articleExcerpt = obj.optString("articleExcerpt", ""),
                fetchedAt = obj.optLong("fetchedAt", 0L),
                isLoadedFromRemote = obj.optBoolean("isLoadedFromRemote", true)
            )
            memoryCache[normalized] = meta
            meta
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getRoomCachedMetadata(context: Context, normalizedUrl: String): WebLinkMetadata? {
        return try {
            val entity = AppDatabase.getDatabase(context).webLinkCacheDao().getByUrl(normalizedUrl)
                ?: return null
            runCatching {
                val mode = CardDisplayMode.valueOf(entity.displayMode)
                if (!displayModeCache.containsKey(normalizedUrl)) {
                    displayModeCache[normalizedUrl] = mode
                }
            }
            val meta = WebLinkMetadata(
                url = entity.url,
                domain = entity.domain,
                title = entity.title,
                description = entity.description,
                imageUrl = entity.imageUrl,
                localImagePath = entity.localImagePath,
                siteName = entity.siteName,
                faviconUrl = entity.faviconUrl,
                localFaviconPath = entity.localFaviconPath,
                articleExcerpt = entity.articleExcerpt,
                fetchedAt = entity.fetchedAt,
                isLoadedFromRemote = entity.isLoadedFromRemote
            )
            memoryCache[normalizedUrl] = meta
            meta
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun saveMetadataToCache(context: Context, metadata: WebLinkMetadata) {
        val normalized = normalizeUrl(metadata.url)
        memoryCache[normalized] = metadata
        try {
            val obj = JSONObject().apply {
                put("url", normalized)
                put("domain", metadata.domain)
                put("title", metadata.title)
                put("description", metadata.description)
                put("imageUrl", metadata.imageUrl ?: "")
                put("localImagePath", metadata.localImagePath ?: "")
                put("siteName", metadata.siteName)
                put("faviconUrl", metadata.faviconUrl)
                put("localFaviconPath", metadata.localFaviconPath ?: "")
                put("articleExcerpt", metadata.articleExcerpt)
                put("fetchedAt", metadata.fetchedAt)
                put("isLoadedFromRemote", metadata.isLoadedFromRemote)
            }
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PREFIX_META + normalized, obj.toString()).apply()
        } catch (_: Exception) {
        }

        try {
            val currentMode = getDisplayMode(context, normalized).name
            val entity = WebLinkCacheEntity(
                url = normalized,
                domain = metadata.domain,
                title = metadata.title,
                description = metadata.description,
                imageUrl = metadata.imageUrl,
                localImagePath = metadata.localImagePath,
                siteName = metadata.siteName,
                faviconUrl = metadata.faviconUrl,
                localFaviconPath = metadata.localFaviconPath,
                articleExcerpt = metadata.articleExcerpt,
                displayMode = currentMode,
                fetchedAt = metadata.fetchedAt,
                isLoadedFromRemote = metadata.isLoadedFromRemote
            )
            AppDatabase.getDatabase(context).webLinkCacheDao().upsert(entity)
        } catch (_: Exception) {
        }
    }

    fun buildFallbackMetadata(url: String, customTitle: String? = null): WebLinkMetadata {
        val normalized = normalizeUrl(url)
        val domain = extractDomain(normalized)
        val readableTitle = customTitle?.takeIf { it.isNotBlank() } ?: run {
            val pathSegment = try {
                val path = Uri.parse(normalized).path?.trim('/') ?: ""
                if (path.isNotBlank()) {
                    path.substringAfterLast('/')
                        .replace('-', ' ')
                        .replace('_', ' ')
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else ""
            } catch (_: Exception) {
                ""
            }
            if (pathSegment.length >= 3) {
                "$pathSegment — ${domain.replaceFirstChar { it.uppercase() }}"
            } else {
                domain.replaceFirstChar { it.uppercase() }
            }
        }
        return WebLinkMetadata(
            url = normalized,
            domain = domain,
            title = readableTitle,
            description = normalized,
            imageUrl = null,
            localImagePath = null,
            siteName = domain.substringBefore('.').replaceFirstChar { it.uppercase() },
            faviconUrl = defaultFaviconUrl(domain),
            localFaviconPath = null,
            articleExcerpt = "",
            isLoadedFromRemote = false
        )
    }

    private fun hashKey(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.take(12).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toUInt().toString(16)
        }
    }

    private fun downloadAndCacheImage(
        context: Context,
        imageUrl: String,
        prefix: String,
        maxDimensionPx: Int
    ): String? {
        if (imageUrl.isBlank()) return null
        return try {
            val cacheDir = File(context.applicationContext.filesDir, CACHE_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            val fileHash = hashKey(imageUrl)
            val targetFile = File(cacheDir, "${prefix}_${fileHash}.jpg")
            if (targetFile.exists() && targetFile.length() > 128L) {
                return targetFile.absolutePath
            }

            val request = Request.Builder()
                .url(imageUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (bytes.isEmpty() || bytes.size > 6 * 1024 * 1024) return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return null

                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimensionPx * 2 || options.outHeight / sampleSize > maxDimensionPx * 2) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

                val scaled = if (decoded.width > maxDimensionPx || decoded.height > maxDimensionPx) {
                    val ratio = minOf(
                        maxDimensionPx.toFloat() / decoded.width.toFloat(),
                        maxDimensionPx.toFloat() / decoded.height.toFloat()
                    )
                    val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, w, h, true)
                } else {
                    decoded
                }

                FileOutputStream(targetFile).use { out ->
                    val format = if (prefix == "fav") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    val quality = if (prefix == "fav") 100 else 84
                    scaled.compress(format, quality, out)
                    out.flush()
                }
                if (scaled !== decoded) {
                    scaled.recycle()
                }
                decoded.recycle()

                if (targetFile.exists() && targetFile.length() > 0L) targetFile.absolutePath else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMetadata(
        context: Context,
        rawUrl: String,
        forceRefresh: Boolean = false
    ): WebLinkMetadata = withContext(Dispatchers.IO) {
        val normalized = normalizeUrl(rawUrl)
        if (!forceRefresh) {
            getCachedMetadata(context, normalized)?.let { cached ->
                if (cached.isLoadedFromRemote) return@withContext cached
            }
            getRoomCachedMetadata(context, normalized)?.let { roomCached ->
                if (roomCached.isLoadedFromRemote) return@withContext roomCached
            }
        }

        val mutex = fetchMutexes.getOrPut(normalized) { Mutex() }
        mutex.withLock {
            if (!forceRefresh) {
                getCachedMetadata(context, normalized)?.let { cached ->
                    if (cached.isLoadedFromRemote) return@withLock cached
                }
                getRoomCachedMetadata(context, normalized)?.let { roomCached ->
                    if (roomCached.isLoadedFromRemote) return@withLock roomCached
                }
            }

            val existingCached = getRoomCachedMetadata(context, normalized) ?: getCachedMetadata(context, normalized)
            val domain = extractDomain(normalized)
            val fallback = existingCached ?: buildFallbackMetadata(normalized)

            try {
                val request = Request.Builder()
                    .url(normalized)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        saveMetadataToCache(context, fallback)
                        return@withLock fallback
                    }

                    val contentType = response.header("Content-Type")?.lowercase() ?: ""
                    if (contentType.isNotEmpty() && !contentType.contains("html") && !contentType.contains("xml")) {
                        saveMetadataToCache(context, fallback)
                        return@withLock fallback
                    }

                    val bodyStream = response.body?.byteStream()
                    if (bodyStream == null) {
                        saveMetadataToCache(context, fallback)
                        return@withLock fallback
                    }

                    // Read up to 160 KB to capture <head> Open Graph tags AND initial article <p> paragraphs for Web Clipping
                    val buffer = ByteArray(160 * 1024)
                    var totalRead = 0
                    while (totalRead < buffer.size) {
                        val read = bodyStream.read(buffer, totalRead, buffer.size - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }

                    val html = String(buffer, 0, totalRead, Charsets.UTF_8)
                    val parsed = parseHtmlMetadata(normalized, domain, html)

                    // Download and cache thumbnail and favicon locally for 100% offline display & PDF embedding
                    val localImage = parsed.imageUrl?.let { imgUrl ->
                        downloadAndCacheImage(context, imgUrl, prefix = "img", maxDimensionPx = 720)
                    } ?: existingCached?.localImagePath

                    val localFavicon = parsed.faviconUrl.takeIf { it.isNotBlank() }?.let { favUrl ->
                        downloadAndCacheImage(context, favUrl, prefix = "fav", maxDimensionPx = 96)
                    } ?: existingCached?.localFaviconPath

                    val enriched = parsed.copy(
                        localImagePath = localImage,
                        localFaviconPath = localFavicon
                    )
                    saveMetadataToCache(context, enriched)
                    return@withLock enriched
                }
            } catch (_: Exception) {
                saveMetadataToCache(context, fallback)
                return@withLock fallback
            }
        }
    }

    internal fun parseHtmlMetadata(url: String, domain: String, html: String): WebLinkMetadata {
        val ogTitle = extractMetaContent(html, "og:title")
            ?: extractMetaContent(html, "twitter:title")
            ?: extractHtmlTitle(html)

        val ogDescription = extractMetaContent(html, "og:description")
            ?: extractMetaContent(html, "twitter:description")
            ?: extractMetaContent(html, "description")

        val rawImage = extractMetaContent(html, "og:image")
            ?: extractMetaContent(html, "og:image:url")
            ?: extractMetaContent(html, "twitter:image")
            ?: extractMetaContent(html, "twitter:image:src")

        val resolvedImage = rawImage?.let { resolveRelativeUrl(url, it) }?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }

        val siteName = extractMetaContent(html, "og:site_name")
            ?.takeIf { it.isNotBlank() }
            ?: domain.substringBefore('.').replaceFirstChar { it.uppercase() }

        val rawIcon = extractIconHref(html)
        val resolvedFavicon = rawIcon?.let { resolveRelativeUrl(url, it) }?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        } ?: defaultFaviconUrl(domain)

        val cleanTitle = decodeHtmlEntities(ogTitle ?: "").trim()
            .takeIf { it.isNotBlank() }
            ?: buildFallbackMetadata(url).title

        val cleanDescription = decodeHtmlEntities(ogDescription ?: "").trim()
        val articleExcerpt = extractArticleExcerpt(html, cleanDescription)

        return WebLinkMetadata(
            url = url,
            domain = domain,
            title = cleanTitle,
            description = cleanDescription,
            imageUrl = resolvedImage,
            siteName = decodeHtmlEntities(siteName).trim(),
            faviconUrl = resolvedFavicon,
            articleExcerpt = articleExcerpt,
            isLoadedFromRemote = true
        )
    }

    internal fun extractArticleExcerpt(html: String, fallbackDescription: String): String {
        return try {
            // Strip scripts, styles, nav, header, footer to isolate readable article paragraphs
            val bodyPart = html.substringAfter("<body", html)
                .replace(Regex("""<(script|style|nav|header|footer|noscript|svg|aside)[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")

            val paragraphRegex = Regex("""<p[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val paragraphs = paragraphRegex.findAll(bodyPart)
                .map { match ->
                    val stripped = match.groupValues[1]
                        .replace(Regex("""<[^>]+>"""), " ")
                        .replace(Regex("""\s+"""), " ")
                    decodeHtmlEntities(stripped).trim()
                }
                .filter { p ->
                    p.length >= 45 &&
                        !p.startsWith("Cookie", ignoreCase = true) &&
                        !p.contains("Aceptar cookies", ignoreCase = true) &&
                        !p.contains("All rights reserved", ignoreCase = true)
                }
                .take(3)
                .toList()

            if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n").take(520).trim()
            } else {
                fallbackDescription
            }
        } catch (_: Exception) {
            fallbackDescription
        }
    }

    private fun extractMetaContent(html: String, key: String): String? {
        val escapedKey = Regex.escape(key)
        // Pattern 1: property="og:title" content="..." or name="description" content="..."
        val pattern1 = Regex(
            """<meta[^>]+(?:property|name|itemprop)\s*=\s*["']$escapedKey["'][^>]+content\s*=\s*["']([^"']+)["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        pattern1.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { return it }

        // Pattern 2: content="..." property="og:title"
        val pattern2 = Regex(
            """<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+(?:property|name|itemprop)\s*=\s*["']$escapedKey["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        return pattern2.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractHtmlTitle(html: String): String? {
        val titleRegex = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return titleRegex.find(html)?.groupValues?.getOrNull(1)?.replace(Regex("""\s+"""), " ")?.trim()
    }

    private fun extractIconHref(html: String): String? {
        val iconRegex = Regex(
            """<link[^>]+rel\s*=\s*["'](?:shortcut\s+icon|icon|apple-touch-icon)["'][^>]+href\s*=\s*["']([^"']+)["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        iconRegex.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { return it }

        val iconRegex2 = Regex(
            """<link[^>]+href\s*=\s*["']([^"']+)["'][^>]+rel\s*=\s*["'](?:shortcut\s+icon|icon|apple-touch-icon)["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        return iconRegex2.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun resolveRelativeUrl(baseUrl: String, candidateUrl: String): String {
        val trimmed = decodeHtmlEntities(candidateUrl.trim())
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        return try {
            URI(baseUrl).resolve(trimmed).toString()
        } catch (_: Exception) {
            trimmed
        }
    }

    internal fun decodeHtmlEntities(input: String): String {
        if (input.isEmpty() || !input.contains('&')) return input
        var text = input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&aacute;", "á")
            .replace("&eacute;", "é")
            .replace("&iacute;", "í")
            .replace("&oacute;", "ó")
            .replace("&uacute;", "ú")
            .replace("&Aacute;", "Á")
            .replace("&Eacute;", "É")
            .replace("&Iacute;", "Í")
            .replace("&Oacute;", "Ó")
            .replace("&Uacute;", "Ú")
            .replace("&ntilde;", "ñ")
            .replace("&Ntilde;", "Ñ")

        // Numeric decimal entities &#1234;
        text = Regex("""&#(\d+);""").replace(text) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 32..0x10FFFF) String(Character.toChars(code)) else match.value
        }
        // Numeric hex entities &#x1F600;
        text = Regex("""&#x([0-9a-fA-F]+);""").replace(text) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 32..0x10FFFF) String(Character.toChars(code)) else match.value
        }
        return text
    }
}
