package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DtcManagerTest {
    private class Adapter : ObdConnection {
        override val status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        override val statusMessage = MutableStateFlow("Prueba")
        val commands = mutableListOf<String>()
        var cleared = false
        var failClear = false
        var rpm = 0
        var blocked = false
        var headers = false
        override suspend fun connect(): Boolean { status.value = ConnectionStatus.CONNECTED; return true }
        override suspend fun disconnect() { status.value = ConnectionStatus.DISCONNECTED }
        override fun isConnected() = status.value == ConnectionStatus.CONNECTED
        private fun frame(payload: String): String = if (headers) "7E8 %02X $payload\r>".format(payload.split(" ").size) else "$payload\r>"
        override suspend fun sendCommand(command: String): String {
            commands.add(command)
            when (command) {
                "AT H1" -> headers = true
                "AT H0", "AT D" -> headers = false
            }
            if (command == "03" && blocked) awaitCancellation()
            return when (command) {
                "AT DPN" -> "A6\r>"
                "0100" -> "41 00 00 00 00 00\r>"
                "0101" -> frame(if (cleared) "41 01 00 07 00 00" else "41 01 81 07 00 00")
                "010C" -> frame("41 0C %02X %02X".format(rpm * 4 / 256, rpm * 4 % 256))
                "03" -> frame(if (cleared) "43 00" else "43 01 03 00")
                "07" -> frame("47 00")
                "0A" -> frame("4A 01 04 20")
                "020200" -> frame("42 02 00 00 00")
                "04" -> if (failClear) "NO DATA\r>" else { cleared = true; frame("44") }
                else -> if (command.startsWith("AT")) "OK\r>" else "NO DATA\r>"
            }
        }
    }
    private suspend fun connected(scope: CoroutineScope, adapter: Adapter): ObdManager {
        val manager = ObdManager(scope, VehicleProfile(customInitCommands = "AT SH 7E0"), ioDispatcher = Dispatchers.Unconfined,
            connectionFactory = { adapter }, retryDelayMillis = { 1 })
        manager.pausePolling()
        manager.connectWifi("localhost", 35000)
        withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.CONNECTED } }
        yield()
        return manager
    }
    private suspend fun finished(manager: ObdManager) = withTimeout(2000) { manager.dtcState.first { !it.busy && (it.report != null || it.error != null) } }
    @Test fun readRestoresPollingFormatAndNeverClears() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.readTroubleCodes(); val state = finished(manager)
            assertNull(state.error); assertEquals(listOf("P0300", "P0420"), state.report!!.codes.map { it.code })
            assertFalse(adapter.commands.contains("04")); assertFalse(adapter.headers)
            assertEquals(listOf("AT E0", "AT L0", "AT S0", "AT H0"), adapter.commands.takeLast(4))
            assertTrue(adapter.commands.contains("AT CAF1"))
        } finally { manager.close() }
    }
    @Test fun clearRequiresExplicitConfirmationAndCurrentSession() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.clearTroubleCodes(0, true); assertFalse(adapter.commands.contains("04"))
            manager.readTroubleCodes(); val report = finished(manager).report!!
            manager.clearTroubleCodes(report.session, false); assertFalse(adapter.commands.contains("04"))
            manager.clearTroubleCodes(report.session + 1, true); assertFalse(adapter.commands.contains("04"))
            manager.clearTroubleCodes(report.session, true); val after = finished(manager)
            assertEquals(1, adapter.commands.count { it == "04" })
            assertEquals(DiagnosticStatus.OK, after.clearResult!!.status)
            assertEquals(listOf("P0420"), after.report!!.codes.map { it.code })
            assertTrue(adapter.commands.indexOf("010C") < adapter.commands.indexOf("04"))
            assertFalse(adapter.headers)
        } finally { manager.close() }
    }
    @Test fun runningEngineBlocksDestructiveCommand() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.readTroubleCodes(); val before = finished(manager).report!!
            adapter.rpm = 800; manager.clearTroubleCodes(before.session, true)
            val after = finished(manager)
            assertTrue(after.error!!.contains("Motor en marcha")); assertFalse(adapter.commands.contains("04"))
            assertEquals(before, after.report); assertFalse(adapter.headers)
        } finally { manager.close() }
    }
    @Test fun unconfirmedClearDoesNotInventSuccessfulClear() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.readTroubleCodes(); val before = finished(manager).report!!
            adapter.failClear = true; manager.clearTroubleCodes(before.session, true)
            val after = finished(manager)
            assertEquals(DiagnosticStatus.NO_DATA, after.clearResult!!.status)
            assertTrue(after.report!!.codes.any { it.code == "P0300" })
            assertTrue(adapter.commands.count { it == "03" } == 2)
        } finally { manager.close() }
    }
    @Test fun cancelRestoresAdapterAndPreservesPriorSnapshot() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.readTroubleCodes(); val before = finished(manager).report!!
            adapter.blocked = true; manager.readTroubleCodes(); yield()
            assertTrue(manager.dtcState.value.busy)
            manager.cancelDiagnostics(); val after = finished(manager)
            assertEquals(before, after.report); assertTrue(after.error!!.contains("cancelada")); assertFalse(adapter.headers)
        } finally { manager.close() }
    }
    @Test fun changedVehicleInvalidatesSnapshotAndOldClearAuthorization() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            manager.readTroubleCodes(); val before = finished(manager).report!!
            manager.updateProfile(VehicleProfile(name = "Otro vehículo", customInitCommands = ""))
            assertNull(manager.dtcState.value.report)
            manager.clearTroubleCodes(before.session, true)
            assertFalse(adapter.commands.contains("04"))
        } finally { manager.close() }
    }
    @Test fun disconnectCancelsPendingQueryBeforeReplacingAdapter() = runBlocking {
        val adapter = Adapter(); val manager = connected(this, adapter)
        try {
            adapter.blocked = true; manager.readTroubleCodes(); yield(); manager.disconnect()
            withTimeout(2000) { manager.connectionState.first { it.status == ConnectionStatus.DISCONNECTED } }
            assertNull(manager.dtcState.value.report); assertFalse(manager.dtcState.value.busy)
            assertFalse(adapter.commands.contains("04")); assertFalse(adapter.headers)
        } finally { manager.close() }
    }
}
