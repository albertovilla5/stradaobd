package com.torqueobd2.dash.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.torqueobd2.dash.i18n.AppLanguagePreference

/** Presentation choices are independent of vehicles, dashboards and OBD connection settings. */
class InterfacePreferences internal constructor(
    private val preferences: SharedPreferences,
    private val devicePreferences: SharedPreferences = preferences
) {
    constructor(context: Context) : this(
        preferences = context.getSharedPreferences("interface", Context.MODE_PRIVATE),
        devicePreferences = context.getSharedPreferences("device_locale_state", Context.MODE_PRIVATE)
    )

    var carMode: Boolean
        get() = preferences.getBoolean("car_mode", true)
        set(value) { preferences.edit().putBoolean("car_mode", value).apply() }

    var languagePreference: AppLanguagePreference
        get() = AppLanguagePreference.fromCode(preferences.getString("language", AppLanguagePreference.SYSTEM.code))
        set(value) { preferences.edit().putString("language", value.code).apply() }

    var lastSyncedOsLocale: String?
        get() = devicePreferences.getString("last_synced_os_locale", null)
        set(value) { devicePreferences.edit().putString("last_synced_os_locale", value).apply() }
}
