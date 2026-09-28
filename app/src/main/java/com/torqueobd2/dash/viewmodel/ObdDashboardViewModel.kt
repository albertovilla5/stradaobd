package com.torqueobd2.dash.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.WidgetSize
import com.torqueobd2.dash.data.parser.TorqueCsvParser
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.data.repository.VehicleProfileRepository
import com.torqueobd2.dash.obd.ObdManager
import com.torqueobd2.dash.util.BluetoothPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class ObdDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = VehicleProfileRepository(application.applicationContext)
    private val obdManager = ObdManager(viewModelScope, profileRepository.getVehicleProfile())

    val vehicleState = obdManager.vehicleState
    val connectionState = obdManager.connectionState
    val activePids = obdManager.activePids
    val vehicleProfile = obdManager.vehicleProfile
    val diagnosticLogs = obdManager.diagnosticLogs

    private val _customDashboard = MutableStateFlow(profileRepository.getCustomDashboard())
    val customDashboard: StateFlow<CustomDashboardConfig> = _customDashboard.asStateFlow()

    private val _pairedBtDevices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val pairedBtDevices: StateFlow<List<DeviceItem>> = _pairedBtDevices.asStateFlow()

    private val _bluetoothErrorHint = MutableStateFlow<String?>(null)
    val bluetoothErrorHint: StateFlow<String?> = _bluetoothErrorHint.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "Terminal OBD-II lista.",
            "Escribe un comando AT o PID (ej. ATI, 0100, 010C, 222512) y pulsa Enviar."
        )
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _csvImportNotification = MutableStateFlow<String?>(null)
    val csvImportNotification: StateFlow<String?> = _csvImportNotification.asStateFlow()

    private val _isScanningSensors = MutableStateFlow(false)
    val isScanningSensors: StateFlow<Boolean> = _isScanningSensors.asStateFlow()

    init {
        refreshPairedBtDevices()
    }

    fun clearNotification() {
        _csvImportNotification.value = null
    }

    fun logDiagnostic(msg: String) {
        obdManager.logDiagnostic(msg)
    }

    fun clearDiagnosticLogs() {
        obdManager.clearDiagnosticLogs()
    }

    fun setManualThrottle(percent: Float?) {
        obdManager.setManualThrottle(percent)
    }

    fun switchToSimulation() {
        obdManager.startSimulation()
    }

    fun connectBluetooth(device: DeviceItem) {
        obdManager.connectBluetooth(device)
    }

    fun connectWifi(host: String, port: Int) {
        obdManager.connectWifi(host, port)
    }

    fun disconnect() {
        obdManager.disconnect()
    }

    fun togglePid(pidId: String, isEnabled: Boolean) {
        obdManager.togglePid(pidId, isEnabled)
    }

    fun updateVehicleProfile(newProfile: VehicleProfile) {
        profileRepository.saveVehicleProfile(newProfile)
        obdManager.updateProfile(newProfile)
        _csvImportNotification.value = "Perfil de vehículo guardado correctamente."
    }

    fun scanSupportedSensors() {
        viewModelScope.launch {
            _isScanningSensors.value = true
            obdManager.logDiagnostic("🔍 Escaneando sensores soportados por la centralita del coche...")
            _terminalLogs.value = _terminalLogs.value + "> Escaneando sensores soportados (PIDs 0100/0120/Modo 22)..."
            val results = obdManager.scanSupportedPids()
            val supportedCount = results.count { it.value }
            val unsupportedCount = results.size - supportedCount
            _isScanningSensors.value = false
            _csvImportNotification.value = "Escaneo completado: $supportedCount sensores soportados, $unsupportedCount no disponibles."
            _terminalLogs.value = _terminalLogs.value + "< Sensores soportados por la ECU: $supportedCount de ${results.size}"
        }
    }

    // GESTIÓN DEL DASHBOARD PERSONALIZADO
    fun addCustomWidget(widget: DashboardWidget) {
        val currentWidgets = _customDashboard.value.widgets.toMutableList()
        currentWidgets.add(widget)
        val updated = _customDashboard.value.copy(widgets = currentWidgets)
        _customDashboard.value = updated
        profileRepository.saveCustomDashboard(updated)
    }

    fun removeCustomWidget(widgetId: String) {
        val currentWidgets = _customDashboard.value.widgets.filter { it.id != widgetId }
        val updated = _customDashboard.value.copy(widgets = currentWidgets)
        _customDashboard.value = updated
        profileRepository.saveCustomDashboard(updated)
    }

    fun toggleWidgetSize(widgetId: String) {
        val currentWidgets = _customDashboard.value.widgets.map {
            if (it.id == widgetId) {
                val newSize = if (it.size == WidgetSize.STANDARD) WidgetSize.FULL_WIDTH else WidgetSize.STANDARD
                it.copy(size = newSize)
            } else it
        }
        val updated = _customDashboard.value.copy(widgets = currentWidgets)
        _customDashboard.value = updated
        profileRepository.saveCustomDashboard(updated)
    }

    fun resetCustomDashboardToDefault() {
        val defaultCfg = CustomDashboardConfig()
        _customDashboard.value = defaultCfg
        profileRepository.saveCustomDashboard(defaultCfg)
        _csvImportNotification.value = "Dashboard personalizado restablecido a valores por defecto."
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedBtDevices() {
        val app = getApplication<Application>()

        // 1. Validar permisos
        if (!BluetoothPermissionHelper.hasAllPermissions(app)) {
            _bluetoothErrorHint.value = "Faltan permisos de Bluetooth para detectar dispositivos. Concede los permisos solicitados."
            _pairedBtDevices.value = emptyList()
            obdManager.logDiagnostic("⚠️ Permisos de Bluetooth pendientes de concesión por el usuario.")
            return
        }

        // 2. Obtener BluetoothAdapter de manera moderna y retrocompatible
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            _bluetoothErrorHint.value = "Este dispositivo no dispone de hardware Bluetooth."
            _pairedBtDevices.value = emptyList()
            obdManager.logDiagnostic("❌ No se detectó hardware Bluetooth en el dispositivo.")
            return
        }

        if (!adapter.isEnabled) {
            _bluetoothErrorHint.value = "El Bluetooth está desactivado. Actívalo en los Ajustes del teléfono para listar tu ELM327."
            _pairedBtDevices.value = emptyList()
            obdManager.logDiagnostic("⚠️ El adaptador Bluetooth está apagado en el sistema.")
            return
        }

        try {
            val paired = adapter.bondedDevices?.map {
                DeviceItem(name = it.name ?: "Dispositivo Bluetooth", address = it.address)
            } ?: emptyList()

            // Ordenar destacando los dispositivos con nombres típicos de OBD2
            val obdKeywords = listOf("OBD", "ELM", "VGATE", "VLINKER", "VIECA", "CARISTA", "KONNWEI", "SCAN")
            val sortedList = paired.sortedByDescending { dev ->
                val upper = dev.name.uppercase()
                obdKeywords.any { upper.contains(it) }
            }

            _pairedBtDevices.value = sortedList
            _bluetoothErrorHint.value = null

            if (sortedList.isEmpty()) {
                obdManager.logDiagnostic("ℹ️ No hay ningún dispositivo Bluetooth emparejado en el sistema. Empareja primero el ELM327 en los Ajustes de Bluetooth de Android (PIN habitual: 1234 o 0000).")
            } else {
                obdManager.logDiagnostic("📋 Dispositivos Bluetooth emparejados detectados: ${sortedList.size}")
            }
        } catch (e: SecurityException) {
            _bluetoothErrorHint.value = "Error de seguridad: Permiso de conexión Bluetooth denegado (${e.message})."
            obdManager.logDiagnostic("❌ Error de permisos al leer bondedDevices: ${e.message}")
        } catch (e: Exception) {
            _bluetoothErrorHint.value = "Error al consultar dispositivos: ${e.message}"
            obdManager.logDiagnostic("❌ Excepción al buscar dispositivos: ${e.message}")
        }
    }

    fun importCsvFromStream(inputStream: InputStream) {
        try {
            val result = TorqueCsvParser.parse(inputStream)
            if (result.pids.isNotEmpty()) {
                obdManager.addPids(result.pids)
                _csvImportNotification.value = "Se importaron exitosamente ${result.pids.size} PIDs de Torque Pro."
                if (result.initCommands.isNotEmpty()) {
                    _terminalLogs.value = _terminalLogs.value + "Comandos Init detectados en CSV: ${result.initCommands.joinToString(", ")}"
                }
            } else {
                _csvImportNotification.value = "No se encontraron PIDs válidos en el archivo CSV."
            }
        } catch (e: Exception) {
            _csvImportNotification.value = "Error al procesar el archivo CSV: ${e.message}"
        }
    }

    fun importCsvFromString(csvText: String) {
        try {
            val result = TorqueCsvParser.parseString(csvText)
            if (result.pids.isNotEmpty()) {
                obdManager.addPids(result.pids)
                _csvImportNotification.value = "Se importaron ${result.pids.size} PIDs correctamente."
            } else {
                _csvImportNotification.value = "No se detectaron filas válidas de PIDs."
            }
        } catch (e: Exception) {
            _csvImportNotification.value = "Error: ${e.message}"
        }
    }

    fun loadSampleHondaIdtecPids() {
        importCsvFromString(DefaultPids.SAMPLE_HONDA_IDTEC_CSV)
    }

    fun loadStandardPids() {
        obdManager.addPids(DefaultPids.getStandardPids())
        _csvImportNotification.value = "PIDs estándar OBD-II restaurados."
    }

    fun sendTerminalCommand(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return

        val currentList = _terminalLogs.value.toMutableList()
        currentList.add("> $cmd")
        _terminalLogs.value = currentList

        viewModelScope.launch {
            val response = obdManager.sendManualCommand(cmd)
            val updated = _terminalLogs.value.toMutableList()
            updated.add("< ${response.trim()}")
            _terminalLogs.value = updated
        }
    }
}
