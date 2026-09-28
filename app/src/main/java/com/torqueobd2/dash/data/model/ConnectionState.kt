package com.torqueobd2.dash.data.model

enum class ConnectionType {
    SIMULATION,
    BLUETOOTH,
    WIFI
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    INITIALIZING_ELM,
    CONNECTED,
    ERROR
}

data class DeviceItem(
    val name: String,
    val address: String // MAC address para BT o "IP:Puerto" para WiFi
)

data class ConnectionState(
    val type: ConnectionType = ConnectionType.SIMULATION,
    val status: ConnectionStatus = ConnectionStatus.CONNECTED,
    val activeDevice: DeviceItem? = DeviceItem("Simulador de Motor", "127.0.0.1"),
    val statusMessage: String = "Modo simulación activo",
    val elmVersion: String = "ELM327 v1.5 (Simulado)",
    val protocol: String = "ISO 15765-4 CAN (11 bit ID, 500 kbaud)",
    val wifiHost: String = "192.168.0.10",
    val wifiPort: Int = 35000,
    val availableBtDevices: List<DeviceItem> = emptyList(),
    val isScanningBt: Boolean = false,
    val commandsPerSecond: Float = 14.5f,
    val customInitCommands: List<String> = listOf("AT SP7", "AT SH DA10F1")
)
