package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.parser.DiagnosticResponseParser
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.ObdReading
import com.torqueobd2.dash.data.model.PollingMode
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.model.SensorTelemetryStatus
import com.torqueobd2.dash.data.model.SensorIds
import com.torqueobd2.dash.data.parser.PidDecoder
import com.torqueobd2.dash.data.parser.PidFieldUnavailableException
import com.torqueobd2.dash.data.parser.EquationEvaluator
import com.torqueobd2.dash.data.parser.SupportedPidsDetector
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.i18n.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Gestor central de OBD2: coordina la conexión física o simulada,
 * ejecuta el ciclo de lectura (polling) de PIDs soportados, evalúa ecuaciones matemáticas,
 * rechaza datos inválidos y actualiza el estado unificado sin estimaciones.
 */
class ObdManager(
    private val scope: CoroutineScope,
    initialProfile: VehicleProfile = VehicleProfile(),
    private val onRealConnectionEstablished: (ConnectionState) -> Unit = {},
    private val canConnect: (ConnectionType) -> Boolean = { true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectionFactory: ((ConnectionState) -> ObdConnection)? = null,
    private val retryDelayMillis: (Long) -> Long = { minOf(it * 2000L, 8000L) },
    initialPollingMode: PollingMode = PollingMode.VISIBLE,
    private val readingTimeoutMillis: Long = 5000L,
    private val onReading: suspend (ObdReading, ObdPid, VehicleProfile, ConnectionState) -> Unit = { _, _, _, _ -> }
) {
    private var connection: ObdConnection = SimulatedObdConnection()
    private var pollingJob: Job? = null
    private var freshnessJob: Job? = null
    private var connectionJob: Job? = null
    private var dtcJob: Job? = null
    private val diagnosticGeneration = java.util.concurrent.atomic.AtomicLong()
    private val diagnosticSession: Long get() = diagnosticGeneration.get()
    private val _dtcState = MutableStateFlow(DtcDiagnosticState())
    val dtcState = _dtcState.asStateFlow()
    private var pollingPaused = false
    private val telemetryLock = Any()
    private val alertEvaluator = SensorAlertEvaluator()
    private val alertRules = MutableStateFlow<List<SensorAlertRule>>(emptyList())
    private val _sensorAlerts = MutableStateFlow<List<SensorAlertState>>(emptyList())
    val sensorAlerts = _sensorAlerts.asStateFlow()
    private val sessionMutex = Mutex()
    private val commandSequenceMutex = Mutex()
    private var adapterHeader = ""
    private val pollingSelection = MutableStateFlow(
        initialPollingMode to ObdPollingPolicy.screenSensors(0, CustomDashboardConfig())
    )
    private var isManuallyDisconnected = false
    private val recordingActive = MutableStateFlow(false)
    private val backgroundReading = MutableStateFlow(false)

    fun setBackgroundReading(active: Boolean) { backgroundReading.value = active }
    fun setRecordingActive(active: Boolean) { recordingActive.value = active }

    private val _connectionState = MutableStateFlow(
        ConnectionState(customInitCommands = parseInitCommands(initialProfile.customInitCommands))
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _vehicleProfile = MutableStateFlow(initialProfile)
    val vehicleProfile: StateFlow<VehicleProfile> = _vehicleProfile.asStateFlow()

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _activePids = MutableStateFlow<List<ObdPid>>(emptyList())
    val activePids: StateFlow<List<ObdPid>> = _activePids.asStateFlow()


    private val _diagnosticLogs = MutableStateFlow<List<String>>(
        listOf(
            "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] ${
                tr(
                    "Sistema OBD2 iniciado. Simulación desactivada.",
                    "OBD2 system started. Simulation disabled."
                )
            }"
        )
    )
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    fun logDiagnostic(entry: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$time] $entry"
        _diagnosticLogs.update { entries ->
            (entries + formatted).takeLast(200)
        }
    }

    fun clearDiagnosticLogs() {
        _diagnosticLogs.value = emptyList()
    }

    // Conteo de comandos por segundo
    private var commandCounter = 0
    private var lastRateCalculationTime = System.nanoTime()

    init {
        val initialPids = mutableListOf<ObdPid>()
        initialPids.addAll(DefaultPids.getStandardPids())
        // Las fórmulas propietarias del CSV requieren selección explícita para la ECU concreta.
        initialPids.addAll(DefaultPids.getHondaIdtecPids().map { it.copy(isEnabled = false) })
        _activePids.value = initialPids

    }

    fun refreshLocalization() {
        val builtInById = DefaultPids.getAllBuiltInPidsById()
        _activePids.update { current ->
            current.map { pid ->
                val builtIn = builtInById[pid.id]
                if (builtIn != null) {
                    pid.copy(
                        name = builtIn.name,
                        shortName = builtIn.shortName,
                        valueLabels = builtIn.valueLabels
                    )
                } else pid
            }
        }
        synchronized(telemetryLock) {
            _vehicleState.update { state ->
                val updatedReadings = state.readings.mapValues { (id, reading) ->
                    val builtIn = builtInById[id]
                    if (builtIn != null) {
                        reading.copy(
                            pidName = builtIn.name,
                            shortName = builtIn.shortName,
                            valueLabels = builtIn.valueLabels
                        )
                    } else reading
                }
                state.copy(readings = updatedReadings)
            }
            evaluateAlerts()
        }
        _connectionState.update { state ->
            val updatedDevice = if (state.type == ConnectionType.SIMULATION && state.activeDevice?.address == "local") {
                state.activeDevice.copy(name = tr("Simulador Honda i-DTEC", "Honda i-DTEC Simulator"))
            } else state.activeDevice
            val updatedMessage = when (state.status) {
                ConnectionStatus.CONNECTED -> if (state.type == ConnectionType.SIMULATION) {
                    tr("Simulación activa", "Simulation active")
                } else {
                    tr("Conectado a ${updatedDevice?.name}", "Connected to ${updatedDevice?.name}")
                }
                ConnectionStatus.DISCONNECTED -> if (isManuallyDisconnected) {
                    tr(
                        "Desconectado manualmente. Selecciona un adaptador para volver a conectar.",
                        "Disconnected manually. Select an adapter to reconnect."
                    )
                } else {
                    tr("Selecciona un adaptador o activa la simulación.", "Select an adapter or enable simulation.")
                }
                else -> state.statusMessage
            }
            state.copy(activeDevice = updatedDevice, statusMessage = updatedMessage)
        }
    }

    fun updateProfile(newProfile: VehicleProfile) {
        resetDiagnostics()
        synchronized(telemetryLock) {
            alertEvaluator.reset()
            _vehicleProfile.value = newProfile
            _vehicleState.value = VehicleState()
            evaluateAlerts()
        }
        _connectionState.update {
            it.copy(customInitCommands = parseInitCommands(newProfile.customInitCommands))
        }
    }

    fun setPids(pids: List<ObdPid>) {
        val previous = _activePids.value.associateBy { it.id }
        _activePids.value = pids.filterNot { it.cleanObdCommand().startsWith("CALC_") }.distinctBy { it.id }
        val retainedIds = _activePids.value.filter { previous[it.id]?.isEnabled == it.isEnabled }.map { it.id }.toSet()
        _vehicleState.update { it.copy(sensorStatuses = it.sensorStatuses.filterKeys { id -> id in retainedIds }) }
        invalidateReadings(_vehicleState.value.readings.filterValues { reading ->
            _activePids.value.none { pid ->
                pid.id == reading.pidId && pid.isEnabled && pid.isSupported &&
                    previous[pid.id]?.copy(isEnabled = true, isSupported = true, supportChecked = false) ==
                    pid.copy(isEnabled = true, isSupported = true, supportChecked = false)
            }
        }.keys)
        synchronized(telemetryLock) { evaluateAlerts() }
    }

    fun setSensorAlertRules(rules: List<SensorAlertRule>) {
        require(rules.all { it.validationError() == null })
        synchronized(telemetryLock) {
            alertRules.value = rules.toList()
            evaluateAlerts()
        }
    }

    // Se llama bajo telemetryLock, también al invalidar o pausar los datos.
    private fun evaluateAlerts() {
        _sensorAlerts.value = alertEvaluator.evaluate(
            alertRules.value.filter { it.vehicleId == _vehicleProfile.value.id },
            _activePids.value, _vehicleState.value, _connectionState.value)
    }

    fun setVisibleSensors(pidIds: Set<String>) {
        pollingSelection.update { it.first to pidIds.toSet() }
    }

    fun setPollingMode(mode: PollingMode) {
        pollingSelection.update { mode to it.second }
    }

    fun addPids(newPids: List<ObdPid>) {
        val current = _activePids.value.toMutableList()
        for (p in newPids.filterNot { it.cleanObdCommand().startsWith("CALC_") }) {
            val existingIdx = current.indexOfFirst { it.id == p.id || (it.cleanObdCommand() == p.cleanObdCommand() &&
                it.header.replace(" ", "").uppercase() == p.header.replace(" ", "").uppercase() &&
                it.name == p.name && it.shortName == p.shortName && it.equation == p.equation && it.units == p.units) }
            if (existingIdx >= 0) {
                val existing = current[existingIdx]
                current[existingIdx] = p.copy(id = existing.id, isEnabled = existing.isEnabled,
                    isSupported = existing.isSupported, supportChecked = existing.supportChecked)
            } else {
                current.add(p)
            }
        }
        setPids(current)
    }

    fun togglePid(pidId: String, isEnabled: Boolean) {
        val updated = _activePids.value.map {
            if (it.id == pidId) it.copy(isEnabled = isEnabled) else it
        }
        setPids(updated)
    }

    fun setManualThrottle(percent: Float?) {
        (connection as? SimulatedObdConnection)?.setManualThrottle(percent)
    }

    /**
     * Escanea activamente la ECU del coche para identificar qué PIDs están soportados
     * mediante las tramas 0100, 0120, etc.
     */
    suspend fun scanSupportedPids(includeExtended: Boolean = true): Map<String, Boolean> {
        return try {
            commandSequenceMutex.withLock { scanSupportedPidsInternal(includeExtended) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logDiagnostic(tr("❌ Escaneo interrumpido: ${e.message}", "❌ Scan interrupted: ${e.message}"))
            if (!connection.isConnected()) {
                triggerAutoReconnect(tr("Enlace perdido durante el escaneo", "Link lost during scan"))
            }
            emptyMap()
        }
    }

    private fun parseInitCommands(value: String): List<String> =
        value.replace("\\n", "\n").lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun initialHeader(): String = _connectionState.value.customInitCommands
        .map { it.replace(" ", "").uppercase() }
        .lastOrNull { it.startsWith("ATSH") }?.removePrefix("ATSH") ?: ""

    private suspend fun configureAdapter(command: String) {
        val raw = connection.sendCommand(command)
        val response = Elm327Protocol.cleanResponse(raw).replace(" ", "").uppercase()
            .removePrefix(command.replace(" ", "").uppercase())
        if (response != "OK") {
            throw java.io.IOException(
                tr(
                    "El adaptador rechazó $command: ${raw.trim()}",
                    "Adapter rejected $command: ${raw.trim()}"
                )
            )
        }
    }

    private suspend fun selectHeader(header: String) {
        if (header == adapterHeader) return
        if (header.isBlank()) {
            // AT D restaura la cabecera automática sin asumir un protocolo CAN concreto.
            // Reaplicar el formato y la inicialización del perfil porque AT D también los restablece.
            adapterHeader = "?"
            for (command in listOf("AT D", "AT E0", "AT L0", "AT S0", "AT H0", "AT AT1", "AT SP0") +
                _connectionState.value.customInitCommands) configureAdapter(command)
        } else {
            configureAdapter("AT SH $header")
        }
        adapterHeader = header
    }

    private suspend fun scanSupportedPidsInternal(includeExtended: Boolean): Map<String, Boolean> {
        val supportedHexPids = mutableSetOf<String>()
        val checkedStandardPids = mutableSetOf<String>()
        val extendedResults = mutableMapOf<String, Boolean>() // Por sensor/ECU, no solo por comando.

        if (!connection.isConnected()) return emptyMap()

        // Solo mapas de soporte al conectar: no consultar toda la biblioteca extendida.
        val scanHeader = initialHeader()
        selectHeader(scanHeader)
        for (base in 0..0xE0 step 0x20) {
            val command = "01%02X".format(base)
            val response = connection.sendCommand(command)
            if (Elm327Protocol.extractDataBytes(command, response).size < 4) break
            checkedStandardPids.addAll((base + 1..minOf(base + 0x20, 0xFF)).map { "%02X".format(it) })
            val supported = SupportedPidsDetector.parseSupportedPidsRange(command, response)
            supportedHexPids.addAll(supported)
            if ("%02X".format(base + 0x20) !in supported) {
                checkedStandardPids.addAll((base + 0x21..0xFF).map { "%02X".format(it) })
                break
            }
        }

        // El escaneo completo sigue disponible cuando se solicita expresamente.
        if (includeExtended) {
            val requests = ObdPollingPolicy.requests(_activePids.value.map { it.copy(isEnabled = true) },
                hideUnsupported = false, initialHeader = initialHeader())
            for (request in requests.filter { it.command.startsWith("22") }) {
                selectHeader(request.header)
                val response = connection.sendCommand(request.command)
                val bytes = Elm327Protocol.extractDataBytes(request.command, response)
                val variables = EquationEvaluator.mapBytesToVariables(bytes)
                val failure = SupportedPidsDetector.responseFailure(request.command, response)
                request.pids.forEach { pid ->
                    when {
                        failure == SensorTelemetryStatus.INCOMPATIBLE -> {
                            extendedResults[pid.id] = false
                            invalidateReadings(setOf(pid.id), failure)
                        }
                        bytes.isNotEmpty() -> {
                            // Positive response proves command support, not validity of every field.
                            extendedResults[pid.id] = true
                            try { PidDecoder.decode(pid, variables) }
                            catch (e: Exception) { invalidateReadings(setOf(pid.id),
                                if (e is PidFieldUnavailableException) SensorTelemetryStatus.FIELD_UNAVAILABLE else SensorTelemetryStatus.INVALID_RESPONSE) }
                        }
                        else -> invalidateReadings(setOf(pid.id), failure ?: SensorTelemetryStatus.INVALID_RESPONSE)
                    }
                }
            }
        }

        // 3. Actualizar la lista en memoria
        _activePids.update { current ->
            val previous = current.associateBy { it.id }
            SupportedPidsDetector.updatePidsSupport(current, supportedHexPids, emptyMap(), checkedStandardPids).map { pid ->
                if (pid.cleanObdCommand().startsWith("22")) {
                    pid.copy(isSupported = extendedResults[pid.id] ?: previous.getValue(pid.id).isSupported,
                        supportChecked = pid.id in extendedResults || previous.getValue(pid.id).supportChecked)
                } else if (pid.header.isNotBlank() && pid.header.replace(" ", "").uppercase() != scanHeader) {
                    previous.getValue(pid.id)
                } else pid
            }
        }

        invalidateReadings(_vehicleState.value.readings.filterValues { reading ->
            _activePids.value.none { it.id == reading.pidId && it.isEnabled && it.isSupported }
        }.keys)
        return _activePids.value.associate { it.id to it.isSupported }
    }

    fun pausePolling() {
        pollingPaused = true
        stopPolling()
    }

    fun resumePolling() {
        pollingPaused = false
        _vehicleState.update { it.copy(sensorStatuses = it.sensorStatuses.filterValues { status -> status != SensorTelemetryStatus.PAUSED }) }
        if (isManuallyDisconnected) return
        if (connection.isConnected() && pollingJob == null) {
            startPolling()
        } else if (!connection.isConnected()) {
            triggerAutoReconnect(tr("Reanudando conexión automática", "Resuming automatic connection"))
        }
    }

    fun startSimulation() {
        isManuallyDisconnected = false
        replaceConnectionJob {
            _connectionState.value = ConnectionState(
                type = ConnectionType.SIMULATION,
                activeDevice = DeviceItem(tr("Simulador Honda i-DTEC", "Honda i-DTEC Simulator"), "local"),
                status = ConnectionStatus.CONNECTING,
                statusMessage = tr("Iniciando simulación...", "Starting simulation..."),
                customInitCommands = _connectionState.value.customInitCommands
            )
            connection = connectionFactory?.invoke(_connectionState.value) ?: SimulatedObdConnection()
            if (connection.connect()) {
                _connectionState.update { it.copy(status = ConnectionStatus.CONNECTED, statusMessage = tr("Simulación activa", "Simulation active")) }
                startPolling()
            }
        }
    }

    fun connectBluetooth(device: DeviceItem) {
        connectReal(_connectionState.value.copy(type = ConnectionType.BLUETOOTH, activeDevice = device))
    }

    fun connectWifi(host: String, port: Int) {
        if (host.isBlank() || port !in 1..65535) {
            logDiagnostic(tr("Indica una IP y un puerto WiFi válido (1–65535).", "Enter a valid WiFi IP and port (1–65535)."))
            return
        }
        connectReal(_connectionState.value.copy(
            type = ConnectionType.WIFI, wifiHost = host.trim(), wifiPort = port,
            activeDevice = DeviceItem("WiFi ELM327", "${host.trim()}:$port")
        ))
    }

    private fun connectReal(target: ConnectionState) {
        isManuallyDisconnected = false
        replaceConnectionJob {
            _connectionState.value = target.copy(
                status = ConnectionStatus.CONNECTING, commandsPerSecond = 0f,
                statusMessage = tr("Conectando a ${target.activeDevice?.name}...", "Connecting to ${target.activeDevice?.name}...")
            )
            connectUntilAvailable()
        }
    }

    // Cada selección sustituye el intento anterior y espera su cierre antes de abrir otro socket.
    private fun replaceConnectionJob(action: suspend () -> Unit) {
        val previousDtc = dtcJob
        resetDiagnostics()
        val previous = connectionJob
        previous?.cancel()
        val previousPolling = pollingJob
        stopPolling()
        connectionJob = scope.launch {
            previous?.join()
            previousPolling?.join()
            previousDtc?.join()
            sessionMutex.withLock {
                connection.disconnect()
                clearTelemetry()
                try {
                    action()
                } catch (e: CancellationException) {
                    withContext(NonCancellable) { connection.disconnect() }
                    throw e
                }
            }
        }
    }

    private fun clearTelemetry() {
        _vehicleState.value = VehicleState()
        adapterHeader = ""
        // El soporte pertenece a la ECU de esta sesión. Los PIDs extendidos
        // se probarán al necesitarse, aunque otra sesión devolviese NO DATA.
        _activePids.update { pids -> pids.map { it.copy(isSupported = true, supportChecked = false) } }
        commandCounter = 0
        lastRateCalculationTime = System.nanoTime()
        _connectionState.update { it.copy(commandsPerSecond = 0f) }
    }

    private suspend fun connectUntilAvailable() {
        var attempt = 0
        while (currentCoroutineContext().isActive && !isManuallyDisconnected) {
            val target = _connectionState.value
            if (!canConnect(target.type)) {
                _connectionState.update { it.copy(
                    status = ConnectionStatus.RECONNECTING,
                    statusMessage = tr(
                        "Esperando Bluetooth, permisos o fin de búsqueda para conectar a ${target.activeDevice?.name}",
                        "Waiting for Bluetooth, permissions, or scan completion to connect to ${target.activeDevice?.name}"
                    )
                ) }
                delay(2000)
                continue
            }
            attempt++
            _connectionState.update { it.copy(
                status = if (attempt == 1) ConnectionStatus.CONNECTING else ConnectionStatus.RECONNECTING,
                statusMessage = tr(
                    "Conectando a ${target.activeDevice?.name} (intento $attempt)...",
                    "Connecting to ${target.activeDevice?.name} (attempt $attempt)..."
                )
            ) }
            connection = connectionFactory?.invoke(target) ?: when (target.type) {
                ConnectionType.BLUETOOTH -> BluetoothObdConnection(
                    target.activeDevice!!.address, target.customInitCommands, ::logDiagnostic
                )
                ConnectionType.WIFI -> WifiObdConnection(target.wifiHost, target.wifiPort, target.customInitCommands, ::logDiagnostic)
                ConnectionType.SIMULATION -> return
            }
            val connected = try {
                connection.connect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logDiagnostic(tr("Error de conexión: ${e.message}", "Connection error: ${e.message}"))
                false
            }
            currentCoroutineContext().ensureActive()
            if (connected) {
                _connectionState.update {
                    it.copy(
                        status = ConnectionStatus.CONNECTED,
                        statusMessage = tr("Conectado a ${target.activeDevice?.name}", "Connected to ${target.activeDevice?.name}")
                    )
                }
                onRealConnectionEstablished(_connectionState.value)
                scanSupportedPids(includeExtended = pollingSelection.value.first == PollingMode.ALL)
                if (connection.isConnected()) {
                    startPolling()
                    return
                }
            }
            val error = connection.statusMessage.value
            connection.disconnect()
            val retryMillis = retryDelayMillis(attempt.toLong())
            val delaySeconds = retryMillis / 1000
            _connectionState.update { it.copy(
                status = ConnectionStatus.RECONNECTING,
                statusMessage = tr("$error. Reintentando en ${delaySeconds}s...", "$error. Retrying in ${delaySeconds}s..."),
                commandsPerSecond = 0f
            ) }
            logDiagnostic(
                tr(
                    "Reconexión a ${target.activeDevice?.name}: intento $attempt; nuevo intento en ${delaySeconds}s.",
                    "Reconnecting to ${target.activeDevice?.name}: attempt $attempt; next attempt in ${delaySeconds}s."
                )
            )
            delay(retryMillis)
        }
    }

    fun disconnect(
        message: String = tr(
            "Desconectado manualmente. Selecciona un adaptador para volver a conectar.",
            "Disconnected manually. Select an adapter to reconnect."
        )
    ) {
        isManuallyDisconnected = true
        replaceConnectionJob {
            _connectionState.update { it.copy(
                type = if (it.type == ConnectionType.SIMULATION) ConnectionType.BLUETOOTH else it.type,
                status = ConnectionStatus.DISCONNECTED, activeDevice = null,
                statusMessage = message
            ) }
        }
    }

    fun close() {
        isManuallyDisconnected = true
        val previousDtc = dtcJob
        resetDiagnostics()
        val previous = connectionJob
        previous?.cancel()
        stopPolling()
        scope.launch(NonCancellable) {
            previous?.join()
            previousDtc?.join()
            sessionMutex.withLock { connection.disconnect() }
        }
    }

    private fun resetDiagnostics() {
        diagnosticGeneration.incrementAndGet()
        dtcJob?.cancel()
        _dtcState.value = DtcDiagnosticState()
    }

    fun cancelDiagnostics() { dtcJob?.cancel() }
    fun readTroubleCodes() { launchDiagnostics(null, false) }
    fun clearTroubleCodes(expectedSession: Long, engineStoppedConfirmed: Boolean) {
        if (!engineStoppedConfirmed || _dtcState.value.report?.session != expectedSession || expectedSession != diagnosticSession) {
            _dtcState.update {
                it.copy(
                    error = tr(
                        "El borrado necesita confirmación y una lectura de esta conexión.",
                        "Clearing requires confirmation and a reading from this connection."
                    )
                )
            }
            return
        }
        launchDiagnostics(expectedSession, true)
    }

    private fun launchDiagnostics(expectedSession: Long?, clear: Boolean) {
        if (_dtcState.value.busy) return
        if (!connection.isConnected() || _connectionState.value.status != ConnectionStatus.CONNECTED) {
            _dtcState.update {
                it.copy(
                    error = tr(
                        "Conecta el adaptador antes de consultar averías.",
                        "Connect the adapter before reading trouble codes."
                    )
                )
            }
            return
        }
        val epoch = diagnosticSession
        val link = connection
        val vehicle = _vehicleProfile.value
        val simulated = _connectionState.value.type == ConnectionType.SIMULATION
        val adapter = _connectionState.value.activeDevice?.name ?: "ELM327"
        _dtcState.update {
            it.copy(
                busy = true,
                progress = tr("Preparando diagnóstico", "Preparing diagnostics"),
                error = null,
                clearResult = null
            )
        }
        dtcJob = scope.launch(ioDispatcher) {
            try {
                commandSequenceMutex.withLock {
                    currentCoroutineContext().ensureActive()
                    check(epoch == diagnosticSession && link === connection && link.isConnected()) {
                        tr("La conexión cambió", "Connection changed")
                    }
                    try {
                        // Dirección funcional estándar: consultar todas las ECU OBD de emisiones.
                        // AT D elimina filtros/cabeceras dejados por sensores o por el terminal.
                        for (cmd in listOf("AT D", "AT E0", "AT L0", "AT S1", "AT H1", "AT AT1", "AT SP0")) configureAdapter(cmd)
                        link.sendCommand("0100") // Selección automática del protocolo antes de AT DPN.
                        val protocol = Elm327Protocol.cleanResponse(link.sendCommand("AT DPN"))
                            .replace(" ", "").uppercase().removePrefix("ATDPN").removePrefix("A").toIntOrNull(16)
                        val can = protocol?.let { it in 6..9 }
                        if (can == true) configureAdapter("AT CAF1")
                        val reader = DtcDiagnosticReader({ cmd ->
                            currentCoroutineContext().ensureActive()
                            check(epoch == diagnosticSession && link === connection) {
                                tr("La conexión cambió", "Connection changed")
                            }
                            link.sendCommand(cmd)
                        }, can) { text -> if (epoch == diagnosticSession) _dtcState.update { it.copy(progress = text) } }
                        if (clear) {
                            check(expectedSession == epoch) {
                                tr("Informe de otra conexión", "Report from another connection")
                            }
                            if (!simulated) {
                                val rpm = DiagnosticResponseParser.parse("010C", link.sendCommand("010C"), can)
                                    .frames.mapNotNull { DiagnosticResponseParser.pidData(it, 1, 12) }
                                    .filter { it.size >= 2 }.map { (it[0] * 256 + it[1]) / 4.0 }
                                check(rpm.none { it > 0 }) {
                                    tr(
                                        "Motor en marcha: apágalo y deja el contacto encendido antes de borrar.",
                                        "Engine running: turn off the engine and leave ignition on before clearing."
                                    )
                                }
                            }
                            val result = reader.clear()
                            if (epoch == diagnosticSession) _dtcState.update { it.copy(clearResult = result) }
                            // Releer incluso si la ECU no confirma: un timeout no prueba que el borrado fallase.
                        }
                        val report = reader.read(vehicle, adapter, simulated, epoch)
                        if (epoch == diagnosticSession) _dtcState.update { it.copy(report = report) }
                    } finally {
                        withContext(NonCancellable) {
                            if (link === connection && link.isConnected()) {
                                try {
                                    for (cmd in listOf("AT D", "AT E0", "AT L0", "AT S0", "AT H0", "AT AT1", "AT SP0") +
                                        _connectionState.value.customInitCommands + listOf("AT E0", "AT L0", "AT S0", "AT H0")) configureAdapter(cmd)
                                    adapterHeader = initialHeader()
                                } catch (e: Exception) {
                                    adapterHeader = "?"
                                    link.disconnect()
                                    logDiagnostic(
                                        tr(
                                            "No se pudo restaurar el adaptador tras el diagnóstico: ${e.message}",
                                            "Could not restore adapter after diagnostics: ${e.message}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                if (epoch == diagnosticSession) {
                    _dtcState.update {
                        it.copy(
                            error = if (clear) {
                                tr(
                                    "Consulta cancelada. La captura puede ser anterior al borrado; vuelve a leer las averías.",
                                    "Query cancelled. Snapshot may be from before clearing; read trouble codes again."
                                )
                            } else {
                                tr(
                                    "Consulta cancelada; se conserva la última captura.",
                                    "Query cancelled; keeping last snapshot."
                                )
                            }
                        )
                    }
                }
                throw e
            } catch (e: Exception) {
                if (epoch == diagnosticSession) {
                    _dtcState.update { it.copy(error = e.message ?: tr("Falló el diagnóstico", "Diagnostics failed")) }
                }
                logDiagnostic(tr("Diagnóstico DTC: ${e.message}", "DTC Diagnostics: ${e.message}"))
            } finally {
                if (epoch == diagnosticSession) {
                    _dtcState.update { it.copy(busy = false, progress = "") }
                    if (!link.isConnected()) {
                        triggerAutoReconnect(tr("Enlace perdido durante diagnóstico", "Link lost during diagnostics"))
                    }
                }
            }
        }
    }

    suspend fun sendManualCommand(command: String): String {
        if (!connection.isConnected()) return "ERROR: NOT CONNECTED"
        return try {
            commandSequenceMutex.withLock {
                val response = connection.sendCommand(command)
                // Un comando de terminal puede cambiar/restablecer la cabecera.
                adapterHeader = "?"
                response
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!connection.isConnected()) {
                triggerAutoReconnect(tr("Enlace perdido desde terminal", "Link lost from terminal"))
            }
            logDiagnostic(tr("❌ Falló $command: ${e.message}", "❌ Command $command failed: ${e.message}"))
            "ERROR: ${e.message}"
        }
    }

    private fun startPolling() {
        if (pollingPaused || !connection.isConnected()) return
        pollingJob?.cancel()
        freshnessJob?.cancel()
        freshnessJob = scope.launch(ioDispatcher) {
            while (isActive) {
                delay(250)
                expireReadings()
            }
        }
        pollingJob = scope.launch(ioDispatcher) {
            commandCounter = 0
            lastRateCalculationTime = System.nanoTime()
            val backgroundBudget = BackgroundPollingBudget()
            val retryAfter = mutableMapOf<Pair<String, String>, Long>()

            while (isActive && connection.isConnected()) {
                val selection = pollingSelection.value
                val recording = recordingActive.value
                val backgroundReadingActive = backgroundReading.value
                val mode = selection.first
                val watched = alertRules.value.filter { it.enabled && it.vehicleId == _vehicleProfile.value.id }
                    .map { it.pidId }.toSet()
                val wanted = ObdPollingPolicy.withDependencies(selection.second + watched)
                val requests = ObdPollingPolicy.requests(_activePids.value,
                    _vehicleProfile.value.hideUnsupportedSensors, initialHeader())
                val foreground = requests.filter { request ->
                    recording || backgroundReadingActive || mode == PollingMode.ALL || request.pids.any { it.id in wanted }
                }
                val background = if (mode == PollingMode.BALANCED) {
                    backgroundBudget.next(requests.filter { request ->
                        request !in foreground && request.pids.any { it.id in ObdPollingPolicy.backgroundSensors }
                    }, System.nanoTime() / 1_000_000)
                } else null
                val selected = (foreground + listOfNotNull(background)).filter { request ->
                    System.nanoTime() >= (retryAfter[request.header to request.command] ?: 0L)
                }

                if (selected.isEmpty()) {
                    if (mode != PollingMode.BALANCED) {
                        commandCounter = 0
                        lastRateCalculationTime = System.nanoTime()
                        _connectionState.update { it.copy(commandsPerSecond = 0f) }
                    }
                    delay(100)
                    continue
                }

                for (request in selected) {
                    if (!isActive || !connection.isConnected() || pollingSelection.value != selection || recordingActive.value != recording || backgroundReading.value != backgroundReadingActive) break
                    yield()
                    try {
                        val rawResponse = commandSequenceMutex.withLock {
                            selectHeader(request.header)
                            connection.sendCommand(request.command)
                        }
                        currentCoroutineContext().ensureActive()
                        commandCounter++
                        val now = System.nanoTime()
                        val diff = (now - lastRateCalculationTime) / 1_000_000
                        if (diff >= 1000L) {
                            _connectionState.update { it.copy(commandsPerSecond = (commandCounter * 1000f) / diff) }
                            commandCounter = 0
                            lastRateCalculationTime = now
                        }
                        Elm327Protocol.diagnoseResponseError(rawResponse)?.let {
                            logDiagnostic("⚠️ [${request.pids.first().shortName}] $it")
                        }
                        val failure = SupportedPidsDetector.responseFailure(request.command, rawResponse)
                        if (failure != null) {
                            val ids = request.pids.map { it.id }.toSet()
                            if (failure == SensorTelemetryStatus.INCOMPATIBLE) {
                                _activePids.update { pids -> pids.map { if (it.id in ids) it.copy(isSupported = false, supportChecked = true) else it } }
                            }
                            if (failure != SensorTelemetryStatus.INCOMPATIBLE) retryAfter[request.header to request.command] = System.nanoTime() + 1_000_000_000L
                            invalidateReadings(ids, failure)
                            continue
                        }
                        val bytes = Elm327Protocol.extractDataBytes(request.command, rawResponse)
                        if (bytes.isNotEmpty()) {
                            val variables = EquationEvaluator.mapBytesToVariables(bytes)
                            // Actualizar también los sensores de la misma trama es gratis para el bus.
                            for (pid in request.pids) {
                                try {
                                    processReading(pid, PidDecoder.decode(pid, variables))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    invalidateReadings(setOf(pid.id), if (e is PidFieldUnavailableException)
                                        SensorTelemetryStatus.FIELD_UNAVAILABLE else SensorTelemetryStatus.INVALID_RESPONSE)
                                }
                            }
                        } else {
                            retryAfter[request.header to request.command] = System.nanoTime() + 1_000_000_000L
                            invalidateReadings(request.pids.map { it.id }.toSet(), SensorTelemetryStatus.INVALID_RESPONSE)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (!connection.isConnected()) {
                            val reason = e.message ?: tr("Socket cerrado", "Socket closed")
                            logDiagnostic(
                                tr(
                                    "❌ Enlace OBD2 perdido: $reason",
                                    "❌ OBD2 link lost: $reason"
                                )
                            )
                            triggerAutoReconnect(tr("Pérdida de enlace con el adaptador", "Lost link with adapter"))
                            break
                        }
                        retryAfter[request.header to request.command] = System.nanoTime() + 1_000_000_000L
                        invalidateReadings(request.pids.map { it.id }.toSet(),
                            if (e is java.io.IOException) SensorTelemetryStatus.NO_RESPONSE else SensorTelemetryStatus.INVALID_RESPONSE)
                        logDiagnostic("❌ ${request.command}: ${e.message}")
                    } finally {
                        if (request == background) backgroundBudget.recordCompletion(System.nanoTime() / 1_000_000)
                    }
                }
                // Solo el simulador necesita limitar su bucle de respuestas en memoria.
                if (_connectionState.value.type == ConnectionType.SIMULATION) delay(20)
            }
            if (isActive && !connection.isConnected()) {
                triggerAutoReconnect(tr("Enlace OBD perdido", "OBD link lost"))
            }
        }
    }

    private fun triggerAutoReconnect(reason: String) {
        // El polling usa IO; las decisiones de conexión se serializan en el scope del gestor.
        scope.launch {
            val state = _connectionState.value
            if (isManuallyDisconnected || state.type == ConnectionType.SIMULATION || state.activeDevice == null) return@launch
            if (connectionJob?.isActive == true || connection.isConnected()) return@launch
            logDiagnostic(reason)
            replaceConnectionJob { connectUntilAvailable() }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        freshnessJob?.cancel()
        freshnessJob = null
        synchronized(telemetryLock) {
            _vehicleState.value = VehicleState(sensorStatuses =
                if (pollingPaused) _activePids.value.associate { it.id to SensorTelemetryStatus.PAUSED } else emptyMap())
            alertEvaluator.reset()
            evaluateAlerts()
        }
    }

    private suspend fun processReading(pid: ObdPid, decodedValue: Double) {
        val profile = _vehicleProfile.value
        val source = _connectionState.value
        val value = decodedValue.toFloat()
        require(decodedValue.isFinite() && value.isFinite()) { tr("Lectura no finita", "Non-finite reading") }
        val reading = ObdReading(
            pidId = pid.id, pidName = pid.name, shortName = pid.shortName,
            value = value, units = pid.units, minValue = pid.minValue, maxValue = pid.maxValue,
            decodedValue = decodedValue, valueLabels = pid.valueLabels, isSimulated = _connectionState.value.type == ConnectionType.SIMULATION
        )
        _activePids.update { pids -> pids.map { if (it.id == pid.id) it.copy(isSupported = true, supportChecked = true) else it } }
        val context = currentCoroutineContext()
        var accepted = false
        synchronized(telemetryLock) {
            _vehicleState.update { state ->
                if (!context.isActive || pollingPaused || _activePids.value.none {
                    it.id == pid.id && it.isEnabled && it.isSupported
                }) return@update state
                val history = (state.sparklineHistory[pid.id].orEmpty() + value).takeLast(40)
                state.copy(
                    readings = state.readings + (pid.id to reading),
                    sparklineHistory = state.sparklineHistory + (pid.id to history),
                    sensorStatuses = state.sensorStatuses - pid.id
                )
            }
            evaluateAlerts()
            accepted = _vehicleState.value.readings[pid.id] === reading
        }
        if (accepted) onReading(reading, pid, profile, source)
    }

    private fun invalidateReadings(ids: Set<String>, reason: SensorTelemetryStatus = SensorTelemetryStatus.WAITING) {
        if (ids.isEmpty()) return
        synchronized(telemetryLock) {
            _vehicleState.update { it.copy(readings = it.readings - ids, sparklineHistory = it.sparklineHistory - ids,
                sensorStatuses = it.sensorStatuses + ids.associateWith { reason }) }
            evaluateAlerts()
        }
    }

    private fun expireReadings() {
        val now = System.nanoTime()
        synchronized(telemetryLock) {
            _vehicleState.update { state ->
                val expired = state.readings.filterValues { now - it.receivedAtNanos >= readingTimeoutMillis * 1_000_000L }.keys
                if (expired.isEmpty()) state else state.copy(
                    readings = state.readings - expired, sparklineHistory = state.sparklineHistory - expired,
                    sensorStatuses = state.sensorStatuses + expired.associateWith { SensorTelemetryStatus.STALE }
                )
            }
            evaluateAlerts()
        }
    }
}
