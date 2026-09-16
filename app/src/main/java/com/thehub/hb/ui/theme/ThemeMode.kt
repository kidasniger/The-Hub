package com.thehub.hb.ui.theme

/**
 * Modes de thème pris en charge par The Hub :
 * - DARK : Sombre épuré (par défaut, reposant pour les yeux)
 * - LIGHT : Clair lumineux (haute lisibilité)
 * - GLASS : Effet Glassmorphism Aéro (verre dépoli moderne translucide avec reflets)
 * - SYSTEM : Automatique selon les paramètres du système
 */
enum class AppThemeMode(
    val key: String,
    val titleFr: String,
    val descriptionFr: String,
    val titleEn: String = titleFr,
    val descriptionEn: String = descriptionFr
) {
    DARK("dark", "Mode Sombre", "Design épuré et reposant pour les yeux", "Dark Mode", "Clean design, easy on the eyes"),
    LIGHT("light", "Mode Clair", "Design épuré, lumineux et contrasté", "Light Mode", "Bright, high-contrast clean design"),
    GLASS("glass", "Effet Glass", "Verre dépoli aéro avec reflets translucides modernes", "Glass Effect", "Aero frosted glass with modern translucent reflections"),
    SYSTEM("system", "Système (Automatique)", "S'adapte au mode clair ou sombre de l'appareil", "System (Automatic)", "Follows your device dark/light setting");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DARK
        }
    }
}

/**
 * Langues prises en charge dans The Hub (Français et Anglais).
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flagEmoji: String
) {
    FR("fr", "Français", "🇫🇷"),
    EN("en", "English", "🇬🇧");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: FR
        }
    }
}
