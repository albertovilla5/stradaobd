package com.torqueobd2.dash.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.data.model.PollingMode

/** Solo recuerda adaptadores reales con los que se estableció una conexión. */
class ConnectionPreferences internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("obd_connection", Context.MODE_PRIVATE))

    var simulationEnabled: Boolean
        get() = preferences.getBoolean("simulation_enabled", false)
        set(value) { preferences.edit().putBoolean("simulation_enabled", value).apply() }

    var backgroundEnabled: Boolean
        get() = preferences.getBoolean("background_enabled", true)
        set(value) { preferences.edit().putBoolean("background_enabled", value).apply() }

    var pollingMode: PollingMode
        get() = PollingMode.entries.firstOrNull { it.name == preferences.getString("polling_mode", null) }
            ?: PollingMode.VISIBLE
        set(value) { preferences.edit().putString("polling_mode", value.name).apply() }

    fun saveLastConnection(state: ConnectionState) {
        if (state.type == ConnectionType.SIMULATION || state.activeDevice == null) return
        preferences.edit()
            .putString("last_type", state.type.name)
            .putString("last_name", state.activeDevice.name)
            .putString("last_address", state.activeDevice.address)
            .putString("wifi_host", state.wifiHost)
            .putInt("wifi_port", state.wifiPort)
            .apply()
    }

    fun lastConnection(): ConnectionState? {
        val type = when (preferences.getString("last_type", null)) {
            "BLUETOOTH" -> ConnectionType.BLUETOOTH
            "WIFI" -> ConnectionType.WIFI
            else -> return null
        }
        val address = preferences.getString("last_address", null)?.takeIf { it.isNotBlank() } ?: return null
        return ConnectionState(
            type = type,
            activeDevice = DeviceItem(preferences.getString("last_name", null) ?: "ELM327", address),
            wifiHost = preferences.getString("wifi_host", null) ?: "192.168.0.10",
            wifiPort = preferences.getInt("wifi_port", 35000)
        )
    }
}
