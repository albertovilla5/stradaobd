package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.ObdReading
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.parser.EquationEvaluator
import com.torqueobd2.dash.data.parser.SupportedPidsDetector
import com.torqueobd2.dash.data.repository.DefaultPids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gestor central de OBD2: coordina la conexión física o simulada,
 * ejecuta el ciclo de lectura (polling) de PIDs soportados, evalúa ecuaciones matemáticas,
 * procesa la física del vehículo según el perfil configurado y actualiza el estado unificado.
 */
class ObdManager(
    private val scope: CoroutineScope,
    initialProfile: VehicleProfile = VehicleProfile()
) {
    private var connection: ObdConnection = SimulatedObdConnection()
    private var pollingJob: Job? = null

    private val _connectionState = MutableStateFlow(
        ConnectionState(
            type = ConnectionType.SIMULATION,
            status = ConnectionStatus.CONNECTED,
            activeDevice = DeviceItem("Simulador Automotriz", "local"),
            statusMessage = "Modo simulación en ejecución"
        )
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _vehicleProfile = MutableStateFlow(initialProfile)
    val vehicleProfile: StateFlow<VehicleProfile> = _vehicleProfile.asStateFlow()

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _activePids = MutableStateFlow<List<ObdPid>>(emptyList())
    val activePids: StateFlow<List<ObdPid>> = _activePids.asStateFlow()

    private val sparklineHistory = mutableMapOf<String, MutableList<Float>>()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(
        listOf(
            "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] Sistema OBD2 iniciado en modo simulación."
        )
    )
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    fun logDiagnostic(entry: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$time] $entry"
        val list = _diagnosticLogs.value.toMutableList()
        list.add(formatted)
        if (list.size > 200) {
            list.removeAt(0)
        }
        _diagnosticLogs.value = list
    }

    fun clearDiagnosticLogs() {
        _diagnosticLogs.value = emptyList()
    }

    // Conteo de comandos por segundo
    private var commandCounter = 0
    private var lastRateCalculationTime = System.currentTimeMillis()

    init {
        val initialPids = mutableListOf<ObdPid>()
        initialPids.addAll(DefaultPids.getStandardPids())
        initialPids.addAll(DefaultPids.getHondaIdtecPids())
        _activePids.value = initialPids

        startSimulation()
    }

    fun updateProfile(newProfile: VehicleProfile) {
        _vehicleProfile.value = newProfile
        // Recalcular métricas dependientes del perfil
        recalculateVehicleDerivedMetrics()
    }

    fun setPids(pids: List<ObdPid>) {
        _activePids.value = pids
    }

    fun addPids(newPids: List<ObdPid>) {
        val current = _activePids.value.toMutableList()
        for (p in newPids) {
            val existingIdx = current.indexOfFirst { it.cleanObdCommand() == p.cleanObdCommand() && it.name == p.name }
            if (existingIdx >= 0) {
                current[existingIdx] = p
            } else {
                current.add(p)
            }
        }
        _activePids.value = current
    }

    fun togglePid(pidId: String, isEnabled: Boolean) {
        val updated = _activePids.value.map {
            if (it.id == pidId) it.copy(isEnabled = isEnabled) else it
        }
        _activePids.value = updated
    }

    fun setManualThrottle(percent: Float?) {
        (connection as? SimulatedObdConnection)?.setManualThrottle(percent)
    }

    /**
     * Escanea activamente la ECU del coche para identificar qué PIDs están soportados
     * mediante las tramas 0100, 0120, etc.
     */
    suspend fun scanSupportedPids(): Map<String, Boolean> {
        val supportedHexPids = mutableSetOf<String>()
        val extendedResults = mutableMapOf<String, Boolean>()

        if (!connection.isConnected()) return emptyMap()

        // 1. Escanear PIDs estándar 0100 y 0120
        val resp0100 = connection.sendCommand("0100")
        supportedHexPids.addAll(SupportedPidsDetector.parseSupportedPidsRange("0100", resp0100))

        if (supportedHexPids.contains("20")) {
            val resp0120 = connection.sendCommand("0120")
            supportedHexPids.addAll(SupportedPidsDetector.parseSupportedPidsRange("0120", resp0120))
        }

        // 2. Comprobar PIDs extendidos Modo 22
        val mode22Pids = _activePids.value.filter { it.cleanObdCommand().startsWith("22") }
        for (p in mode22Pids) {
            val resp = connection.sendCommand(p.cleanObdCommand())
            val ok = !SupportedPidsDetector.isResponseUnsupported(resp)
            extendedResults[p.cleanObdCommand()] = ok
        }

        // 3. Actualizar la lista en memoria
        _activePids.value = SupportedPidsDetector.updatePidsSupport(
            _activePids.value,
            supportedHexPids,
            extendedResults
        )

        return _activePids.value.associate { it.name to it.isSupported }
    }

    fun startSimulation() {
        stopPolling()
        scope.launch {
            connection.disconnect()
            val sim = SimulatedObdConnection()
            connection = sim
            _connectionState.value = _connectionState.value.copy(
                type = ConnectionType.SIMULATION,
                activeDevice = DeviceItem("Simulador Honda i-DTEC", "local"),
                status = ConnectionStatus.CONNECTING,
                statusMessage = "Iniciando simulación..."
            )
            val ok = sim.connect()
            if (ok) {
                _connectionState.value = _connectionState.value.copy(
                    status = ConnectionStatus.CONNECTED,
                    statusMessage = "Simulación activa (100% interactiva)"
                )
                startPolling()
            }
        }
    }

    fun connectBluetooth(device: DeviceItem) {
        stopPolling()
        scope.launch {
            logDiagnostic("🔌 Solicitando conexión Bluetooth a ${device.name} [${device.address}]...")
            connection.disconnect()
            val bt = BluetoothObdConnection(
                deviceAddress = device.address,
                customInitCommands = _connectionState.value.customInitCommands,
                onLog = { logDiagnostic(it) }
            )
            connection = bt
            _connectionState.value = _connectionState.value.copy(
                type = ConnectionType.BLUETOOTH,
                activeDevice = device,
                status = ConnectionStatus.CONNECTING,
                statusMessage = "Conectando a ${device.name}..."
            )
            val ok = bt.connect()
            if (ok) {
                _connectionState.value = _connectionState.value.copy(
                    status = ConnectionStatus.CONNECTED,
                    statusMessage = "Conectado a ${device.name}"
                )
                logDiagnostic("🔍 Realizando escaneo inicial de PIDs soportados...")
                scanSupportedPids()
                startPolling()
            } else {
                val err = bt.statusMessage.value
                _connectionState.value = _connectionState.value.copy(
                    status = ConnectionStatus.ERROR,
                    statusMessage = err
                )
                logDiagnostic("❌ Falló la conexión con ${device.name}: $err")
            }
        }
    }

    fun connectWifi(host: String, port: Int) {
        stopPolling()
        scope.launch {
            connection.disconnect()
            val wifi = WifiObdConnection(host, port, _connectionState.value.customInitCommands)
            connection = wifi
            _connectionState.value = _connectionState.value.copy(
                type = ConnectionType.WIFI,
                activeDevice = DeviceItem("WiFi ELM327", "$host:$port"),
                status = ConnectionStatus.CONNECTING,
                statusMessage = "Conectando a $host:$port..."
            )
            val ok = wifi.connect()
            if (ok) {
                _connectionState.value = _connectionState.value.copy(
                    status = ConnectionStatus.CONNECTED,
                    statusMessage = "Conectado vía WiFi"
                )
                scanSupportedPids()
                startPolling()
            } else {
                _connectionState.value = _connectionState.value.copy(
                    status = ConnectionStatus.ERROR,
                    statusMessage = "Fallo al conectar WiFi a $host:$port"
                )
            }
        }
    }

    fun disconnect() {
        stopPolling()
        scope.launch {
            connection.disconnect()
            _connectionState.value = _connectionState.value.copy(
                status = ConnectionStatus.DISCONNECTED,
                statusMessage = "Desconectado"
            )
        }
    }

    suspend fun sendManualCommand(command: String): String {
        return connection.sendCommand(command)
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            var currentHeader = ""

            while (isActive && connection.isConnected()) {
                val profile = _vehicleProfile.value
                // Filtrar según PIDs habilitados y si se ocultan los no soportados
                val enabledPids = _activePids.value.filter { pid ->
                    pid.isEnabled && (!profile.hideUnsupportedSensors || pid.isSupported)
                }

                if (enabledPids.isEmpty()) {
                    delay(200)
                    continue
                }

                for (pid in enabledPids) {
                    if (!isActive || !connection.isConnected()) break

                    try {
                        if (pid.header.isNotBlank() && pid.header != currentHeader) {
                            connection.sendCommand("AT SH ${pid.header}")
                            currentHeader = pid.header
                        }

                        val rawResponse = connection.sendCommand(pid.cleanObdCommand())
                        commandCounter++

                        val now = System.currentTimeMillis()
                        val diff = now - lastRateCalculationTime
                        if (diff >= 1000L) {
                            val rate = (commandCounter * 1000f) / diff
                            _connectionState.value = _connectionState.value.copy(commandsPerSecond = rate)
                            commandCounter = 0
                            lastRateCalculationTime = now
                        }

                        // Diagnóstico de respuestas erróneas de la ECU
                        val diagError = Elm327Protocol.diagnoseResponseError(rawResponse)
                        if (diagError != null) {
                            logDiagnostic("⚠️ [${pid.shortName}] $diagError")
                        }

                        // Verificar si el PID responde "NO DATA" para marcarlo como no soportado
                        if (SupportedPidsDetector.isResponseUnsupported(rawResponse)) {
                            markPidUnsupported(pid.id)
                            continue
                        }

                        val dataBytes = Elm327Protocol.extractDataBytes(pid.cleanObdCommand(), rawResponse)
                        if (dataBytes.isNotEmpty()) {
                            val vars = EquationEvaluator.mapBytesToVariables(dataBytes)
                            val evaluatedVal = EquationEvaluator.evaluate(pid.equation, vars).toFloat()

                            processReading(pid, evaluatedVal)
                        }
                    } catch (e: Exception) {
                        if (!connection.isConnected()) {
                            _connectionState.value = _connectionState.value.copy(
                                status = ConnectionStatus.ERROR,
                                statusMessage = "Se perdió la comunicación con el adaptador OBD2"
                            )
                            logDiagnostic("❌ Conexión Bluetooth perdida: ${e.message ?: "Socket desconectado"}")
                            break
                        }
                    }
                    delay(30)
                }
            }
        }
    }

    private fun markPidUnsupported(pidId: String) {
        val updated = _activePids.value.map {
            if (it.id == pidId) it.copy(isSupported = false) else it
        }
        _activePids.value = updated
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun processReading(pid: ObdPid, value: Float) {
        val currentReadings = _vehicleState.value.readings.toMutableMap()
        val reading = ObdReading(
            pidId = pid.id,
            pidName = pid.name,
            shortName = pid.shortName,
            value = value,
            units = pid.units,
            minValue = pid.minValue,
            maxValue = pid.maxValue
        )
        currentReadings[pid.shortName] = reading

        val historyList = sparklineHistory.getOrPut(pid.shortName) { mutableListOf() }
        historyList.add(value)
        if (historyList.size > 40) {
            historyList.removeAt(0)
        }

        val currentState = _vehicleState.value
        var updated = currentState.copy(
            readings = currentReadings,
            sparklineHistory = sparklineHistory.mapValues { it.value.toList() }
        )

        when (pid.shortName) {
            "RPM" -> {
                val estGear = when {
                    updated.speedKmh < 5f -> 1
                    value < 100f -> 1
                    else -> {
                        val ratio = updated.speedKmh / (value / 1000f)
                        when {
                            ratio < 11f -> 1
                            ratio < 18f -> 2
                            ratio < 26f -> 3
                            ratio < 36f -> 4
                            ratio < 46f -> 5
                            else -> 6
                        }
                    }
                }
                updated = updated.copy(rpm = value, gearEstimated = estGear)
            }
            "Velocidad" -> updated = updated.copy(speedKmh = value)
            "Temp Liq" -> updated = updated.copy(coolantTempC = value)
            "Carga" -> updated = updated.copy(engineLoadPercent = value)
            "Acelerador" -> updated = updated.copy(throttlePercent = value)
            "MAP" -> {
                val boost = (value / 100f - 1.0f + _vehicleProfile.value.boostPressureAdjustmentBar).coerceAtLeast(0f)
                updated = updated.copy(boostPressureBar = boost)
            }
            "Temp Adm" -> updated = updated.copy(intakeAirTempC = value)
            "MAF" -> updated = updated.copy(mafFlowGramsSec = value)
            "Nivel Dep" -> updated = updated.copy(fuelLevelPercent = value)
            "Voltaje" -> updated = updated.copy(batteryVoltage = value)
            "Consumo" -> updated = updated.copy(fuelRateLh = value)

            // DPF Honda i-DTEC
            "DPF dist a/reg" -> updated = updated.copy(dpfDistanceLastRegenKm = value)
            "DPF fuel a/reg" -> updated = updated.copy(dpfFuelLastRegenL = value)
            "DPF time a/reg" -> updated = updated.copy(dpfTimeLastRegenSec = value)
            "DPF diff.pres" -> {
                val warn = value > 25.0f
                updated = updated.copy(dpfDifferentialPressureKpa = value, dpfWarningLight = warn)
            }
            "EGT" -> updated = updated.copy(exhaustGasTemp1C = value)
            "DPF calc PM" -> {
                val warn = value > 30.0f
                updated = updated.copy(dpfCalculatedPmGrams = value, dpfWarningLight = warn || updated.dpfWarningLight)
            }
            "DPF reg.state" -> updated = updated.copy(dpfRegenStatus = value.toLong())
            "DPF active reg" -> updated = updated.copy(dpfActiveRegen = value > 0f)
            "Eng.oil lvl f" -> {
                val warn = value > 115f || value < 80f
                updated = updated.copy(engineOilLevelMm = value, oilWarningLight = warn)
            }
            "DPF calc.flow" -> updated = updated.copy(dpfExhaustFlowM3h = value)
        }

        _vehicleState.value = updated
        recalculateVehicleDerivedMetrics()
    }

    /**
     * Aplica los modelos matemáticos de Torque Pro para calcular la potencia, par,
     * combustible restante y autonomía basados en el perfil del vehículo.
     */
    private fun recalculateVehicleDerivedMetrics() {
        val s = _vehicleState.value
        val p = _vehicleProfile.value

        // 1. Litros de combustible restantes en el depósito
        val remainingLiters = p.fuelTankCapacityLiters * (s.fuelLevelPercent / 100f)

        // 2. Consumo en L/100km (si no se lee por PID directo)
        val l100km = if (s.speedKmh > 5f && s.fuelRateLh > 0.1f) {
            (s.fuelRateLh / s.speedKmh) * 100f
        } else if (s.fuelConsumptionL100km > 0.1f) {
            s.fuelConsumptionL100km
        } else {
            5.8f // Consumo medio de referencia para 2.2 i-DTEC
        }

        // 3. Autonomía estimada restante en km
        val rangeKm = if (l100km > 0.5f) {
            (remainingLiters / l100km) * 100f
        } else 600f

        // 4. Potencia estimada (CV / HP)
        // Fórmula Speed-Density / Carga / MAF:
        // Potencia = (Carga% / 100) * (Cilindrada en L) * (RPM / 1000) * Factor según combustible y turbo
        val boostFactor = 1.0f + (s.boostPressureBar * 0.8f)
        val fuelEfficiencyFactor = if (p.fuelType == com.torqueobd2.dash.data.model.FuelType.DIESEL) 16.5f else 14.0f
        val estHp = ((s.engineLoadPercent / 100f) * p.engineDisplacementLiters * (s.rpm / 1000f) * fuelEfficiencyFactor * boostFactor * (p.volumetricEfficiencyPercent / 100f)).coerceAtLeast(0f)

        // 5. Par motor estimado (Nm) = (CV * 7024) / RPM
        val estTorque = if (s.rpm > 200f) {
            (estHp * 7024f) / s.rpm
        } else 0f

        _vehicleState.value = s.copy(
            fuelRemainingLiters = remainingLiters,
            estimatedRangeKm = rangeKm,
            estimatedHorsepowerCv = estHp,
            estimatedTorqueNm = estTorque,
            fuelConsumptionL100km = l100km
        )
    }
}
