package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ObdManagerTest {
    private class RecordingAdapter(val reply: suspend (String) -> String) : ObdConnection {
        override val status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        override val statusMessage = MutableStateFlow("Prueba")
        val commands = mutableListOf<String>()
        var stopBeforeRepeat: String? = null
        var allowedOccurrences = 1
        override suspend fun connect(): Boolean {
            status.value = ConnectionStatus.CONNECTED
            return true
        }
        override suspend fun disconnect() { status.value = ConnectionStatus.DISCONNECTED }
        override suspend fun sendCommand(command: String): String {
            if (command == stopBeforeRepeat && commands.count { it == command } >= allowedOccurrences) awaitCancellation()
            commands.add(command)
            return reply(command)
        }
        override fun isConnected() = status.value == ConnectionStatus.CONNECTED
    }

    private class Adapter(private val connects: Boolean = true) : ObdConnection {
        override val status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        override val statusMessage = MutableStateFlow("No disponible")
        var disconnects = 0
        var loseLinkOnReading = false
        override suspend fun connect(): Boolean {
            status.value = if (connects) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
            return connects
        }
        override suspend fun disconnect() {
            disconnects++
            status.value = ConnectionStatus.DISCONNECTED
        }
        override suspend fun sendCommand(command: String): String {
            if (loseLinkOnReading) {
                status.value = ConnectionStatus.DISCONNECTED
                throw IOException("Enlace perdido")
            }
            return "NO DATA\r>"
        }
        override fun isConnected() = status.value == ConnectionStatus.CONNECTED
    }

    private val firstDevice = DeviceItem("OBD A", "AA:BB:CC:DD:EE:01")
    private val secondDevice = DeviceItem("OBD B", "AA:BB:CC:DD:EE:02")

    private fun manager(scope: CoroutineScope, saved: (ConnectionState) -> Unit = {},
                        available: (ConnectionType) -> Boolean = { true }, timeoutMillis: Long = 5000L,
                        factory: (ConnectionState) -> ObdConnection) = ObdManager(
        scope, initialProfile = VehicleProfile(customInitCommands = ""),
        initialPollingMode = PollingMode.ALL, onRealConnectionEstablished = saved, canConnect = available,
        ioDispatcher = Dispatchers.Unconfined, connectionFactory = factory, retryDelayMillis = { 1L }, readingTimeoutMillis = timeoutMillis
    )

    @Test
    fun startsDisconnectedWithoutSimulationOrFakeTelemetry() = runBlocking {
        var creations = 0
        val manager = manager(this) { creations++; Adapter() }
        assertEquals(ConnectionStatus.DISCONNECTED, manager.connectionState.value.status)
        assertEquals(ConnectionType.BLUETOOTH, manager.connectionState.value.type)
        assertEquals(0f, manager.connectionState.value.commandsPerSecond)
        assertNull(manager.connectionState.value.activeDevice)
        assertTrue(manager.vehicleState.value.readings.isEmpty())
        assertTrue(manager.vehicleState.value.batteryVoltage.isNaN())
        assertTrue(manager.vehicleState.value.dpfCalculatedPmGrams.isNaN())
        assertEquals("ERROR: NOT CONNECTED", manager.sendManualCommand("010C"))
        assertEquals(0, creations)
        manager.close()
    }

    @Test
    fun keepsRetryingPastTenFailuresAndSavesOnlySuccessfulDevice() = runBlocking {
        var attempts = 0
        val saved = mutableListOf<ConnectionState>()
        val adapters = mutableListOf<Adapter>()
        val manager = manager(this, saved = { saved.add(it) }) {
            Adapter(connects = ++attempts > 11).also { adapters.add(it) }
        }
        manager.pausePolling()
        manager.connectBluetooth(firstDevice)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        assertEquals(12, attempts)
        assertEquals(listOf(firstDevice), saved.map { it.activeDevice })
        assertTrue(adapters.dropLast(1).all { it.disconnects > 0 })
        manager.close()
    }

    @Test
    fun choosingAnotherDeviceCancelsRetriesAndManualDisconnectStopsThem() = runBlocking {
        val attempts = mutableListOf<String>()
        val manager = manager(this) {
            attempts.add(it.activeDevice!!.address)
            Adapter(connects = it.activeDevice == secondDevice)
        }
        manager.pausePolling()
        manager.connectBluetooth(firstDevice)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.RECONNECTING } }
        manager.connectBluetooth(secondDevice)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        val lastFirstAttempt = attempts.indexOfLast { it == firstDevice.address }
        assertEquals(0, lastFirstAttempt)
        manager.disconnect()
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.DISCONNECTED } }
        val count = attempts.size
        manager.resumePolling()
        delay(20)
        assertEquals(count, attempts.size)
        manager.close()
    }

    @Test
    fun waitsForBluetoothPermissionsOrRadioWithoutCreatingSockets() = runBlocking {
        var available = false
        var attempts = 0
        val manager = manager(this, available = { available }) { attempts++; Adapter() }
        manager.pausePolling()
        manager.connectBluetooth(firstDevice)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.RECONNECTING } }
        assertEquals(0, attempts)
        available = true
        withTimeout(4000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        assertEquals(1, attempts)
        manager.close()
    }

    @Test
    fun lostLinkFromTerminalReconnectsToSameDevice() = runBlocking {
        val adapters = mutableListOf<Adapter>()
        val manager = manager(this) { Adapter().also { adapters.add(it) } }
        manager.pausePolling()
        manager.connectBluetooth(firstDevice)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        // CONNECTED se publica antes del escaneo, esperar a que el job de conexión termine.
        yield()
        adapters.last().loseLinkOnReading = true
        assertTrue(manager.sendManualCommand("010C").startsWith("ERROR:"))
        withTimeout(2000) {
            while (adapters.size < 2) delay(1)
            manager.connectionState.first { it.status == ConnectionStatus.CONNECTED }
        }
        assertEquals(firstDevice, manager.connectionState.value.activeDevice)
        manager.close()
    }

    @Test
    fun switchingDuringHandshakeWaitsForCancelledSocketToClose() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val manager = manager(this) { target ->
            if (target.activeDevice == firstDevice) object : ObdConnection {
                override val status = MutableStateFlow(ConnectionStatus.CONNECTING)
                override val statusMessage = MutableStateFlow("Conectando")
                override suspend fun connect(): Boolean {
                    entered.complete(Unit)
                    try { awaitCancellation() } finally {
                        withContext(NonCancellable) { delay(10); closed.complete(Unit) }
                    }
                }
                override suspend fun disconnect() { }
                override suspend fun sendCommand(command: String) = ""
                override fun isConnected() = false
            } else {
                assertTrue("El intento previo debe haberse cerrado", closed.isCompleted)
                Adapter()
            }
        }
        manager.pausePolling()
        manager.connectBluetooth(firstDevice)
        withTimeout(2000) { entered.await() }
        // Dos selecciones consecutivas comprueban también la cancelación de un job todavía pendiente.
        manager.connectBluetooth(secondDevice)
        manager.connectWifi("192.168.0.10", 35000)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        assertEquals(ConnectionType.WIFI, manager.connectionState.value.type)
        manager.close()
    }

    @Test
    fun simulationIsExplicitAndNeverOverwritesRememberedRealAdapter() = runBlocking {
        val saved = mutableListOf<ConnectionState>()
        val manager = manager(this, saved = { saved.add(it) }) { Adapter() }
        manager.pausePolling()
        manager.connectWifi("192.168.0.20", 35000)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        manager.startSimulation()
        withTimeout(2000) { manager.connectionState.first { it.type == ConnectionType.SIMULATION && it.status == ConnectionStatus.CONNECTED } }
        assertEquals(1, saved.size)
        assertEquals("192.168.0.20", saved.single().wifiHost)
        manager.disconnect()
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.DISCONNECTED } }
        assertEquals(ConnectionType.BLUETOOTH, manager.connectionState.value.type)
        assertTrue(manager.vehicleState.value.readings.isEmpty())
        manager.close()
    }

    @Test
    fun sharedResponseUpdatesEveryEquationWithoutSendingCalculatedPids() = runBlocking {
        val adapter = RecordingAdapter { command ->
            when (command) {
                "222512" -> "62 25 12 0A 14\r>"
                "010C" -> "41 0C 1A F8\r>"
                "010D" -> "41 0D 32\r>"
                else -> "?\r>"
            }
        }
        adapter.stopBeforeRepeat = "222512"
        val manager = manager(this) { adapter }
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A"),
            ObdPid(id = "SAE_010C_RPM", name = "RPM", shortName = "RPM", modeAndPid = "010C", equation = "((A*256)+B)/4"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "0x222512", equation = "B*2"),
            ObdPid(id = "Potencia", name = "Potencia", shortName = "Potencia", modeAndPid = "CALC_HP", equation = "HP"),
            ObdPid(id = "Par", name = "Par", shortName = "Par", modeAndPid = "CALC_TORQUE", equation = "NM"),
            ObdPid(id = "SAE_010D_Velocidad", name = "Velocidad", shortName = "Velocidad", modeAndPid = "010D", equation = "A")
        ))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { it.speedKmh == 50f } }
            manager.pausePolling()
            assertEquals(listOf("222512", "010C", "010D"), adapter.commands)
            assertEquals(10f, state.readings.getValue("Uno").value)
            assertEquals(40f, state.readings.getValue("Dos").value)
            assertEquals(1726f, state.rpm)
            assertFalse(state.readings.containsKey("Potencia"))
            assertFalse(state.readings.containsKey("Par"))
        } finally { manager.close() }
    }

    @Test
    fun responseCacheDistinguishesEcusAndRestoresAutomaticHeader() = runBlocking {
        var header = ""
        val adapter = RecordingAdapter { command ->
            when {
                command == "AT D" -> { header = ""; "AT D\rOK\r>" }
                command.startsWith("AT SH") -> { header = command.substringAfter("AT SH "); "OK\r>" }
                command.startsWith("AT") -> "OK\r>"
                else -> "62 25 12 ${when (header) { "7E0" -> "0A"; "7E1" -> "14"; else -> "1E" }}\r>"
            }
        }
        adapter.stopBeforeRepeat = "AT SH 7E0"
        val manager = manager(this) { adapter }
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A", header = "7E0"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "222512", equation = "A", header = "7E1"),
            ObdPid(id = "Tres", name = "Tres", shortName = "Tres", modeAndPid = "222512", equation = "A")
        ))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { "Tres" in it.readings } }
            manager.pausePolling()
            assertEquals(listOf("AT SH 7E0", "222512", "AT SH 7E1", "222512",
                "AT D", "AT E0", "AT L0", "AT S0", "AT H0", "AT AT1", "AT SP0", "222512"), adapter.commands)
            assertEquals(10f, state.readings.getValue("Uno").value)
            assertEquals(20f, state.readings.getValue("Dos").value)
            assertEquals(30f, state.readings.getValue("Tres").value)
        } finally { manager.close() }
    }

    @Test fun sharedNoDataRetainsUnknownSupportAndRecoversOnNextQuery() = runBlocking {
        var calls = 0
        val adapter = RecordingAdapter { if (++calls == 1) "NO DATA\r>" else "62 25 12 0A 14\r>" }
        adapter.stopBeforeRepeat = "222512"
        adapter.allowedOccurrences = 2
        val manager = manager(this) { adapter }
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "222512", equation = "B")
        ))
        try {
            manager.startSimulation()
            val missing = withTimeout(2000) { manager.vehicleState.first { it.sensorStatuses["Uno"] == SensorTelemetryStatus.NO_RESPONSE } }
            assertTrue(missing.readings.isEmpty())
            assertTrue(manager.activePids.value.all { it.isSupported && !it.supportChecked })
            val recovered = withTimeout(2000) { manager.vehicleState.first { "Dos" in it.readings } }
            assertEquals(10f, recovered.readings.getValue("Uno").value)
            assertEquals(20f, recovered.readings.getValue("Dos").value)
            assertTrue(recovered.sensorStatuses.isEmpty())
            assertEquals(listOf("222512", "222512"), adapter.commands)
        } finally { manager.close() }
    }

    @Test
    fun responseCacheExpiresAfterEveryPollingCycle() = runBlocking {
        var value = 0
        val adapter = RecordingAdapter { "62 25 12 ${"%02X".format(++value)}\r>" }
        adapter.stopBeforeRepeat = "222512"
        adapter.allowedOccurrences = 2
        val manager = manager(this) { adapter }
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "222512", equation = "A")
        ))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { it.readings["Dos"]?.value == 2f } }
            manager.pausePolling()
            assertEquals(listOf("222512", "222512"), adapter.commands)
            assertEquals(2f, state.readings.getValue("Uno").value)
            assertEquals(2f, state.readings.getValue("Dos").value)
        } finally { manager.close() }
    }

    @Test
    fun supportScanQueriesSharedExtendedPidOnce() = runBlocking {
        val adapter = RecordingAdapter { command ->
            if (command == "0100") "41 00 00 00 00 00\r>" else "62 25 12 0A 14\r>"
        }
        val manager = manager(this) { adapter }
        manager.pausePolling()
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "222512", equation = "B")
        ))
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            yield()
            assertEquals(listOf("0100", "222512"), adapter.commands)
            assertTrue(manager.activePids.value.all { it.isSupported })
        } finally { manager.close() }
    }

    @Test
    fun visibleModeOnlyQueriesShownPayloadAndUpdatesRelatedSensorsForFree() = runBlocking {
        val adapter = RecordingAdapter { if (it == "222512") "62 25 12 0A 14\r>" else "41 0C 1A F8\r>" }
        adapter.stopBeforeRepeat = "222512"
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.setVisibleSensors(setOf("Uno"))
        manager.setPids(listOf(
            ObdPid(id = "Uno", name = "Uno", shortName = "Uno", modeAndPid = "222512", equation = "A"),
            ObdPid(id = "Dos", name = "Dos", shortName = "Dos", modeAndPid = "222512", equation = "B"),
            ObdPid(id = "SAE_010C_RPM", name = "RPM", shortName = "RPM", modeAndPid = "010C", equation = "A")
        ))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { "Dos" in it.readings } }
            manager.pausePolling()
            assertEquals(listOf("222512"), adapter.commands)
            assertEquals(20f, state.readings.getValue("Dos").value)
            assertFalse("SAE_010C_RPM" in state.readings)
        } finally { manager.close() }
    }

    @Test
    fun powerWidgetNeverManufacturesPowerOrQueriesApproximationInputs() = runBlocking {
        val adapter = RecordingAdapter { "NO DATA\r>" }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.setVisibleSensors(setOf("Potencia", "Par"))
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            delay(50)
            assertTrue(adapter.commands.isEmpty())
            assertTrue(manager.vehicleState.value.readings.isEmpty())
            assertTrue(manager.vehicleState.value.horsepowerCv.isNaN())
            assertTrue(manager.vehicleState.value.torqueNm.isNaN())
        } finally { manager.close() }
    }

    @Test
    fun userAlertsWatchHiddenSensorsInVisibleAndBalancedModesAndPauseClearsThem() = runBlocking {
        for (mode in listOf(PollingMode.VISIBLE, PollingMode.BALANCED)) {
            val adapter = RecordingAdapter { "41 05 AA\r>" }.also { it.stopBeforeRepeat = "0105" }
            val manager = manager(this) { adapter }
            val pid = ObdPid(id = "temperature", name = "Temperatura", shortName = "Temp", modeAndPid = "0105", equation = "A-40", units = "°C")
            manager.setPids(listOf(pid))
            manager.setPollingMode(mode)
            manager.setVisibleSensors(emptySet())
            manager.setSensorAlertRules(listOf(SensorAlertRule(vehicleId = manager.vehicleProfile.value.id,
                pidId = pid.id, sensorName = pid.name, units = pid.units, upperLimit = 100.0, durationSeconds = 0)))
            try {
                manager.startSimulation()
                val alerts = withTimeout(2000) { manager.sensorAlerts.first { it.any { alert -> alert.status == SensorAlertStatus.ACTIVE } } }
                assertEquals(listOf("0105"), adapter.commands)
                assertEquals(130.0, alerts.single().reading!!.decodedValue, 0.0)
                assertTrue(alerts.single().reading!!.isSimulated)
                manager.pausePolling()
                assertEquals(SensorAlertStatus.UNAVAILABLE, manager.sensorAlerts.value.single().status)
                assertEquals("Lectura pausada", manager.sensorAlerts.value.single().detail)
            } finally { manager.close() }
        }
    }

    @Test
    fun disabledAlertsAndRulesForOtherVehiclesDoNotPollHiddenSensors() = runBlocking {
        val adapter = RecordingAdapter { "41 05 AA\r>" }
        val manager = manager(this) { adapter }
        val pid = ObdPid(id = "temperature", name = "Temp", shortName = "Temp", modeAndPid = "0105", equation = "A-40", units = "°C")
        manager.setPids(listOf(pid))
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.setVisibleSensors(emptySet())
        val rule = SensorAlertRule(vehicleId = manager.vehicleProfile.value.id, pidId = pid.id,
            sensorName = pid.name, units = pid.units, upperLimit = 100.0, enabled = false)
        manager.setSensorAlertRules(listOf(rule, rule.copy(id = "other", vehicleId = "another-car", enabled = true)))
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            delay(150)
            assertTrue(adapter.commands.isEmpty())
            assertEquals(SensorAlertStatus.DISABLED, manager.sensorAlerts.value.single().status)
            manager.updateProfile(manager.vehicleProfile.value.copy(id = "another-car"))
            assertEquals("other", manager.sensorAlerts.value.single().rule.id)
            assertEquals(SensorAlertStatus.UNAVAILABLE, manager.sensorAlerts.value.single().status)
        } finally { manager.close() }
    }

    @Test
    fun replacingASensorEquationCannotKeepAnAlertBasedOnThePreviousDefinition() = runBlocking {
        val adapter = RecordingAdapter { "41 05 AA\r>" }.also { it.stopBeforeRepeat = "0105" }
        val manager = manager(this) { adapter }
        val pid = ObdPid(id = "temperature", name = "Temp", shortName = "Temp", modeAndPid = "0105", equation = "A-40", units = "°C")
        manager.setPids(listOf(pid))
        manager.setSensorAlertRules(listOf(SensorAlertRule(vehicleId = manager.vehicleProfile.value.id, pidId = pid.id,
            sensorName = pid.name, units = pid.units, upperLimit = 100.0, durationSeconds = 0)))
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.sensorAlerts.first { it.single().status == SensorAlertStatus.ACTIVE } }
            manager.setPids(listOf(pid.copy(equation = "A-100")))
            assertFalse(pid.id in manager.vehicleState.value.readings)
            assertEquals(SensorAlertStatus.UNAVAILABLE, manager.sensorAlerts.value.single().status)
        } finally { manager.close() }
    }

    @Test
    fun navigationDuringResponseFinishesCurrentCommandThenUsesNewScreen() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val adapter = RecordingAdapter {
            if (it == "010C") {
                entered.complete(Unit)
                release.await()
                "41 0C 1A F8\r>"
            } else "41 05 7D\r>"
        }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.setVisibleSensors(SensorIds.driving - setOf(SensorIds.COOLANT, SensorIds.VOLTAGE))
        try {
            manager.startSimulation()
            withTimeout(2000) { entered.await() }
            manager.setVisibleSensors(setOf("SAE_0105_Temp Liq"))
            release.complete(Unit)
            withTimeout(2000) { manager.vehicleState.first { "SAE_0105_Temp Liq" in it.readings } }
            manager.pausePolling()
            assertEquals(listOf("010C", "0105"), adapter.commands)
        } finally { manager.close() }
    }

    @Test
    fun emptyVisibleScreenSendsNoQueriesAndTerminalStillWorks() = runBlocking {
        val adapter = RecordingAdapter { "ELM327\r>" }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.setVisibleSensors(emptySet())
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            delay(50)
            assertTrue(adapter.commands.isEmpty())
            assertEquals("ELM327\r>", manager.sendManualCommand("ATI"))
            manager.pausePolling()
            assertEquals(listOf("ATI"), adapter.commands)
        } finally { manager.close() }
    }

    @Test
    fun balancedModeLimitsBackgroundInsteadOfPollingEveryHiddenSensor() = runBlocking {
        val adapter = RecordingAdapter {
            when (it) {
                "010C" -> "41 0C 1A F8\r>"
                "222512" -> "62 25 12 0A\r>"
                else -> "NO DATA\r>"
            }
        }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.BALANCED)
        manager.setVisibleSensors(setOf("Visible"))
        manager.setPids(listOf(
            ObdPid(id = "SAE_010C_RPM", name = "RPM", shortName = "RPM", modeAndPid = "010C", equation = "A"),
            ObdPid(id = "SAE_0105_Temp Liq", name = "Refrigerante", shortName = "Temp Liq", modeAndPid = "0105", equation = "A"),
            ObdPid(id = "SAE_0142_Voltaje", name = "Voltaje", shortName = "Voltaje", modeAndPid = "0142", equation = "A"),
            ObdPid(id = "Ajeno", name = "Ajeno", shortName = "Ajeno", modeAndPid = "222510", equation = "A"),
            ObdPid(id = "Visible", name = "Visible", shortName = "Visible", modeAndPid = "222512", equation = "A")
        ))
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.vehicleState.first { "SAE_010C_RPM" in it.readings } }
            delay(100)
            manager.pausePolling()
            assertTrue(adapter.commands.count { it == "222512" } > 1)
            assertEquals(1, adapter.commands.count { it == "010C" })
            assertFalse(adapter.commands.any { it in setOf("0105", "0142", "222510") })
        } finally { manager.close() }
    }

    @Test
    fun lightweightConnectionSkipsExtendedScanUntilExplicitlyRequested() = runBlocking {
        val adapter = RecordingAdapter { if (it == "0100") "41 00 00 10 00 00\r>" else "62 25 12 0A\r>" }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.pausePolling()
        manager.setPids(listOf(ObdPid(id = "DPF", name = "DPF", shortName = "DPF", modeAndPid = "222512", equation = "A", isSupported = false)))
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            yield()
            assertEquals(listOf("0100"), adapter.commands)
            assertTrue(manager.activePids.value.single().isSupported)
            assertEquals(mapOf("DPF" to true), manager.scanSupportedPids())
            assertEquals(listOf("0100", "0100", "222512"), adapter.commands)
        } finally { manager.close() }
    }

    @Test
    fun supportScanIncludesRangesForVoltageAndFuelRate() = runBlocking {
        val adapter = RecordingAdapter {
            when (it) {
                "0100" -> "41 00 00 00 00 01\r>"
                "0120" -> "41 20 00 00 00 01\r>"
                "0140" -> "41 40 40 00 00 04\r>"
                else -> "NO DATA\r>"
            }
        }
        val manager = manager(this) { adapter }
        manager.setPollingMode(PollingMode.VISIBLE)
        manager.pausePolling()
        manager.setPids(listOf(
            ObdPid(id = "SAE_0142_Voltaje", name = "Voltaje", shortName = "Voltaje", modeAndPid = "0142", equation = "A"),
            ObdPid(id = "Consumo", name = "Consumo", shortName = "Consumo", modeAndPid = "015E", equation = "A")
        ))
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            yield()
            assertEquals(listOf("0100", "0120", "0140"), adapter.commands)
            assertTrue(manager.activePids.value.all { it.isSupported })
        } finally { manager.close() }
    }

    @Test fun idleReadingsNeverGeneratePowerTorqueOrOtherProfileEstimates() = runBlocking {
        val adapter = RecordingAdapter {
            when (it) {
                "010C" -> "41 0C 0C 80\r>" // 800 RPM
                "0104" -> "41 04 33\r>" // 20% carga
                "010B" -> "41 0B 64\r>" // MAP = 100 kPa
                else -> "NO DATA\r>"
            }
        }
        adapter.stopBeforeRepeat = "010C"
        val manager = manager(this) { adapter }
        manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids()
            .filter { it.shortName in setOf("RPM", "Carga", "MAP") })
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { "SAE_010B_MAP" in it.readings } }
            assertEquals(800f, state.rpm)
            assertEquals(100f, state.manifoldPressureKpa)
            assertEquals(setOf("SAE_010C_RPM", "SAE_0104_Carga", "SAE_010B_MAP"), state.readings.keys)
            assertTrue(state.horsepowerCv.isNaN())
            assertTrue(state.torqueNm.isNaN())
            assertTrue(state.readings.values.all { it.isSimulated })
        } finally { manager.close() }
    }

    @Test fun invalidResponseRemovesPreviousReadingAndHistory() = runBlocking {
        for (invalid in listOf("NO DATA\r>", "41 0D 50\r>", "41 0C 1A\r>", "41 0C 1A ZZ\r>")) {
            val release = CompletableDeferred<Unit>()
            var calls = 0
            val adapter = RecordingAdapter {
                if (++calls == 1) "41 0C 1A F8\r>" else {
                    release.await()
                    invalid
                }
            }
            adapter.stopBeforeRepeat = "010C"
            adapter.allowedOccurrences = 2
            val manager = manager(this) { adapter }
            manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().filter { it.shortName == "RPM" })
            try {
                manager.startSimulation()
                withTimeout(2000) { manager.vehicleState.first { "SAE_010C_RPM" in it.readings } }
                release.complete(Unit)
                val cleared = withTimeout(2000) { manager.vehicleState.first { it.readings.isEmpty() } }
                assertTrue(cleared.rpm.isNaN())
                assertTrue(cleared.sparklineHistory.isEmpty())
            } finally { manager.close() }
        }
    }

    @Test fun blockedAdapterCannotKeepShowingStaleReadings() = runBlocking {
        val adapter = RecordingAdapter { "41 0C 1A F8\r>" }
        adapter.stopBeforeRepeat = "010C"
        val manager = manager(this, timeoutMillis = 100L) { adapter }
        manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().filter { it.shortName == "RPM" })
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.vehicleState.first { "SAE_010C_RPM" in it.readings } }
            val cleared = withTimeout(2000) { manager.vehicleState.first { it.readings.isEmpty() } }
            assertTrue(cleared.rpm.isNaN())
            assertTrue(cleared.sparklineHistory.isEmpty())
        } finally { manager.close() }
    }

    @Test fun truncatedSharedPayloadOnlyUpdatesSensorsWithAllRequiredBytes() = runBlocking {
        val adapter = RecordingAdapter { "62 25 12 00 00\r>" }
        adapter.stopBeforeRepeat = "222512"
        val manager = manager(this) { adapter }
        manager.setPids(listOf(
            ObdPid(id = "Dato", name = "Dato", shortName = "Dato", modeAndPid = "222512", equation = "INT16(A:B)"),
            ObdPid(id = "Incompleto", name = "Incompleto", shortName = "Incompleto", modeAndPid = "222512", equation = "INT16(AH:AI)"),
            ObdPid(id = "Inventado", name = "Inventado", shortName = "Inventado", modeAndPid = "222512", equation = "10")
        ))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { "Dato" in it.readings } }
            assertEquals(setOf("Dato"), state.readings.keys)
            assertEquals(0f, state.readings.getValue("Dato").value)
        } finally { manager.close() }
    }

    @Test fun disablingSensorImmediatelyClearsItsReading() = runBlocking {
        val adapter = RecordingAdapter { "41 0C 1A F8\r>" }
        adapter.stopBeforeRepeat = "010C"
        val manager = manager(this) { adapter }
        val pid = com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().first { it.shortName == "RPM" }
        manager.setPids(listOf(pid))
        try {
            manager.startSimulation()
            withTimeout(2000) { manager.vehicleState.first { "SAE_010C_RPM" in it.readings } }
            manager.togglePid(pid.id, false)
            assertTrue(manager.vehicleState.value.readings.isEmpty())
        } finally { manager.close() }
    }

    @Test fun pendingReplyDoesNotRestoreADisabledSensor() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val adapter = RecordingAdapter {
            entered.complete(Unit)
            release.await()
            returned.complete(Unit)
            "41 0C 1A F8\r>"
        }
        val manager = manager(this) { adapter }
        val pid = com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().first { it.shortName == "RPM" }
        manager.setPids(listOf(pid))
        try {
            manager.startSimulation()
            withTimeout(2000) { entered.await() }
            manager.togglePid(pid.id, false)
            release.complete(Unit)
            withTimeout(2000) { returned.await() }
            yield()
            assertTrue(manager.vehicleState.value.readings.isEmpty())
        } finally { manager.close() }
    }

    @Test fun pendingReplyCannotRestoreTelemetryAfterPause() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val adapter = RecordingAdapter {
            withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                returned.complete(Unit)
                "41 0C 1A F8\r>"
            }
        }
        val manager = manager(this) { adapter }
        manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().filter { it.shortName == "RPM" })
        try {
            manager.startSimulation()
            withTimeout(2000) { entered.await() }
            manager.pausePolling()
            release.complete(Unit)
            withTimeout(2000) { returned.await() }
            yield()
            assertTrue(manager.vehicleState.value.readings.isEmpty())
        } finally { manager.close() }
    }
    @Test fun sharedStandardPayloadOnlyPublishesPresentSensors() = runBlocking {
        val adapter = RecordingAdapter { "41 66 01 01 40 00 00\r>" }
        adapter.stopBeforeRepeat = "0166"
        val manager = manager(this) { adapter }
        manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().filter { it.modeAndPid == "0166" })
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { manager.activePids.value.single { pid -> pid.shortName == "MAF A" }.id in it.readings } }
            assertEquals(10f, state.readings.getValue(manager.activePids.value.single { pid -> pid.shortName == "MAF A" }.id).value)
            assertFalse(manager.activePids.value.single { pid -> pid.shortName == "MAF B" }.id in state.readings)
            assertFalse(manager.activePids.value.single { pid -> pid.shortName == "MAF B" }.id in state.sparklineHistory)
            assertEquals(listOf("0166"), adapter.commands)
        } finally { manager.close() }
    }

    @Test fun unusedOxygenTrimDoesNotRemoveValidVoltageFromSameFrame() = runBlocking {
        val adapter = RecordingAdapter { "41 14 64 FF\r>" }
        adapter.stopBeforeRepeat = "0114"
        val manager = manager(this) { adapter }
        manager.setPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids().filter { it.modeAndPid == "0114" })
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { manager.activePids.value.single { pid -> pid.shortName == "O2 1 V" }.id in it.readings } }
            assertEquals(0.5f, state.readings.getValue(manager.activePids.value.single { pid -> pid.shortName == "O2 1 V" }.id).value)
            assertFalse(manager.activePids.value.single { pid -> pid.shortName == "O2 1 STFT" }.id in state.readings)
            assertEquals(listOf("0114"), adapter.commands)
        } finally { manager.close() }
    }

    @Test fun highSupportRangesDiscoverModernBatteryAndEngineOdometer() = runBlocking {
        val adapter = RecordingAdapter { command ->
            when (command) {
                "0100", "0120", "0140", "0160", "0180", "01A0" -> "41 ${command.takeLast(2)} 00 00 00 01\r>"
                "01C0" -> "41 C0 00 00 60 00\r>"
                else -> "NO DATA\r>"
            }
        }
        val manager = manager(this) { adapter }
        manager.pausePolling()
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.activePids.first { pids -> pids.any { it.modeAndPid == "01D3" && it.supportChecked } } }
            val checked = manager.activePids.value
            assertTrue(checked.filter { it.modeAndPid in setOf("01D2", "01D3") }.all { it.isSupported && it.supportChecked })
            assertTrue(checked.single { it.shortName == "RPM" }.supportChecked)
            assertFalse(checked.single { it.shortName == "RPM" }.isSupported)
            assertTrue(adapter.commands.contains("01C0"))
            assertFalse(adapter.commands.contains("01D3"))
        } finally { manager.close() }
    }

    @Test fun failedSupportMapLeavesCompatibilityUnknown() = runBlocking {
        val manager = manager(this) { RecordingAdapter { "NO DATA\r>" } }
        manager.pausePolling()
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
            yield()
            assertTrue(manager.activePids.value.filter { it.modeAndPid.startsWith("01") }.none { it.supportChecked })
        } finally { manager.close() }
    }

    @Test fun catalogReloadPreservesDisabledSensorsAndCompatibility() = runBlocking {
        val manager = manager(this) { Adapter() }
        try {
            val original = manager.activePids.value.single { it.shortName == "Temp aceite" }
            manager.setPids(listOf(original.copy(isEnabled = false, isSupported = false, supportChecked = true)))
            manager.addPids(com.torqueobd2.dash.data.repository.DefaultPids.getStandardPids())
            val restored = manager.activePids.value.single { it.shortName == "Temp aceite" }
            assertEquals(original.id, restored.id)
            assertFalse(restored.isEnabled)
            assertFalse(restored.isSupported)
            assertTrue(restored.supportChecked)
        } finally { manager.close() }
    }


    @Test fun sameShortNameOnDifferentEcusKeepsValuesHistoryAndPollingIndependent() = runBlocking {
        var header = ""
        val adapter = RecordingAdapter { command ->
            if (command.startsWith("AT SH")) { header = command.removePrefix("AT SH "); "OK\r>" }
            else "62 10 01 ${if (header == "7E0") "0A" else "14"}\r>"
        }
        adapter.stopBeforeRepeat = "221001"
        adapter.allowedOccurrences = 2
        val manager = manager(this) { adapter }
        val a = ObdPid(id = "ecu-a", name = "Temperatura", shortName = "TEMP", modeAndPid = "221001", equation = "A", header = "7E0")
        val b = a.copy(id = "ecu-b", header = "7E1")
        manager.addPids(listOf(a, b))
        manager.setPids(manager.activePids.value.filter { it.id in setOf(a.id, b.id) })
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { b.id in it.readings } }
            assertEquals(10f, state.readings.getValue(a.id).value)
            assertEquals(20f, state.readings.getValue(b.id).value)
            assertEquals(listOf(10f), state.sparklineHistory[a.id])
            assertEquals(listOf(20f), state.sparklineHistory[b.id])
            manager.pausePolling()
            adapter.stopBeforeRepeat = null
            manager.setPollingMode(PollingMode.VISIBLE)
            manager.setVisibleSensors(setOf(b.id))
            manager.resumePolling()
            withTimeout(2000) { manager.vehicleState.first { b.id in it.readings } }
            manager.pausePolling()
            assertEquals(1, adapter.commands.count { it == "AT SH 7E0" })
            assertEquals(2, manager.activePids.value.size)
        } finally { manager.close() }
    }

    @Test fun malformedFieldDoesNotInvalidateAnotherSensorWithTheSameName() = runBlocking {
        val adapter = RecordingAdapter { "62 10 01 0A\r>" }
        adapter.stopBeforeRepeat = "221001"
        val manager = manager(this) { adapter }
        val a = ObdPid(id = "valid", name = "Sensor", shortName = "TEMP", modeAndPid = "221001", equation = "A")
        val b = a.copy(id = "invalid", equation = "B")
        manager.setPids(listOf(a, b))
        try {
            manager.startSimulation()
            val state = withTimeout(2000) { manager.vehicleState.first { it.sensorStatuses[b.id] == SensorTelemetryStatus.INVALID_RESPONSE } }
            assertEquals(10f, state.readings.getValue(a.id).value)
            assertEquals(listOf(10f), state.sparklineHistory[a.id])
            assertFalse(b.id in state.readings)
            assertFalse(b.id in state.sparklineHistory)
            assertTrue(manager.activePids.value.all { it.isSupported })
        } finally { manager.close() }
    }

    @Test fun explicitEcuRejectionIsIncompatibleButBrokenPayloadIsRetryable() = runBlocking {
        for ((response, reason) in listOf("7F 22 31\r>" to SensorTelemetryStatus.INCOMPATIBLE,
            "62 10 02 0A\r>" to SensorTelemetryStatus.INVALID_RESPONSE)) {
            val adapter = RecordingAdapter { response }
            adapter.stopBeforeRepeat = "221001"
            val manager = manager(this) { adapter }
            val pid = ObdPid(id = "test", name = "Sensor", shortName = "TEMP", modeAndPid = "221001", equation = "A")
            manager.setPids(listOf(pid))
            try {
                manager.startSimulation()
                val state = withTimeout(2000) { manager.vehicleState.first { it.sensorStatuses[pid.id] == reason } }
                assertTrue(state.readings.isEmpty())
                assertEquals(reason != SensorTelemetryStatus.INCOMPATIBLE, manager.activePids.value.single().isSupported)
            } finally { manager.close() }
        }
    }

    @Test fun supportBitmapForDefaultEcuCannotRejectADifferentExplicitHeader() = runBlocking {
        val adapter = RecordingAdapter { command ->
            when {
                command.startsWith("AT") -> "OK\r>"
                command == "0100" -> "41 00 00 00 00 00\r>"
                else -> "NO DATA\r>"
            }
        }
        val manager = manager(this) { adapter }
        val primary = ObdPid(id = "primary", name = "RPM", shortName = "RPM", modeAndPid = "010C", equation = "A")
        val other = primary.copy(id = "secondary", header = "7E1")
        manager.setPids(listOf(primary, other))
        manager.pausePolling()
        try {
            manager.connectBluetooth(firstDevice)
            withTimeout(2000) { manager.activePids.first { it.single { pid -> pid.id == primary.id }.supportChecked } }
            assertFalse(manager.activePids.value.single { it.id == primary.id }.isSupported)
            assertTrue(manager.activePids.value.single { it.id == other.id }.isSupported)
            assertFalse(manager.activePids.value.single { it.id == other.id }.supportChecked)
        } finally { manager.close() }
    }
    @Test
    fun recordingAndBackgroundQueryHiddenEnabledSensorsAndRestoreVisibleSelection() = runBlocking {
        val adapter = RecordingAdapter { command -> delay(1); if (command == "0105") "41 05 7E\r>" else "41 0C 1A F8\r>" }
        val samples = MutableStateFlow<List<String>>(emptyList())
        val manager = ObdManager(this, VehicleProfile(id = "test", customInitCommands = ""),
            ioDispatcher = Dispatchers.Unconfined, connectionFactory = { adapter },
            onReading = { reading, pid, profile, source ->
                assertEquals(profile.id, "test")
                assertEquals(source.type, ConnectionType.SIMULATION)
                assertEquals(pid.id, reading.pidId)
                samples.value = samples.value + reading.pidId
            })
        val a = ObdPid(id = "shown", name = "RPM", shortName = "RPM", modeAndPid = "010C", equation = "A")
        val b = ObdPid(id = "hidden", name = "Temp", shortName = "Temp", modeAndPid = "0105", equation = "A-40")
        manager.setPids(listOf(a, b, b.copy(id = "disabled", isEnabled = false)))
        manager.setVisibleSensors(emptySet())
        try {
            manager.startSimulation()
            delay(30)
            assertTrue(samples.value.isEmpty())
            manager.setRecordingActive(true)
            withTimeout(1500) { samples.first { "shown" in it && "hidden" in it } }
            assertFalse("disabled" in samples.value)
            manager.setRecordingActive(false)
            delay(50)
            val commands = adapter.commands.size
            delay(150)
            assertEquals(commands, adapter.commands.size)
            val beforeBackground = samples.value.size
            manager.setBackgroundReading(true)
            withTimeout(1500) { samples.first { it.size > beforeBackground + 1 } }
            manager.setBackgroundReading(false)
            delay(50)
            val afterBackground = adapter.commands.size
            delay(150)
            assertEquals(afterBackground, adapter.commands.size)
        } finally { manager.close() }
    }

    @Test
    fun acceptedReadingCallbackDoesNotReplayCachedValuesWhilePaused() = runBlocking {
        val adapter = RecordingAdapter { delay(1); "41 05 7E\r>" }
        var samples = 0
        val manager = ObdManager(this, VehicleProfile(customInitCommands = ""),
            ioDispatcher = Dispatchers.Unconfined, connectionFactory = { adapter }, onReading = { _, _, _, _ -> samples++ })
        manager.setPids(listOf(ObdPid(id = "hidden", name = "Temp", shortName = "Temp", modeAndPid = "0105", equation = "A-40")))
        manager.setRecordingActive(true)
        try {
            manager.startSimulation()
            withTimeout(1500) { manager.vehicleState.first { it.readings.isNotEmpty() } }
            manager.pausePolling()
            val count = samples
            delay(150)
            assertEquals(count, samples)
            assertTrue(manager.vehicleState.value.readings.isEmpty())
        } finally { manager.close() }
    }

}
