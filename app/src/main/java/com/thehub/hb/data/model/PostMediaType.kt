package com.thehub.hb.data.model

/**
 * Type de média associé à une publication.
 *
 * NONE / IMAGE / VIDEO_LINK couvrent les médias actuellement pris en charge.
 * VIDEO est réservé à la future prise en charge de l'upload vidéo natif.
 */
enum class PostMediaType {
    NONE,
    IMAGE,
    VIDEO,
    VIDEO_LINK;

    companion object {
        fun fromUrls(
            imageUrl: String?,
            videoUrl: String?
        ): PostMediaType = when {
            !videoUrl.isNullOrBlank() -> VIDEO_LINK
            !imageUrl.isNullOrBlank() -> IMAGE
            else -> NONE
        }

        fun fromStoredValue(value: String?): PostMediaType? {
            return entries.firstOrNull {
                it.name.equals(value?.trim(), ignoreCase = true)
            }
        }
    }
}
