package com.thehub.hb.utils

private val URL_REGEX = Regex(
    """https?://[^\s<>"']+""",
    setOf(RegexOption.IGNORE_CASE)
)

fun extractFirstHttpUrl(text: String): String? {
    return URL_REGEX.find(text)
        ?.value
        ?.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}')
        ?.takeIf { it.length <= 2048 }
}
