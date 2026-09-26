package com.thehub.hb.ui.theme

/**
 * Modes de thème pris en charge par The Hub.
 * Le mode Système suit le réglage clair/sombre du téléphone.
 */
enum class AppThemeMode(
    val key: String,
    val titleFr: String,
    val descriptionFr: String
) {
    DARK("dark", "Mode Sombre", "Fond profond, contraste confortable et interface sobre"),
    LIGHT("light", "Mode Clair", "Fond lumineux, cartes nettes et texte très lisible"),
    GLASS("glass", "Effet Glass", "Surfaces translucides, reflets et profondeur en verre"),
    SYSTEM("system", "Système", "Suit automatiquement le thème de ton téléphone");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}
