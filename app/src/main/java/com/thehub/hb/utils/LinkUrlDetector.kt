package com.thehub.hb.utils

import java.net.URI

private val URL_REGEX = Regex(
    """https?://[^\s<>"']+""",
    setOf(RegexOption.IGNORE_CASE)
)

fun extractFirstHttpUrl(text: String): String? {
    val candidate = URL_REGEX.find(text)
        ?.value
        ?.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}')
        ?.takeIf { it.length in 12..2048 }
        ?: return null

    return try {
        val uri = URI(candidate)
        val host = uri.host?.trim()?.lowercase()
        if (
            (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !host.isNullOrBlank() &&
            host.contains(".") &&
            uri.userInfo == null
        ) {
            candidate
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
