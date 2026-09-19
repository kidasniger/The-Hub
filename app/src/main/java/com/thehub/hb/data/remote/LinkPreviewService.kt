package com.thehub.hb.data.remote

import android.text.Html
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

data class LinkPreviewData(
    val originalUrl: String,
    val finalUrl: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?,
    val httpStatus: Int = 200
)

class LinkPreviewService(
    private val client: OkHttpClient = defaultClient()
) {
    private val cache = ConcurrentHashMap<String, CachedPreview>()

    suspend fun preview(url: String, force: Boolean = false): LinkPreviewData? {
        val normalized = normalizeUrl(url) ?: return null
        val now = System.currentTimeMillis()

        if (!force) {
            cache[normalized]?.let { cached ->
                if (now - cached.cachedAtMillis < CACHE_TTL_MILLIS) {
                    return cached.data
                }
            }
        }

        val data = fetch(normalized)
        cache[normalized] = CachedPreview(now, data)
        return data
    }

    private fun fetch(url: String): LinkPreviewData? {
        var currentUrl = url

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            if (!isSafeRemoteHost(currentUrl)) return null

            val request = Request.Builder()
                .url(currentUrl)
                .get()
                .header("User-Agent", USER_AGENT)
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )
                .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (_: Exception) {
                return null
            }

            response.use {
                if (it.code in 300..399) {
                    val location = it.header("Location") ?: return null
                    currentUrl = resolveUrl(currentUrl, location) ?: return null
                    if (redirectCount == MAX_REDIRECTS) return null
                    return@repeat
                }

                if (it.code == 401 || it.code == 403 || it.code == 429) {
                    return fallbackData(url, currentUrl, it.code)
                }

                if (it.code == 404 || it.code == 410) {
                    return fallbackData(url, currentUrl, it.code)
                }

                if (!it.isSuccessful) return null

                val contentType = it.header("Content-Type")?.lowercase(Locale.US).orEmpty()
                val looksLikeHtml = contentType.isBlank() ||
                    contentType.contains("text/html") ||
                    contentType.contains("application/xhtml+xml") ||
                    contentType.contains("application/xml")

                if (!looksLikeHtml) return null

                val body = it.body ?: return null
                if (body.contentLength() > MAX_HTML_BYTES) return null

                val bytes = try {
                    body.source().readByteArray(MAX_HTML_BYTES)
                } catch (_: Exception) {
                    return null
                }

                val html = String(bytes, StandardCharsets.UTF_8)
                    .take(MAX_HTML_CHARS)

                return parse(url, currentUrl, html)
            }
        }

        return null
    }

    private fun fallbackData(
        originalUrl: String,
        finalUrl: String,
        status: Int
    ): LinkPreviewData {
        return LinkPreviewData(
            originalUrl = originalUrl,
            finalUrl = finalUrl,
            title = null,
            description = null,
            imageUrl = null,
            siteName = hostOf(finalUrl),
            httpStatus = status
        )
    }

    private fun parse(originalUrl: String, finalUrl: String, html: String): LinkPreviewData {
        val ogTitle = extractProperty(html, "og:title")
            ?: extractProperty(html, "twitter:title")

        val title = firstNonBlank(
            ogTitle,
            extractJsonLdString(html, "headline"),
            extractJsonLdString(html, "name"),
            extractTitle(html)
        )

        val description = firstNonBlank(
            extractProperty(html, "og:description"),
            extractMeta(html, "description"),
            extractProperty(html, "twitter:description"),
            extractJsonLdString(html, "description")
        )

        val image = firstNonBlank(
            extractProperty(html, "og:image"),
            extractProperty(html, "og:image:url"),
            extractProperty(html, "og:image:secure_url"),
            extractProperty(html, "twitter:image"),
            extractProperty(html, "twitter:image:src"),
            extractLinkHref(html, "image_src"),
            extractJsonLdString(html, "thumbnailUrl"),
            extractJsonLdString(html, "image")
        )

        val canonical = extractLinkHref(html, "canonical")

        return LinkPreviewData(
            originalUrl = originalUrl,
            finalUrl = finalUrl,
            title = cleanText(title),
            description = cleanText(description),
            imageUrl = resolveUrl(finalUrl, image),
            siteName = cleanText(
                firstNonBlank(
                    extractProperty(html, "og:site_name"),
                    extractJsonLdString(html, "publisher"),
                    hostOf(canonical ?: finalUrl)
                )
            )
        )
    }

    private fun extractTitle(html: String): String? {
        return Regex(
            pattern = """<title\b[^>]*>(.*?)</title\s*>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)?.groupValues?.getOrNull(1)
    }

    private fun extractMeta(html: String, name: String): String? {
        return extractMetaTag(html) { attrs ->
            attrs["name"]?.equals(name, ignoreCase = true) == true
        }
    }

    private fun extractProperty(html: String, property: String): String? {
        return extractMetaTag(html) { attrs ->
            attrs["property"]?.equals(property, ignoreCase = true) == true ||
                attrs["name"]?.equals(property, ignoreCase = true) == true
        }
    }

    private fun extractMetaTag(
        html: String,
        predicate: (Map<String, String>) -> Boolean
    ): String? {
        val tagRegex = Regex(
            pattern = """<meta\b[^>]*>""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        for (tag in tagRegex.findAll(html)) {
            val attrs = parseAttributes(tag.value)
            if (predicate(attrs)) {
                return attrs["content"]
            }
        }
        return null
    }

    private fun extractLinkHref(html: String, rel: String): String? {
        val tagRegex = Regex(
            pattern = """<link\b[^>]*>""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        for (tag in tagRegex.findAll(html)) {
            val attrs = parseAttributes(tag.value)
            val relValue = attrs["rel"].orEmpty()
            if (relValue.split(Regex("""\s+"""))
                    .any { it.equals(rel, ignoreCase = true) }
            ) {
                return attrs["href"]
            }
        }
        return null
    }

    private fun extractJsonLdString(html: String, key: String): String? {
        val scripts = Regex(
            pattern = """<script\b[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script\s*>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val valueRegex = Regex(
            pattern = """["']${java.util.regex.Pattern.quote(key)}["']\s*:\s*["']((?:\\.|[^"'])+)["']""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        for (script in scripts.findAll(html)) {
            valueRegex.find(script.groupValues[1])?.groupValues?.getOrNull(1)?.let { raw ->
                return decodeJsonLikeString(raw)
            }

            if (key == "image" || key == "thumbnailUrl") {
                val nestedUrl = Regex(
                    pattern = """["']url["']\s*:\s*["']((?:\\.|[^"'])+)["']""",
                    options = setOf(RegexOption.IGNORE_CASE)
                ).find(script.groupValues[1])?.groupValues?.getOrNull(1)
                if (nestedUrl != null) return decodeJsonLikeString(nestedUrl)
            }
        }

        return null
    }

    private fun decodeJsonLikeString(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\"", """)
            .replace("\\\\", "\\")
    }

    private fun parseAttributes(tag: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val attrRegex = Regex(
            pattern = """([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        for (match in attrRegex.findAll(tag)) {
            val value = match.groupValues[2]
                .ifBlank { match.groupValues[3] }
                .ifBlank { match.groupValues[4] }

            result[match.groupValues[1].lowercase(Locale.US)] = value
        }

        return result
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun cleanText(value: String?): String? {
        return value
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            ?.takeIf { it.isNotBlank() }
    }

    private fun resolveUrl(base: String, target: String?): String? {
        if (target.isNullOrBlank()) return null
        return try {
            URI(base).resolve(target).toString().takeIf { isHttpUrl(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeUrl(raw: String): String? {
        val value = raw.trim()
        if (!isHttpUrl(value)) return null
        return try {
            URI(value).normalize().toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun isHttpUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase(Locale.US)
            (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
                !host.isNullOrBlank() &&
                uri.userInfo == null
        } catch (_: Exception) {
            false
        }
    }

    private fun isSafeRemoteHost(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host?.lowercase(Locale.US) ?: return false
        if (
            host == "localhost" ||
            host.endsWith(".localhost") ||
            host == "0.0.0.0" ||
            host == "::1"
        ) {
            return false
        }

        return try {
            val addresses = InetAddress.getAllByName(host)
            addresses.none { address ->
                address.isLoopbackAddress ||
                    address.isLinkLocalAddress ||
                    address.isSiteLocalAddress ||
                    address.isAnyLocalAddress
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun hostOf(url: String): String? {
        return try {
            URI(url).host
                ?.removePrefix("www.")
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private data class CachedPreview(
        val cachedAtMillis: Long,
        val data: LinkPreviewData?
    )

    companion object {
        private const val CACHE_TTL_MILLIS = 10 * 60 * 1000L
        private const val MAX_HTML_BYTES = 2_000_000L
        private const val MAX_HTML_CHARS = 1_500_000
        private const val MAX_REDIRECTS = 8
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/136.0.0.0 Mobile Safari/537.36"

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .callTimeout(12, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
    }
}
