package com.torqueobd2.dash.i18n

import java.util.Locale

/**
 * Supported languages in the application.
 *
 * Architecture note for adding new languages:
 * 1. Add the new enum entry here (e.g., `FR("fr", "Français", Locale.FRENCH)`).
 * 2. Add its `<locale android:name="fr" />` entry to `res/xml/locales_config.xml`.
 * 3. Add `res/values-fr/strings.xml` and register or extend translations in `AppI18n`.
 * The Settings UI and `AppLanguagePreference` automatically discover all entries in [AppLanguage.entries].
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val locale: Locale
) {
    ES("es", "Español", Locale("es")),
    EN("en", "English", Locale.ENGLISH),
    FR("fr", "Français", Locale.FRENCH),
    DE("de", "Deutsch", Locale.GERMAN),
    PT("pt", "Português", Locale("pt")),
    IT("it", "Italiano", Locale.ITALIAN);

    companion object {
        /** Default fallback language for international Play Store users whose system locale is not explicitly listed. */
        val INTERNATIONAL_FALLBACK: AppLanguage = EN

        fun fromCode(code: String?): AppLanguage? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
            return entries.firstOrNull { it.code == normalized }
        }

        fun fromLocale(locale: Locale): AppLanguage =
            fromCode(locale.language) ?: INTERNATIONAL_FALLBACK
    }
}

/**
 * User-selectable language preference, following the standard Google Play Store pattern:
 * - [SYSTEM]: Automatically follows the device/OS language (or Android 13+ per-app system language).
 * - [ES], [EN], [FR], [DE], [PT], [IT]: Explicit in-app language selection.
 */
enum class AppLanguagePreference(
    val code: String,
    val explicitLanguage: AppLanguage?
) {
    SYSTEM("system", null),
    ES(AppLanguage.ES.code, AppLanguage.ES),
    EN(AppLanguage.EN.code, AppLanguage.EN),
    FR(AppLanguage.FR.code, AppLanguage.FR),
    DE(AppLanguage.DE.code, AppLanguage.DE),
    PT(AppLanguage.PT.code, AppLanguage.PT),
    IT(AppLanguage.IT.code, AppLanguage.IT);

    fun resolve(systemLocale: Locale = AppI18n.getSystemLocale()): AppLanguage =
        explicitLanguage ?: AppLanguage.fromLocale(systemLocale)

    fun title(language: AppLanguage = AppI18n.currentLanguage): String = when (this) {
        SYSTEM -> tr("Automático (idioma del sistema)", "System default", language)
        ES -> AppLanguage.ES.nativeName
        EN -> AppLanguage.EN.nativeName
        FR -> AppLanguage.FR.nativeName
        DE -> AppLanguage.DE.nativeName
        PT -> AppLanguage.PT.nativeName
        IT -> AppLanguage.IT.nativeName
    }

    fun displayName(language: AppLanguage = AppI18n.currentLanguage): String = title(language)

    fun subtitle(
        systemLanguage: AppLanguage = AppLanguage.fromLocale(AppI18n.getSystemLocale()),
        language: AppLanguage = AppI18n.currentLanguage
    ): String = when (this) {
        SYSTEM -> tr(
            "Sigue el idioma del dispositivo (${systemLanguage.nativeName})",
            "Follows device language (${systemLanguage.nativeName})",
            language
        )
        ES -> tr("Interfaz, sensores y diagnósticos en español", "Interface, sensors and diagnostics in Spanish", language)
        EN -> tr("Interfaz, sensores y diagnósticos en inglés", "Interface, sensors and diagnostics in English", language)
        FR -> tr("Interfaz, sensores y diagnósticos en francés", "Interface, sensors and diagnostics in French", language)
        DE -> tr("Interfaz, sensores y diagnósticos en alemán", "Interface, sensors and diagnostics in German", language)
        PT -> tr("Interfaz, sensores y diagnósticos en portugués", "Interface, sensors and diagnostics in Portuguese", language)
        IT -> tr("Interfaz, sensores y diagnósticos en italiano", "Interface, sensors and diagnostics in Italian", language)
    }

    companion object {
        fun fromCode(code: String?): AppLanguagePreference {
            if (code.isNullOrBlank() || code.equals(SYSTEM.code, ignoreCase = true)) return SYSTEM
            val normalized = code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
            return entries.firstOrNull { it.code == normalized } ?: SYSTEM
        }

        fun forLanguage(language: AppLanguage): AppLanguagePreference =
            entries.firstOrNull { it.explicitLanguage == language } ?: SYSTEM
    }
}
