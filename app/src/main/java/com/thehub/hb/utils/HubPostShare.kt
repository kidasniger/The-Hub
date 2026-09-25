package com.thehub.hb.utils

private const val HUB_POST_SHARE_PREFIX = "https://the-hub-f95f4.web.app/post/"
private const val LEGACY_HUB_POST_SHARE_PREFIX = "thehub://post/"

fun buildHubPostShareLink(postId: String): String =
    HUB_POST_SHARE_PREFIX + java.net.URLEncoder.encode(postId, Charsets.UTF_8.name()).replace("+", "%20")

fun buildHubPostShareMessage(postId: String): String =
    "📌 Publication The Hub\n" + buildHubPostShareLink(postId)

fun extractHubPostId(text: String): String? {
    val match = Regex(
        """(?:https://the-hub-f95f4\.web\.app/post/|thehub://post/)([^\s?#]+)""",
        RegexOption.IGNORE_CASE
    ).find(text) ?: return null

    return match.groupValues.getOrNull(1)
        ?.trim()
        ?.trimEnd('.', ',', '!', '?', ')', ']', '}')
        ?.let {
            runCatching { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }.getOrNull()
        }
        ?.takeIf { it.isNotBlank() }
}
