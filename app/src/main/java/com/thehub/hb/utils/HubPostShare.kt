package com.thehub.hb.utils

private const val HUB_POST_SHARE_PREFIX = "thehub://post/"

fun buildHubPostShareLink(postId: String): String = HUB_POST_SHARE_PREFIX + postId

fun buildHubPostShareMessage(postId: String): String =
    "📌 Publication The Hub\n" + buildHubPostShareLink(postId)

fun extractHubPostId(text: String): String? {
    val match = Regex("""thehub://post/([^\s]+)""").find(text) ?: return null
    return match.groupValues.getOrNull(1)
        ?.trim()
        ?.trimEnd('.', ',', '!', '?', ')', ']', '}')
        ?.takeIf { it.isNotBlank() }
}
