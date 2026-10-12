package com.torqueobd2.dash.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.torqueobd2.dash.data.model.CustomDashboardTabs
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardGridLayout
import com.torqueobd2.dash.data.model.DashboardPreset
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PollingMode
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.WidgetSize
import com.torqueobd2.dash.data.model.bindPids
import com.torqueobd2.dash.data.model.bindPid
import com.torqueobd2.dash.data.model.resolvePid
import com.torqueobd2.dash.data.model.DashboardPresets
import com.torqueobd2.dash.data.repository.SensorAlertRepository
import com.torqueobd2.dash.data.model.SensorAlertRule
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import com.torqueobd2.dash.data.parser.TorqueCsvParser
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.data.repository.VehicleProfileRepository
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.util.BluetoothPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class ObdDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = VehicleProfileRepository(application.applicationContext)
    private val alertRepository = SensorAlertRepository(application.getSharedPreferences("sensor_alerts", Context.MODE_PRIVATE))
    private var savedAlertRules = alertRepository.load()
    private val runtime = (application as com.torqueobd2.dash.TorqueApplication).obdRuntime
    private val _simulationEnabled = runtime.simulationEnabled
    val simulationEnabled = _simulationEnabled.asStateFlow()
    private val _pollingMode = runtime.pollingMode
    val pollingMode = _pollingMode.asStateFlow()
    private val _isScanningBluetooth = runtime.scanningBluetooth
    val isScanningBluetooth = _isScanningBluetooth.asStateFlow()
    private val discoveredDevices = linkedMapOf<String, DeviceItem>()
    private val obdManager = runtime.manager
    val recordingState = runtime.recording
    val recordingBusy = runtime.busy
    val backgroundEnabled = runtime.backgroundEnabled
    val backgroundServiceRunning = runtime.serviceRunning
    val recordingError = runtime.error
    fun setBackgroundEnabled(enabled: Boolean) = runtime.setBackgroundEnabled(enabled)
    fun startRecording() = runtime.startRecording()
    fun stopRecording() = runtime.stopRecording()
    fun deleteRecording(id: String) = runtime.deleteRecording(id)
    suspend fun exportRecording(id: String, output: java.io.OutputStream) = runtime.exportRecording(id, output)
    fun setUiVisible(owner: Any, visible: Boolean) {
        if (visible) refreshPairedBtDevices()
        runtime.setUiVisible(owner, visible)
    }

    val vehicleState = obdManager.vehicleState
    val connectionState = obdManager.connectionState
    val activePids = obdManager.activePids
    val vehicleProfile = obdManager.vehicleProfile
    val sensorAlerts = obdManager.sensorAlerts
    val diagnosticLogs = obdManager.diagnosticLogs
    val dtcState = obdManager.dtcState
    private val _dtcCatalog = MutableStateFlow(com.torqueobd2.dash.data.repository.DtcCatalog(emptyMap()))
    val dtcCatalog = _dtcCatalog.asStateFlow()
    fun readTroubleCodes() = obdManager.readTroubleCodes()
    fun clearTroubleCodes(session: Long) = obdManager.clearTroubleCodes(session, true)
    fun cancelDiagnostics() = obdManager.cancelDiagnostics()

    private val _customDashboards = MutableStateFlow(profileRepository.getCustomDashboardTabs())
    val customDashboards: StateFlow<CustomDashboardTabs> = _customDashboards.asStateFlow()
    private val interfacePreferences = com.torqueobd2.dash.data.repository.InterfacePreferences(application)
    private val currentDashboard: CustomDashboardConfig get() = _customDashboards.value.selectedDashboard.let {
        if (interfacePreferences.carMode) it.forCar() else it
    }

    private val _savedVehicles = MutableStateFlow<List<VehicleProfile>>(profileRepository.getAllVehicles())
    val savedVehicles: StateFlow<List<VehicleProfile>> = _savedVehicles.asStateFlow()

    private val _pairedBtDevices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val pairedBtDevices: StateFlow<List<DeviceItem>> = _pairedBtDevices.asStateFlow()

    private val _bluetoothErrorHint = MutableStateFlow<String?>(null)
    val bluetoothErrorHint: StateFlow<String?> = _bluetoothErrorHint.asStateFlow()

    private fun defaultTerminalGreeting(): List<String> = listOf(
        tr("Terminal OBD-II lista.", "OBD-II terminal ready."),
        tr(
            "Escribe un comando AT o PID (ej. ATI, 0100, 010C, 222512) y pulsa Enviar.",
            "Type an AT or PID command (e.g. ATI, 0100, 010C, 222512) and tap Send."
        )
    )

    private val _terminalLogs = MutableStateFlow<List<String>>(defaultTerminalGreeting())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _csvImportNotification = MutableStateFlow<String?>(null)
    val csvImportNotification: StateFlow<String?> = _csvImportNotification.asStateFlow()

    private val _isScanningSensors = MutableStateFlow(false)
    val isScanningSensors: StateFlow<Boolean> = _isScanningSensors.asStateFlow()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isScanningBluetooth.value = false
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF &&
                        !BluetoothPermissionHelper.isBluetoothEnabled()
                    ) {
                        _isScanningBluetooth.value = false
                        discoveredDevices.clear()
                    }
                    refreshPairedBtDevices()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    if (!BluetoothPermissionHelper.hasAllPermissions(context)) return
                    val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
                    try {
                        discoveredDevices[device.address] = DeviceItem(
                            device.name ?: tr("Dispositivo Bluetooth", "Bluetooth Device"),
                            device.address,
                            device.bondState == BluetoothDevice.BOND_BONDED
                        )
                        refreshPairedBtDevices(log = false)
                    } catch (_: SecurityException) { }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshPairedBtDevices()
            }
        }
    }

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _dtcCatalog.value = com.torqueobd2.dash.data.repository.DtcCatalog.read(application.assets.open("dtc/generic.tsv").reader())
        }
        viewModelScope.launch {
            AppI18n.languageFlow.drop(1).collect {
                if (_terminalLogs.value.size == 2 && _terminalLogs.value.none { it.startsWith(">") || it.startsWith("<") }) {
                    _terminalLogs.value = defaultTerminalGreeting()
                }
                refreshPairedBtDevices(log = false)
            }
        }
        saveDashboardTabs(_customDashboards.value.bindPids(obdManager.activePids.value))
        // Se escriben solo cambios de definición/selección, no cada lectura o escaneo.
        viewModelScope.launch {
            obdManager.activePids.map { pids -> pids.map { it.copy(isSupported = true, supportChecked = false) } }
                .distinctUntilChanged().collect { pids ->
                    val bound = _customDashboards.value.bindPids(pids)
                    if (bound != _customDashboards.value) saveDashboardTabs(bound)
                }
        }
        obdManager.logDiagnostic(com.torqueobd2.dash.util.DeviceDiagnostics.describe(application))
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(application, bluetoothReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
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

    fun setVisibleSensors(pidIds: Set<String>) { obdManager.setVisibleSensors(pidIds) }

    fun setPollingMode(mode: PollingMode) = runtime.setPollingMode(mode)

    fun setSimulationEnabled(enabled: Boolean) {
        stopBluetoothDiscovery()
        runtime.setSimulationEnabled(enabled)
    }

    fun connectBluetooth(device: DeviceItem) {
        stopBluetoothDiscovery()
        runtime.connectBluetooth(device)
    }

    fun connectWifi(host: String, port: Int) {
        if (host.isBlank() || port !in 1..65535) return
        stopBluetoothDiscovery()
        runtime.connectWifi(host, port)
    }

    fun disconnect() {
        stopBluetoothDiscovery()
        runtime.stopEverything()
    }

    @SuppressLint("MissingPermission")
    fun scanBluetoothDevices() {
        refreshPairedBtDevices()
        if (!BluetoothPermissionHelper.hasScanPermissions(getApplication())) {
            _bluetoothErrorHint.value = tr(
                "Concede los permisos de búsqueda de Bluetooth para ver dispositivos cercanos.",
                "Grant Bluetooth scan permissions to discover nearby devices."
            )
            return
        }
        try {
            val app = getApplication<Application>()
            @Suppress("DEPRECATION")
            val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter?.isEnabled != true) return
            stopBluetoothDiscovery()
            discoveredDevices.clear()
            refreshPairedBtDevices(log = false)
            _isScanningBluetooth.value = true
            if (!adapter.startDiscovery()) {
                _isScanningBluetooth.value = false
                _bluetoothErrorHint.value = tr(
                    "Android no pudo iniciar la búsqueda. Comprueba Bluetooth y, en Android 6–11, la ubicación del sistema.",
                    "Android could not start discovery. Check Bluetooth and, on Android 6–11, system Location."
                )
            }
        } catch (e: Exception) {
            _isScanningBluetooth.value = false
            _bluetoothErrorHint.value = tr(
                "No se pudo buscar dispositivos: ${e.message}",
                "Could not scan for devices: ${e.message}"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBluetoothDiscovery() {
        try {
            val app = getApplication<Application>()
            if (BluetoothPermissionHelper.hasScanPermissions(app)) {
                @Suppress("DEPRECATION")
                val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    ?: BluetoothAdapter.getDefaultAdapter()
                adapter?.cancelDiscovery()
            }
        } catch (_: Exception) { }
        _isScanningBluetooth.value = false
    }

    override fun onCleared() {
        stopBluetoothDiscovery()
        getApplication<Application>().unregisterReceiver(bluetoothReceiver)
        super.onCleared()
    }

    fun togglePid(pidId: String, isEnabled: Boolean) {
        obdManager.togglePid(pidId, isEnabled)
    }

    fun saveSensorAlert(rule: SensorAlertRule) {
        require(rule.vehicleId == vehicleProfile.value.id && rule.validationError() == null)
        savedAlertRules = if (savedAlertRules.any { it.id == rule.id }) {
            savedAlertRules.map { if (it.id == rule.id) rule else it }
        } else savedAlertRules + rule
        persistSensorAlerts()
    }

    fun setSensorAlertEnabled(id: String, enabled: Boolean) {
        savedAlertRules = savedAlertRules.map {
            if (it.id == id && it.vehicleId == vehicleProfile.value.id) it.copy(enabled = enabled) else it
        }
        persistSensorAlerts()
    }

    fun deleteSensorAlert(id: String) {
        savedAlertRules = savedAlertRules.filterNot { it.id == id && it.vehicleId == vehicleProfile.value.id }
        persistSensorAlerts()
    }

    private fun persistSensorAlerts() {
        alertRepository.save(savedAlertRules)
        obdManager.setSensorAlertRules(savedAlertRules)
    }

    fun createOrUpdateVehicle(profile: VehicleProfile) {
        profileRepository.saveVehicle(profile)
        val updatedList = profileRepository.getAllVehicles()
        _savedVehicles.value = updatedList
        val active = profileRepository.getActiveVehicle()
        runtime.updateProfile(active)
        _csvImportNotification.value = tr(
            "Vehículo '${profile.name}' guardado correctamente.",
            "Vehicle '${profile.name}' saved successfully."
        )
    }

    fun setActiveVehicle(vehicleId: String) {
        val active = profileRepository.setActiveVehicle(vehicleId)
        _savedVehicles.value = profileRepository.getAllVehicles()
        runtime.updateProfile(active)
        _csvImportNotification.value = tr(
            "Vehículo activo cambiado a '${active.name}'.",
            "Active vehicle switched to '${active.name}'."
        )
    }

    fun deleteVehicle(vehicleId: String) {
        val ok = profileRepository.deleteVehicle(vehicleId)
        if (ok) {
            savedAlertRules = savedAlertRules.filterNot { it.vehicleId == vehicleId }
            persistSensorAlerts()
            _savedVehicles.value = profileRepository.getAllVehicles()
            val active = profileRepository.getActiveVehicle()
            runtime.updateProfile(active)
            _csvImportNotification.value = tr("Vehículo eliminado.", "Vehicle deleted.")
        } else {
            _csvImportNotification.value = tr(
                "No se puede eliminar el único vehículo del garaje.",
                "Cannot delete the only vehicle in the garage."
            )
        }
    }

    fun updateVehicleProfile(newProfile: VehicleProfile) {
        createOrUpdateVehicle(newProfile)
    }

    fun scanSupportedSensors() {
        viewModelScope.launch {
            _isScanningSensors.value = true
            obdManager.logDiagnostic(
                tr(
                    "🔍 Escaneando sensores soportados por la centralita del coche...",
                    "🔍 Scanning sensors supported by the vehicle ECU..."
                )
            )
            _terminalLogs.value = _terminalLogs.value + tr(
                "> Escaneando sensores soportados (PIDs 0100/0120/Modo 22)...",
                "> Scanning supported sensors (PIDs 0100/0120/Mode 22)..."
            )
            val results = obdManager.scanSupportedPids()
            if (results.isEmpty()) {
                _isScanningSensors.value = false
                _csvImportNotification.value = tr(
                    "El escaneo no pudo completarse. Consulta el registro en Conexión.",
                    "Scan could not be completed. Check the log in Connection."
                )
                return@launch
            }
            val supportedCount = results.count { it.value }
            val unsupportedCount = results.size - supportedCount
            _isScanningSensors.value = false
            _csvImportNotification.value = tr(
                "Escaneo completado: $supportedCount sensores soportados, $unsupportedCount no disponibles.",
                "Scan complete: $supportedCount sensors supported, $unsupportedCount unavailable."
            )
            _terminalLogs.value = _terminalLogs.value + tr(
                "< Sensores soportados por la ECU: $supportedCount de ${results.size}",
                "< Sensors supported by ECU: $supportedCount of ${results.size}"
            )
        }
    }

    // GESTIÓN DE PESTAÑAS Y RANURAS
    private fun saveDashboardTabs(tabs: CustomDashboardTabs) {
        _customDashboards.value = tabs
        profileRepository.saveCustomDashboardTabs(tabs)
    }

    private fun updateDashboard(config: CustomDashboardConfig) {
        val stored = if (interfacePreferences.carMode) config.copy(
            gridLayout = _customDashboards.value.selectedDashboard.gridLayout, carGridLayout = config.gridLayout.forCar()) else config
        saveDashboardTabs(_customDashboards.value.updateSelected(stored))
    }

    fun selectDashboardTab(id: String) = saveDashboardTabs(_customDashboards.value.select(id))
    fun createDashboardTab(name: String, layout: DashboardGridLayout) {
        val tabs = _customDashboards.value.add(name, layout)
        saveDashboardTabs(if (interfacePreferences.carMode) tabs.updateSelected(tabs.selectedDashboard.copy(carGridLayout = layout.forCar())) else tabs)
    }
    fun renameDashboardTab(id: String, name: String) = saveDashboardTabs(_customDashboards.value.rename(id, name))
    fun moveDashboardTab(id: String, index: Int) = saveDashboardTabs(_customDashboards.value.move(id, index))

    fun duplicateDashboardTab(id: String) = saveDashboardTabs(_customDashboards.value.duplicate(id))
    fun deleteDashboardTab(id: String) = saveDashboardTabs(_customDashboards.value.delete(id))
    fun setDashboardDensity(density: com.torqueobd2.dash.data.model.DashboardDensity) = updateDashboard(currentDashboard.copy(density = density))
    fun addDashboardSensors(ids: List<String>) {
        val empty = currentDashboard.getSlotList().count { it == null }
        val widgets = ids.distinct().mapNotNull { id -> obdManager.activePids.value.firstOrNull { it.id == id && (!it.supportChecked || it.isSupported) } }
            .take(empty).map { pid -> DashboardWidget(type = com.torqueobd2.dash.data.model.WidgetType.COMPACT_TILE,
                pidId = pid.id, pidShortName = pid.shortName, title = pid.shortName.ifBlank { pid.name }) }
        enableWidgetSensors(widgets)
        updateDashboard(currentDashboard.fillEmptySlots(widgets))
    }
    fun transferDashboardWidget(source: Int, destination: Int, duplicate: Boolean) = updateDashboard(currentDashboard.transferSlot(source, destination, duplicate))
    fun importDashboardDesigns(configs: List<CustomDashboardConfig>) {
        val bound = configs.map { it.copy(widgets = emptyList(), slots = it.resolvedSlots().mapValues { entry -> entry.value.bindPid(obdManager.activePids.value) }) }
        saveDashboardTabs(_customDashboards.value.appendImported(bound))
        enableWidgetSensors(bound.flatMap { it.resolvedSlots().values })
    }

    fun setCustomGridLayout(layout: DashboardGridLayout) {
        updateDashboard(currentDashboard.copy(gridLayout = layout))
    }

    private fun enableWidgetSensors(widgets: List<DashboardWidget>) {
        val ids = widgets.mapNotNull { it.resolvePid(obdManager.activePids.value)?.id }.toSet()
        obdManager.activePids.value.filter { it.id in ids && !it.isEnabled }
            .forEach { obdManager.togglePid(it.id, true) }
    }

    fun setWidgetInSlot(slotIndex: Int, widget: DashboardWidget?) {
        if (slotIndex !in 0 until currentDashboard.gridLayout.slotCount) return
        widget?.let { enableWidgetSensors(listOf(it)) }
        updateDashboard(currentDashboard.withSlot(slotIndex, widget?.bindPid(obdManager.activePids.value)))
    }

    fun clearSlot(slotIndex: Int) = setWidgetInSlot(slotIndex, null)

    fun addCustomWidget(widget: DashboardWidget) {
        val targetSlot = (0 until currentDashboard.gridLayout.slotCount).firstOrNull {
            currentDashboard.getWidgetForSlot(it) == null
        } ?: return
        setWidgetInSlot(targetSlot, widget)
    }

    fun removeCustomWidget(widgetId: String) {
        updateDashboard(currentDashboard.copy(slots = currentDashboard.resolvedSlots().filterValues { it.id != widgetId }, widgets = emptyList()))
    }

    fun toggleWidgetSize(widgetId: String) {
        val slots = currentDashboard.resolvedSlots().mapValues { (_, widget) ->
            if (widget.id == widgetId) widget.copy(size = if (widget.size == WidgetSize.STANDARD) WidgetSize.FULL_WIDTH else WidgetSize.STANDARD)
            else widget
        }
        updateDashboard(currentDashboard.copy(slots = slots, widgets = emptyList()))
    }

    fun resetCustomDashboardToDefault() {
        val defaults = CustomDashboardConfig.getDefaultWidgets().filter { widget ->
            widget.resolvePid(obdManager.activePids.value)?.let { !it.supportChecked || it.isSupported } == true
        }
        enableWidgetSensors(defaults)
        updateDashboard(CustomDashboardConfig(widgets = defaults))
    }

    fun applyDashboardPreset(preset: DashboardPreset) {
        val widgets = preset.widgets.mapNotNull { widget ->
            widget.resolvePid(obdManager.activePids.value)?.takeIf { !it.supportChecked || it.isSupported }
                ?.let { widget.copy(pidId = it.id) }
        }
        enableWidgetSensors(widgets)
        updateDashboard(currentDashboard.copy(gridLayout = preset.gridLayout,
            slots = widgets.mapIndexed { index, widget -> index to widget }.toMap(), widgets = emptyList()))
    }

    fun clearCustomDashboard() {
        updateDashboard(currentDashboard.copy(slots = emptyMap(), widgets = emptyList()))
    }

    fun shouldPromptBluetoothOnStartup(): Boolean = runtime.shouldPromptBluetoothOnStartup()

    @SuppressLint("MissingPermission")
    fun refreshPairedBtDevices(log: Boolean = true) {
        val app = getApplication<Application>()

        // 1. Validar permisos
        if (!BluetoothPermissionHelper.hasAllPermissions(app)) {
            _bluetoothErrorHint.value = tr(
                "Faltan permisos de Bluetooth para detectar dispositivos. Concede los permisos solicitados.",
                "Missing Bluetooth permissions to detect devices. Grant the requested permissions."
            )
            _pairedBtDevices.value = emptyList()
            obdManager.logDiagnostic(
                tr(
                    "⚠️ Permisos de Bluetooth pendientes de concesión por el usuario.",
                    "⚠️ Bluetooth permissions pending user grant."
                )
            )
            return
        }
        runtime.onPermissionsUpdated()

        try {
            // 2. Obtener BluetoothAdapter de manera moderna y retrocompatible
            val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            @Suppress("DEPRECATION")
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter == null) {
                _bluetoothErrorHint.value = tr(
                    "Android no ofrece un adaptador Bluetooth a esta app. Usa WiFi si la radio solo permite Bluetooth desde su app de fábrica.",
                    "Android does not expose a Bluetooth adapter to this app. Use WiFi if your head unit only allows Bluetooth from its factory app."
                )
                _pairedBtDevices.value = emptyList()
                obdManager.logDiagnostic(
                    tr(
                        "❌ No se detectó hardware Bluetooth en el dispositivo.",
                        "❌ No Bluetooth hardware detected on the device."
                    )
                )
                return
            }

            if (!adapter.isEnabled) {
                _bluetoothErrorHint.value = tr(
                    "El Bluetooth está desactivado. Actívalo en los Ajustes de la radio para listar tu ELM327.",
                    "Bluetooth is disabled. Enable it in system Settings to list your ELM327."
                )
                _pairedBtDevices.value = emptyList()
                obdManager.logDiagnostic(
                    tr(
                        "⚠️ El adaptador Bluetooth está apagado en el sistema.",
                        "⚠️ Bluetooth adapter is turned off in the system."
                    )
                )
                return
            }

            val paired = adapter.bondedDevices?.map {
                DeviceItem(name = it.name ?: tr("Dispositivo Bluetooth", "Bluetooth Device"), address = it.address)
            } ?: emptyList()

            // Ordenar destacando los dispositivos con nombres típicos de OBD2
            val obdKeywords = listOf("OBD", "ELM", "VGATE", "VLINKER", "VIECA", "CARISTA", "KONNWEI", "SCAN")
            val sortedList = (discoveredDevices.values + paired).associateBy { it.address }.values.sortedByDescending { dev ->
                val upper = dev.name.uppercase()
                obdKeywords.any { upper.contains(it) }
            }

            _pairedBtDevices.value = sortedList
            _bluetoothErrorHint.value = null

            if (log && sortedList.isEmpty()) {
                obdManager.logDiagnostic(
                    tr(
                        "ℹ️ No hay ningún dispositivo Bluetooth emparejado en el sistema. Empareja primero el ELM327 en los Ajustes de Bluetooth de Android (PIN habitual: 1234 o 0000).",
                        "ℹ️ No paired Bluetooth devices found in the system. Pair the ELM327 first in Android Bluetooth Settings (common PIN: 1234 or 0000)."
                    )
                )
            } else if (log) {
                obdManager.logDiagnostic(
                    tr(
                        "📋 Dispositivos Bluetooth emparejados detectados: ${sortedList.size}",
                        "📋 Paired Bluetooth devices detected: ${sortedList.size}"
                    )
                )
            }
        } catch (e: SecurityException) {
            _pairedBtDevices.value = emptyList()
            _bluetoothErrorHint.value = tr(
                "Error de seguridad: Permiso de conexión Bluetooth denegado (${e.message}).",
                "Security error: Bluetooth connection permission denied (${e.message})."
            )
            obdManager.logDiagnostic(
                tr(
                    "❌ Error de permisos al leer bondedDevices: ${e.message}",
                    "❌ Permission error reading bondedDevices: ${e.message}"
                )
            )
        } catch (e: Exception) {
            _pairedBtDevices.value = emptyList()
            _bluetoothErrorHint.value = tr(
                "Error al consultar dispositivos: ${e.message}",
                "Error querying devices: ${e.message}"
            )
            obdManager.logDiagnostic(
                tr(
                    "❌ Excepción al buscar dispositivos: ${e.message}",
                    "❌ Exception searching for devices: ${e.message}"
                )
            )
        }
    }

    fun importCsvFromStream(inputStream: InputStream) {
        try {
            val result = TorqueCsvParser.parse(inputStream)
            if (result.pids.isNotEmpty()) {
                obdManager.addPids(result.pids)
                _csvImportNotification.value = tr(
                    "Se importaron exitosamente ${result.pids.size} PIDs desde el archivo CSV.",
                    "Successfully imported ${result.pids.size} PIDs from CSV file."
                )
                if (result.initCommands.isNotEmpty()) {
                    _terminalLogs.value = _terminalLogs.value + tr(
                        "Comandos Init detectados en CSV: ${result.initCommands.joinToString(", ")}",
                        "Init commands detected in CSV: ${result.initCommands.joinToString(", ")}"
                    )
                }
            } else {
                _csvImportNotification.value = tr(
                    "No se encontraron PIDs válidos en el archivo CSV.",
                    "No valid PIDs found in the CSV file."
                )
            }
        } catch (e: Exception) {
            _csvImportNotification.value = tr(
                "Error al procesar el archivo CSV: ${e.message}",
                "Error processing CSV file: ${e.message}"
            )
        }
    }

    fun importCsvFromString(csvText: String) {
        try {
            val result = TorqueCsvParser.parseString(csvText)
            if (result.pids.isNotEmpty()) {
                obdManager.addPids(result.pids)
                _csvImportNotification.value = tr(
                    "Se importaron ${result.pids.size} PIDs correctamente.",
                    "Successfully imported ${result.pids.size} PIDs."
                )
            } else {
                _csvImportNotification.value = tr(
                    "No se detectaron filas válidas de PIDs.",
                    "No valid PID rows detected."
                )
            }
        } catch (e: Exception) {
            _csvImportNotification.value = tr("Error: ${e.message}", "Error: ${e.message}")
        }
    }

    fun loadSampleHondaIdtecPids() {
        obdManager.addPids(DefaultPids.getHondaIdtecPids())
        val ids = DefaultPids.getHondaIdtecPids().map { it.id }.toSet()
        obdManager.activePids.value.filter { it.id in ids }.forEach { obdManager.togglePid(it.id, true) }
    }

    fun loadManufacturerPack(packId: String) {
        val pack = DefaultPids.getManufacturerPacks().firstOrNull { it.id == packId } ?: return
        obdManager.addPids(pack.pids)
        val ids = pack.pids.map { it.id }.toSet()
        obdManager.activePids.value.filter { it.id in ids }.forEach { obdManager.togglePid(it.id, true) }
        _csvImportNotification.value = tr(
            "Cargado conjunto '${pack.name}' (${pack.pids.size} PIDs).",
            "Loaded pack '${pack.name}' (${pack.pids.size} PIDs)."
        )
    }

    fun loadStandardPids() {
        obdManager.addPids(DefaultPids.getStandardPids())
        _csvImportNotification.value = tr("PIDs estándar OBD-II restaurados.", "Standard OBD-II PIDs restored.")
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

    private val backupRepository = com.torqueobd2.dash.data.repository.AppBackupRepository(application.applicationContext)

    fun exportAppBackup(outputStream: java.io.OutputStream) {
        backupRepository.exportToStream(outputStream, obdManager.activePids.value)
    }

    fun importAppBackup(inputStream: InputStream): com.torqueobd2.dash.data.repository.AppBackupSnapshot {
        val snapshot = backupRepository.importFromStream(inputStream)
        AppI18n.applyPreference(getApplication(), snapshot.languagePreference)
        obdManager.setPids(snapshot.pids)
        obdManager.refreshLocalization()
        savedAlertRules = snapshot.alertRules
        obdManager.setSensorAlertRules(snapshot.alertRules)
        _savedVehicles.value = snapshot.vehicles
        runtime.updateProfile(snapshot.activeVehicle)
        saveDashboardTabs(snapshot.dashboardTabs.bindPids(obdManager.activePids.value))
        return snapshot
    }
}

