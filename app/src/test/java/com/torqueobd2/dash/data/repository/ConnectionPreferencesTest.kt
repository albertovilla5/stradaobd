package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class ConnectionPreferencesTest {
    // Sustituto en memoria del almacenamiento Android para comprobar lo escrito y reabrirlo.
    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }
                method.name == "apply" -> null
                method.name == "commit" -> true
                else -> error("Operación no esperada: ${method.name}")
            }
        }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getBoolean", "getString", "getInt" -> values[args!![0]] ?: args[1]
                "edit" -> editor
                else -> error("Operación no esperada: ${method.name}")
            }
        } as SharedPreferences
    }

    @Test
    fun firstInstallationHasSimulationDisabledAndNoRememberedDevice() {
        val settings = ConnectionPreferences(preferences())
        assertFalse(settings.simulationEnabled)
        assertNull(settings.lastConnection())
        assertEquals(PollingMode.VISIBLE, settings.pollingMode)
    }

    @Test
    fun pollingChoicePersistsAndUnknownValueUsesVisibleMode() {
        val storage = preferences()
        ConnectionPreferences(storage).pollingMode = PollingMode.BALANCED
        assertEquals(PollingMode.BALANCED, ConnectionPreferences(storage).pollingMode)
        ConnectionPreferences(storage).pollingMode = PollingMode.ALL
        assertEquals(PollingMode.ALL, ConnectionPreferences(storage).pollingMode)
        storage.edit().putString("polling_mode", "unknown").apply()
        assertEquals(PollingMode.VISIBLE, ConnectionPreferences(storage).pollingMode)
    }

    @Test
    fun reopensLastBluetoothDeviceAndExplicitSimulationChoice() {
        val storage = preferences()
        val settings = ConnectionPreferences(storage)
        val device = DeviceItem("Mi OBD", "AA:BB:CC:DD:EE:FF")
        settings.saveLastConnection(ConnectionState(type = ConnectionType.BLUETOOTH, activeDevice = device))
        settings.simulationEnabled = true
        val reopened = ConnectionPreferences(storage)
        assertTrue(reopened.simulationEnabled)
        assertEquals(device, reopened.lastConnection()!!.activeDevice)
        assertEquals(ConnectionType.BLUETOOTH, reopened.lastConnection()!!.type)
        reopened.simulationEnabled = false
        assertFalse(ConnectionPreferences(storage).simulationEnabled)
    }

    @Test
    fun savesWifiEndpointAndSimulationDoesNotReplaceIt() {
        val storage = preferences()
        val settings = ConnectionPreferences(storage)
        settings.saveLastConnection(ConnectionState(
            type = ConnectionType.WIFI, activeDevice = DeviceItem("ELM WiFi", "192.168.0.30:12345"),
            wifiHost = "192.168.0.30", wifiPort = 12345
        ))
        settings.saveLastConnection(ConnectionState(type = ConnectionType.SIMULATION, activeDevice = DeviceItem("Sim", "local")))
        val reopened = ConnectionPreferences(storage).lastConnection()!!
        assertEquals(ConnectionType.WIFI, reopened.type)
        assertEquals("192.168.0.30", reopened.wifiHost)
        assertEquals(12345, reopened.wifiPort)
    }
}
