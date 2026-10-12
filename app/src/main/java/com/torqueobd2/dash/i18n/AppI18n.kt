package com.torqueobd2.dash.i18n

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import com.torqueobd2.dash.data.repository.InterfacePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Central internationalization (i18n) coordinator for StradaOBD.
 *
 * Responsibilities:
 * - Tracks the active [AppLanguage] as both a Compose Snapshot state (for instant recomposition)
 *   and a [StateFlow] (for services, ViewModels, and OBD background workers).
 * - Synchronizes language selection with [InterfacePreferences] and Android 13+ [LocaleManager]
 *   (per-app language settings in Android System Settings, standard for Google Play Store apps),
 *   with full backward compatibility down to Android 6.0 (API 23).
 * - Provides pure-JVM defaults (`AppLanguage.ES` when uninitialized) so existing unit tests run
 *   without requiring an Android `Context`, while allowing tests to switch languages via [withLanguage].
 * - Supports extensible language packs via [registerLanguagePack] for future languages.
 */
object AppI18n {
    @android.annotation.SuppressLint("ConstantLocale")
    private val initialJvmLocale: Locale = Locale.getDefault()

    @Volatile
    private var activeLanguage: AppLanguage = AppLanguage.ES

    private var composeLanguageState by mutableStateOf(AppLanguage.ES)
    private var composePreferenceState by mutableStateOf(AppLanguagePreference.SYSTEM)

    private val _languageFlow = MutableStateFlow(AppLanguage.ES)
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    /**
     * Optional translation packs keyed by [AppLanguage], mapping either the English or Spanish
     * canonical string to the target language string. Makes adding future languages effortless.
     */
    private val languagePacks = ConcurrentHashMap<AppLanguage, Map<String, String>>().apply {
        put(AppLanguage.FR, AppTranslationsFr.translations)
        put(AppLanguage.DE, AppTranslationsDe.translations)
        put(AppLanguage.PT, AppTranslationsPt.translations)
        put(AppLanguage.IT, AppTranslationsIt.translations)
    }

    /** Current active language. Reading this inside a `@Composable` automatically subscribes to recomposition. */
    val currentLanguage: AppLanguage
        get() {
            // Read composeLanguageState so Compose Snapshot tracking registers the dependency
            val stateLang = composeLanguageState
            return stateLang
        }

    /** Current language preference (`SYSTEM`, `ES`, `EN`, `FR`, `DE`, `PT`, `IT`). Reading inside `@Composable` subscribes to recomposition. */
    val currentPreference: AppLanguagePreference
        get() = composePreferenceState

    fun setLanguage(language: AppLanguage) {
        activeLanguage = language
        _languageFlow.value = language
        Snapshot.withMutableSnapshot {
            composeLanguageState = language
        }
    }

    private fun setPreferenceState(preference: AppLanguagePreference) {
        Snapshot.withMutableSnapshot {
            composePreferenceState = preference
        }
    }

    /**
     * Temporarily executes [block] under [language] and restores the previous language afterwards.
     * Useful for unit tests and generating reports in a specific language.
     */
    fun <T> withLanguage(language: AppLanguage, block: () -> T): T {
        val previous = activeLanguage
        setLanguage(language)
        return try {
            block()
        } finally {
            setLanguage(previous)
        }
    }

    /**
     * Registers a dictionary of translations for a language.
     * Keys may be either the Spanish (`es`) or English (`en`) base strings.
     */
    fun registerLanguagePack(language: AppLanguage, translations: Map<String, String>) {
        languagePacks[language] = translations
    }

    internal fun lookupPack(language: AppLanguage, es: String, en: String): String? {
        val pack = languagePacks[language]
        if (pack != null) {
            pack["$es|||$en"]?.let { return it }
            pack[en]?.let { return it }
            pack[es]?.let { return it }
        }
        if (language != AppLanguage.ES && language != AppLanguage.EN) {
            AppTemplateTranslations.translate(language, es = es, en = en)?.let { return it }
        }
        return null
    }

