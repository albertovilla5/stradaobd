package com.torqueobd2.dash.service

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.repository.*
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.obd.ObdManager
import com.torqueobd2.dash.util.BluetoothPermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.OutputStream

/** Una sola conexión compartida por las pantallas y el servicio; no pertenece a una Activity. */
class ObdRuntime(private val application: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = ConnectionPreferences(application)
    private val library = SensorLibraryRepository(application.getSharedPreferences("torque_sensors", Context.MODE_PRIVATE))
    private val actions = Mutex()
    val simulationEnabled = MutableStateFlow(preferences.simulationEnabled)
    val pollingMode = MutableStateFlow(preferences.pollingMode)
    val scanningBluetooth = MutableStateFlow(false)
    private val _backgroundEnabled = MutableStateFlow(preferences.backgroundEnabled)
    val backgroundEnabled = _backgroundEnabled.asStateFlow()
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning = _serviceRunning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private var pendingActions = 0
    private var preparingRecording = false
    private val uiVisibility = UiVisibility()
    private var uiVisible = false
    private var initializedConnection = false
    private var servicePending = false
    private var stoppingService = false
    private var suspendedTarget: ConnectionState? = null
    private val recorder = TelemetryRecorder(File(application.noBackupFilesDir, "recordings"))
    val recording = recorder.state
    private val profiles = VehicleProfileRepository(application)
    val manager = ObdManager(scope, profiles.getVehicleProfile(),
        initialPollingMode = preferences.pollingMode,
        onRealConnectionEstablished = preferences::saveLastConnection,
        canConnect = { type -> type != ConnectionType.BLUETOOTH || (!scanningBluetooth.value &&
            BluetoothPermissionHelper.hasAllPermissions(application) && BluetoothPermissionHelper.isBluetoothEnabled()) },
        onReading = { reading, pid, profile, connection ->
            recorder.record(reading, pid, recordingContext(profile, connection))
        })

    init {
        manager.setPids(library.load(manager.activePids.value))
        manager.setSensorAlertRules(SensorAlertRepository(application.getSharedPreferences("sensor_alerts", Context.MODE_PRIVATE)).load())
        scope.launch(Dispatchers.IO) { recorder.refresh() }
        scope.launch {
            AppI18n.languageFlow.drop(1).collect {
                manager.refreshLocalization()
            }
        }
        scope.launch {
            manager.activePids.map { pids -> pids.map { it.copy(isSupported = true, supportChecked = false) } }
                .distinctUntilChanged().collect(library::save)
        }
        scope.launch {
            manager.connectionState.map { Triple(it.type, it.status, it.statusMessage) }.distinctUntilChanged().collect { (_, status, detail) ->
                recorder.event(status.name, detail)
                reconcile()
            }
        }
        scope.launch {
            recording.map { it.active != null }.distinctUntilChanged().collect { active ->
                manager.setRecordingActive(active)
                reconcile()
            }
        }
        scope.launch {
            recording.map { it.active?.id }.distinctUntilChanged().collectLatest { id ->
                if (id != null) while (isActive) { delay(1000); recorder.flush() }
            }
        }
    }

    fun setUiVisible(owner: Any, visible: Boolean) {
        val nowVisible = uiVisibility.update(owner, visible)
        if (nowVisible == uiVisible) return
        uiVisible = nowVisible
        if (nowVisible) {
            manager.resumePolling()
            if (!initializedConnection) {
                initializeConnectionIfConfigured()
            } else suspendedTarget?.let { target ->
                suspendedTarget = null
                connectTarget(target)
            }
        }
        reconcile()
    }

    private fun initializeConnectionIfConfigured() {
        if (initializedConnection || profiles.getAllVehicles().isEmpty()) return
        initializedConnection = true
        if (simulationEnabled.value) manager.startSimulation() else preferences.lastConnection()?.let(::connectTarget)
    }

    fun setBackgroundEnabled(enabled: Boolean) {
        preferences.backgroundEnabled = enabled
        _backgroundEnabled.value = enabled
        _error.value = null
        reconcile()
    }

    fun setPollingMode(mode: PollingMode) {
        preferences.pollingMode = mode; pollingMode.value = mode
        manager.setPollingMode(mode)
    }

    private fun hasTarget() = manager.connectionState.value.let { it.activeDevice != null && it.status != ConnectionStatus.DISCONNECTED }
    private fun wantsService() = hasTarget() && (_backgroundEnabled.value || recording.value.active != null || preparingRecording)
    private fun canStartService() = BluetoothPermissionHelper.canStartConnectedDeviceService(application)

    fun shouldPromptBluetoothOnStartup(): Boolean =
        !simulationEnabled.value && preferences.lastConnection()?.type != ConnectionType.WIFI

    fun onPermissionsUpdated() {
        reconcile()
    }

    private fun reconcile() {
        if (wantsService() && canStartService()) {
            if (uiVisible && !_serviceRunning.value && !servicePending && !stoppingService) {
                servicePending = true
                try {
                    ContextCompat.startForegroundService(application, Intent(application, TelemetryForegroundService::class.java))
                } catch (e: RuntimeException) {
                    servicePending = false
                    _error.value = tr(
                        "No se pudo iniciar la lectura en segundo plano: ${e.message}",
                        "Could not start background reading: ${e.message}"
                    )
                }
            }
        } else if ((_serviceRunning.value || servicePending) && !stoppingService) {
            stoppingService = true
            application.stopService(Intent(application, TelemetryForegroundService::class.java))
            servicePending = false
        }
        manager.setBackgroundReading(!uiVisible && wantsService() && (_serviceRunning.value || servicePending))
        if (!uiVisible && !_serviceRunning.value && !servicePending) suspendConnection()
        // La preferencia puede desactivarse mientras el servicio aún está destruyéndose.
        if (!uiVisible && !wantsService()) suspendConnection()
    }

    private fun suspendConnection() {
        manager.pausePolling()
        if (recording.value.active != null && !_serviceRunning.value && !servicePending) {
            scope.launch {
                recorder.stop(
                    tr("Se detuvo la grabación al pasar a segundo plano sin servicio activo", "Recording stopped when moving to background without active service"),
                    RecordingEnd.INTERRUPTED
                )
            }
        }
        if (hasTarget() && suspendedTarget == null) {
            suspendedTarget = manager.connectionState.value
            manager.disconnect(
                tr(
                    "Lectura suspendida. Vuelve a la app para reanudarla.",
                    "Reading suspended. Return to the app to resume."
                )
            )
        }
    }

    fun onServiceStarted() {
        servicePending = false; stoppingService = false
        _serviceRunning.value = true
        if (wantsService() || uiVisible) manager.resumePolling()
        reconcile()
    }

    fun onServiceDestroyed() {
        val expected = stoppingService
        servicePending = false; stoppingService = false; _serviceRunning.value = false
        if (!expected) {
            _error.value = tr(
                "Android detuvo el servicio. Abre la app para reanudar la lectura.",
                "Android stopped the service. Open the app to resume reading."
            )
            scope.launch {
                recorder.stop(
                    tr("Se detuvo el servicio de lectura", "Reading service stopped"),
                    RecordingEnd.INTERRUPTED
                )
                if (!uiVisible) suspendConnection()
            }
        }
        // Solo una pantalla visible puede iniciar de nuevo un servicio.
        if (uiVisible && expected) reconcile()
    }

    fun startRecording() {
        if (_busy.value || recording.value.active != null) return
        if (!uiVisible || manager.connectionState.value.status != ConnectionStatus.CONNECTED) {
            _error.value = tr(
                "Conecta el adaptador o activa la simulación antes de grabar.",
                "Connect the adapter or enable simulation before recording."
            )
            return
        }
        beginAction(); preparingRecording = true; _error.value = null
        reconcile()
        scope.launch {
            try {
                actions.withLock {
                    if (canStartService()) {
                        val started = withTimeoutOrNull(5000) { serviceRunning.first { it } }
                        if (started == null && !uiVisible) {
                            _error.value = tr(
                                "No se pudo preparar el servicio de grabación.",
                                "Could not prepare the recording service."
                            )
                            return@withLock
                        }
                    }
                    if (manager.connectionState.value.status != ConnectionStatus.CONNECTED) {
                        _error.value = tr(
                            "La conexión cambió antes de iniciar la grabación.",
                            "Connection changed before recording started."
                        )
                        return@withLock
                    }
                    recorder.start(recordingContext(manager.vehicleProfile.value, manager.connectionState.value))
                }
            } finally { preparingRecording = false; endAction(); reconcile() }
        }
    }

    fun stopRecording() = change(tr("Detenida por el usuario", "Stopped by user")) {}
    fun stopEverything() = change(tr("Lectura y grabación detenidas por el usuario", "Reading and recording stopped by user")) {
        suspendedTarget = null
        simulationEnabled.value = false; preferences.simulationEnabled = false
        manager.disconnect()
    }

    fun setSimulationEnabled(enabled: Boolean) = change(tr("Cambió el origen de los datos", "Data source changed")) {
        suspendedTarget = null
        simulationEnabled.value = enabled; preferences.simulationEnabled = enabled
        if (enabled) manager.startSimulation() else {
            manager.disconnect()
            preferences.lastConnection()?.let(::connectTarget)
        }
    }

    fun connectBluetooth(device: DeviceItem) = change(tr("Cambió el adaptador", "Adapter changed")) {
        useRealConnection(); manager.connectBluetooth(device)
    }
    fun connectWifi(host: String, port: Int) = change(tr("Cambió el adaptador", "Adapter changed")) {
        useRealConnection(); manager.connectWifi(host, port)
    }
    fun updateProfile(profile: VehicleProfile) = change(tr("Cambió la configuración del vehículo", "Vehicle configuration changed")) {
        manager.updateProfile(profile)
        if (uiVisible) initializeConnectionIfConfigured()
    }

    private fun useRealConnection() {
        suspendedTarget = null
        simulationEnabled.value = false; preferences.simulationEnabled = false
    }
    private fun connectTarget(target: ConnectionState) {
        when (target.type) {
            ConnectionType.SIMULATION -> manager.startSimulation()
            ConnectionType.BLUETOOTH -> target.activeDevice?.let(manager::connectBluetooth)
            ConnectionType.WIFI -> manager.connectWifi(target.wifiHost, target.wifiPort)
        }
    }

    private fun beginAction() { pendingActions++; _busy.value = true }
    private fun endAction() { pendingActions--; _busy.value = pendingActions > 0 }

    private fun change(reason: String, operation: () -> Unit) {
        beginAction()
        scope.launch {
            try { actions.withLock { recorder.stop(reason); operation() } }
            finally { endAction(); reconcile() }
        }
    }

    fun deleteRecording(id: String) = scope.launch {
        try {
            recorder.delete(id)
        } catch (e: Exception) {
            _error.value = tr(
                "No se pudo eliminar la sesión: ${e.message}",
                "Could not delete session: ${e.message}"
            )
        }
    }
    suspend fun exportRecording(id: String, output: OutputStream) = recorder.export(id, output)

    private fun recordingContext(profile: VehicleProfile, connection: ConnectionState) = RecordingContext(
        profile.id, profile.name, connection.type, connection.activeDevice?.name.orEmpty(), connection.activeDevice?.address.orEmpty())
}

