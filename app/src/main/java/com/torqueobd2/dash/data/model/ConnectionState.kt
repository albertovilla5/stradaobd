package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.tr

enum class ConnectionType {
    SIMULATION,
    BLUETOOTH,
    WIFI
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    RECONNECTING,
    INITIALIZING_ELM,
    CONNECTED,
    ERROR
}

data class DeviceItem(
    val name: String,
    val address: String, // MAC address para BT o "IP:Puerto" para WiFi
    val isPaired: Boolean = true
)

data class ConnectionState(
    val type: ConnectionType = ConnectionType.BLUETOOTH,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activeDevice: DeviceItem? = null,
    val statusMessage: String = tr(
        "Selecciona un adaptador OBD para la primera conexión",
        "Select an OBD adapter for the first connection"
    ),
    val elmVersion: String = "",
    val protocol: String = "",
    val wifiHost: String = "192.168.0.10",
    val wifiPort: Int = 35000,
    val availableBtDevices: List<DeviceItem> = emptyList(),
    val isScanningBt: Boolean = false,
    val commandsPerSecond: Float = 0f,
    val customInitCommands: List<String> = listOf("AT SP7", "AT SH DA10F1")
)
