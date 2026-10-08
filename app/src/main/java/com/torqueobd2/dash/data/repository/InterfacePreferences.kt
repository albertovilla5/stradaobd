package com.torqueobd2.dash.data.repository

import android.content.Context

/** Presentation choices are independent of vehicles, dashboards and OBD connection settings. */
class InterfacePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("interface", Context.MODE_PRIVATE)
    var carMode: Boolean
        get() = preferences.getBoolean("car_mode", true)
        set(value) { preferences.edit().putBoolean("car_mode", value).apply() }
}
