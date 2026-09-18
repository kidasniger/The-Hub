package com.thehub.hb.data.remote

import android.text.Html
import java.net.InetAddress
import java.net.URI
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
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1")
                .header("Accept-Encoding", "gzip")
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

                if (it.code == 404 || it.code == 410) {
                    return LinkPreviewData(
                        originalUrl = url,
                        finalUrl = currentUrl,
                        title = null,
                        description = null,
                        imageUrl = null,
                        siteName = null,
                        httpStatus = it.code
                    )
                }

                if (!it.isSuccessful) return null

                val contentType = it.header("Content-Type")?.lowercase(Locale.US).orEmpty()
                if (!contentType.contains("text/html") && !contentType.contains("application/xhtml+xml")) {
                    return null
                }

                val body = it.body ?: return null
                if (body.contentLength() > MAX_HTML_BYTES) return null

                val html = body.string().take(MAX_HTML_CHARS)
                return parse(url, currentUrl, html)
            }
        }

        return null
    }

    private fun parse(originalUrl: String, finalUrl: String, html: String): LinkPreviewData {
        val title = extractTitle(html)?.let(::cleanText)
        val description = extractMeta(html, "description")
            ?: extractMeta(html, "twitter:description")
        val ogTitle = extractProperty(html, "og:title")
            ?: extractProperty(html, "twitter:title")
        val image = extractProperty(html, "og:image")
            ?: extractProperty(html, "twitter:image")
        val siteName = extractProperty(html, "og:site_name") ?: hostOf(finalUrl)

        return LinkPreviewData(
            originalUrl = originalUrl,
            finalUrl = finalUrl,
            title = cleanText(ogTitle ?: title),
            description = cleanText(description),
            imageUrl = resolveUrl(finalUrl, image),
            siteName = cleanText(siteName)
        )
    }

    private fun extractTitle(html: String): String? {
        return Regex(
            pattern = """<title[^>]*>(.*?)</title>""",
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
                return attrs["content"]?.let(::cleanText)
            }
        }
        return null
    }

    private fun parseAttributes(tag: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val attrRegex = Regex(
            pattern = """([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*["']([^"']*)["']""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        for (match in attrRegex.findAll(tag)) {
            result[match.groupValues[1].lowercase(Locale.US)] = match.groupValues[2]
        }
        return result
    }

    private fun cleanText(value: String?): String? {
        val cleaned = value
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            ?.takeIf { it.isNotBlank() }
        return cleaned
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
        if (host == "localhost" || host.endsWith(".localhost") || host == "0.0.0.0") {
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
            URI(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
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
        private const val MAX_HTML_BYTES = 1_000_000L
        private const val MAX_HTML_CHARS = 750_000
        private const val MAX_REDIRECTS = 5
        private const val USER_AGENT = "TheHubLinkPreview/1.0"

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(8, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
    }
}