    /**
     * Returns the device's true system [Locale], unaffected by in-app `Locale.setDefault(...)` overrides.
     */
    fun getSystemLocale(): Locale {
        return try {
            val sysConfig = Resources.getSystem()?.configuration
            if (sysConfig != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val locales = sysConfig.locales
                    if (!locales.isEmpty) locales[0] else initialJvmLocale
                } else {
                    @Suppress("DEPRECATION")
                    sysConfig.locale ?: initialJvmLocale
                }
            } else {
                initialJvmLocale
            }
        } catch (_: Throwable) {
            initialJvmLocale
        }
    }

    /**
     * Synchronizes [AppI18n] on app/activity startup or configuration change.
     * Respects both the in-app [InterfacePreferences] and Android 13+ per-app system settings ([LocaleManager]).
     */
    fun syncWithContext(context: Context): AppLanguage {
        val prefs = InterfacePreferences(context)
        var preference = prefs.languagePreference

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val appLocales = localeManager?.applicationLocales
                if (appLocales != null) {
                    val osTag = if (appLocales.isEmpty) {
                        AppLanguagePreference.SYSTEM.code
                    } else {
                        AppLanguage.fromLocale(appLocales[0]).code
                    }
                    val lastSynced = prefs.lastSyncedOsLocale
                    if (lastSynced != null && osTag != lastSynced) {
                        // The user changed the app language in Android 13+ System Settings
                        preference = AppLanguagePreference.fromCode(osTag)
                        prefs.languagePreference = preference
                        prefs.lastSyncedOsLocale = osTag
                    } else if (lastSynced == null && !appLocales.isEmpty) {
                        preference = AppLanguagePreference.fromCode(osTag)
                        prefs.languagePreference = preference
                        prefs.lastSyncedOsLocale = osTag
                    } else if (lastSynced == null && preference != AppLanguagePreference.SYSTEM) {
                        // Restored from Android Auto Backup onto a new device: push preference to OS LocaleManager
                        val resolvedTag = preference.resolve(getSystemLocale()).code
                        localeManager.applicationLocales = LocaleList.forLanguageTags(resolvedTag)
                        prefs.lastSyncedOsLocale = preference.code
                    }
                }
            } catch (_: Throwable) {
                // Ignore if LocaleManager is unavailable on custom head unit ROMs
            }
        }

        val resolved = preference.resolve(getSystemLocale())
        setPreferenceState(preference)
        setLanguage(resolved)
        Locale.setDefault(resolved.locale)
        return resolved
    }

    /**
     * Applies a new [AppLanguagePreference] selected by the user in Settings, persists it,
     * and syncs with Android 13+ [LocaleManager] if supported.
     */
    fun applyPreference(context: Context, preference: AppLanguagePreference): AppLanguage {
        val prefs = InterfacePreferences(context)
        prefs.languagePreference = preference
        prefs.lastSyncedOsLocale = preference.code

        val resolved = preference.resolve(getSystemLocale())
        setPreferenceState(preference)
        setLanguage(resolved)
        Locale.setDefault(resolved.locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val targetLocales = when (preference) {
                    AppLanguagePreference.SYSTEM -> LocaleList.getEmptyLocaleList()
                    else -> LocaleList.forLanguageTags(resolved.code)
                }
                if (localeManager != null && localeManager.applicationLocales != targetLocales) {
                    localeManager.applicationLocales = targetLocales
                }
            } catch (_: Throwable) {
                // Ignore on non-standard ROMs
            }
        }
        return resolved
    }

    /**
     * Returns a [Context] whose [Configuration] is localized to [language], ensuring
     * `context.getString(R.string.*)` and `stringResource(R.string.*)` resolve in [language].
     */
    fun createLocalizedContext(base: Context, language: AppLanguage = currentLanguage): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(language.locale)
        config.setLayoutDirection(language.locale)
        return base.createConfigurationContext(config)
    }
}

/**
 * Translates a string according to the active [AppLanguage].
 * When called inside a `@Composable`, automatically triggers recomposition on language change.
 */
fun tr(
    es: String,
    en: String,
    language: AppLanguage = AppI18n.currentLanguage
): String {
    AppI18n.lookupPack(language, es = es, en = en)?.let { return it }
    return when (language) {
        AppLanguage.ES -> es
        AppLanguage.EN,
        AppLanguage.FR,
        AppLanguage.DE,
        AppLanguage.PT,
        AppLanguage.IT -> en
    }
}

/**
 * Translates descriptive unit labels for UI presentation while keeping canonical `ObdPid.units`
 * intact for internal equality comparisons in `SensorTelemetryStatus`, `SensorAlertEvaluator`, and `TelemetryRecorder`.
 */
fun trUnit(
    unit: String,
    language: AppLanguage = AppI18n.currentLanguage
): String = when (language) {
    AppLanguage.ES -> unit
    AppLanguage.EN -> when (unit) {
        "códigos" -> "codes"
        "código" -> "code"
        "máscara" -> "mask"
        "ciclos" -> "cycles"
        "mg/ciclo" -> "mg/stroke"
        "estado" -> "state"
        "CV" -> "HP"
        "marcha" -> "gear"
        "pasos" -> "steps"
        else -> unit
    }
    AppLanguage.FR -> when (unit) {
        "códigos" -> "codes"
        "código" -> "code"
        "máscara" -> "masque"
        "ciclos" -> "cycles"
        "mg/ciclo" -> "mg/cycle"
        "estado" -> "état"
        "CV" -> "ch"
        "marcha" -> "rapport"
        "pasos" -> "pas"
        else -> unit
    }
    AppLanguage.DE -> when (unit) {
        "códigos" -> "Codes"
        "código" -> "Code"
        "máscara" -> "Maske"
        "ciclos" -> "Zyklen"
        "mg/ciclo" -> "mg/Hub"
        "estado" -> "Status"
        "CV" -> "PS"
        "marcha" -> "Gang"
        "pasos" -> "Schritte"
        else -> unit
    }
    AppLanguage.PT -> when (unit) {
        "códigos" -> "códigos"
        "código" -> "código"
        "máscara" -> "máscara"
        "ciclos" -> "ciclos"
        "mg/ciclo" -> "mg/ciclo"
        "estado" -> "estado"
        "CV" -> "cv"
        "marcha" -> "mudança"
        "pasos" -> "passos"
        else -> unit
    }
    AppLanguage.IT -> when (unit) {
        "códigos" -> "codici"
        "código" -> "codice"
        "máscara" -> "maschera"
        "ciclos" -> "cicli"
        "mg/ciclo" -> "mg/ciclo"
        "estado" -> "stato"
        "CV" -> "CV"
        "marcha" -> "marcia"
        "pasos" -> "passi"
        else -> unit
    }
}

val LocalAppLanguage = compositionLocalOf { AppI18n.currentLanguage }
val LocalAppLanguagePreference = compositionLocalOf { AppLanguagePreference.SYSTEM }
val LocalOnAppLanguagePreferenceChanged = staticCompositionLocalOf<(AppLanguagePreference) -> Unit> { {} }
