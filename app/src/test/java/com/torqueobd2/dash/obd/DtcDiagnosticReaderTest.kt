package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DtcDiagnosticReaderTest {
    private fun frame(payload: String, ecu: String = "7E8") = "$ecu %02X $payload\r>".format(payload.split(" ").size)
    private fun reply(command: String): String = when (command) {
        "0101" -> frame("41 01 82 07 21 20")
        "03" -> frame("43 02 03 00 01 33")
        "07" -> frame("47 01 01 71")
        "0A" -> frame("4A 01 04 20")
        "020200" -> frame("42 02 00 03 00")
        "020000" -> frame("42 00 00 08 18 00 00")
        "020500" -> frame("42 05 00 7E")
        "020C00" -> frame("42 0C 00 0C 80")
        "020D00" -> frame("42 0D 00 00")
        "04" -> frame("44")
        else -> "NO DATA\r>"
    }
    @Test fun fullSnapshotUsesOnlyFrozenValuesAndIncludesProvenance() = runBlocking {
        val commands = mutableListOf<String>()
        val report = DtcDiagnosticReader({ commands.add(it); reply(it) }, true).read(VehicleProfile(), "ELM de prueba", false, 7)
        assertEquals(4, report.codes.size)
        assertEquals(3, report.freezeFrame.values.size)
        assertTrue(report.freezeFrame.values.any { it.value == 800.0 && it.unit == "RPM" })
        assertTrue(report.freezeFrame.values.any { it.value == 86.0 })
        assertEquals(DiagnosticStatus.OK, report.freezeFrame.status)
        assertEquals("P0300", report.freezeFrame.triggeringCodes["7E8"])
        assertTrue(report.monitors.single().milOn)
        assertEquals(2, report.monitors.single().confirmedCount)
        assertTrue(report.monitors.single().monitors.any { it.name == "Sonda de oxígeno" && !it.complete })
        assertFalse(commands.contains("04"))
        assertFalse(commands.contains("010C"))
        val text = report.asText { "Definición de $it" }
        assertTrue(text.contains("StradaOBD")); assertTrue(text.contains("ECU 7E8")); assertTrue(text.contains("Respuestas originales")); assertTrue(text.contains("ELM de prueba"))
        assertFalse(text.contains("SIMULACIÓN")); assertEquals(7L, report.session)
    }
    @Test fun noFreezeFrameDoesNotReadCurrentData() = runBlocking {
        val commands = mutableListOf<String>()
        val report = DtcDiagnosticReader({ commands.add(it); if (it == "020200") frame("42 02 00 00 00") else reply(it) }, true)
            .read(VehicleProfile(), "Prueba", false, 0)
        assertEquals(DiagnosticStatus.OK, report.freezeFrame.status)
        assertTrue(report.freezeFrame.values.isEmpty()); assertFalse(commands.contains("020C00"))
    }
    @Test fun unsupportedModeAndNoResponseNeverClaimNoFaults() = runBlocking {
        val report = DtcDiagnosticReader({ if (it == "0A") frame("7F 0A 11") else "NO DATA" }, true)
            .read(VehicleProfile(), "Prueba", false, 0)
        assertFalse(report.complete)
        assertEquals(DiagnosticStatus.UNSUPPORTED, report.batches.last().status)
        assertEquals(DiagnosticStatus.NO_DATA, report.batches.first().status)
        assertFalse(report.asText { it }.contains("Sin códigos"))
    }
    @Test fun truncatedMonitorIsInvalid() = runBlocking {
        val report = DtcDiagnosticReader({ if (it == "0101") frame("41 01 80") else reply(it) }, true).read(VehicleProfile(), "Prueba", false, 0)
        assertEquals(DiagnosticStatus.INVALID, report.monitorStatus)
        assertTrue(report.monitors.isEmpty())
    }
    @Test fun wrongFrozenFrameDoesNotDecodeAsValid() = runBlocking {
        val report = DtcDiagnosticReader({ if (it == "020C00") frame("42 0C 01 0C 80") else reply(it) }, true).read(VehicleProfile(), "Prueba", false, 0)
        assertEquals(DiagnosticStatus.PARTIAL, report.freezeFrame.status)
        assertFalse(report.freezeFrame.values.any { it.unit == "RPM" })
    }
    @Test fun clearNeedsExactPositiveResponse() = runBlocking {
        assertEquals(DiagnosticStatus.OK, DtcDiagnosticReader(::reply, true).clear().status)
        assertEquals(DiagnosticStatus.INVALID, DtcDiagnosticReader({ frame("44 00") }, true).clear().status)
        assertEquals(DiagnosticStatus.NO_DATA, DtcDiagnosticReader({ "NO DATA" }, true).clear().status)
        assertEquals(DiagnosticStatus.NO_DATA, DtcDiagnosticReader({ frame("7F 04 22") }, true).clear().status)
    }
    @Test fun mixedClearConfirmationRemainsPartial() = runBlocking {
        val result = DtcDiagnosticReader({ frame("44") + "\r" + frame("7F 04 22", "7E9") }, true).clear()
        assertEquals(DiagnosticStatus.PARTIAL, result.status)
        assertEquals(listOf("7E8"), result.acceptedEcus)
    }
    @Test fun missingSupportRangeDoesNotSuppressUnverifiedFrozenPid() = runBlocking {
        val commands = mutableListOf<String>()
        val report = DtcDiagnosticReader({ command ->
            commands.add(command)
            when (command) {
                "020000" -> frame("42 00 00 08 18 00 01")
                "022000" -> "NO DATA"
                "024200" -> frame("42 42 00 36 B0")
                else -> reply(command)
            }
        }, true).read(VehicleProfile(), "Prueba", false, 0)
        assertTrue(commands.contains("024200"))
        assertTrue(report.freezeFrame.values.any { it.value == 14.0 && it.unit == "V" })
    }
    @Test fun mixedEmptyFreezeAndNoDataRemainsPartial() = runBlocking {
        val report = DtcDiagnosticReader({ if (it == "020200") frame("42 02 00 00 00") + "\rNO DATA" else reply(it) }, true)
            .read(VehicleProfile(), "Prueba", false, 0)
        assertEquals(DiagnosticStatus.PARTIAL, report.freezeFrame.status)
        assertFalse(report.complete)
    }
    @Test fun simulationCanBeReadAndClearKeepsPermanentCode() = runBlocking {
        val simulator = SimulatedObdConnection(); simulator.connect(); simulator.sendCommand("AT H1")
        val reader = DtcDiagnosticReader(simulator::sendCommand, true)
        val before = reader.read(VehicleProfile(), "Simulador", true, 1)
        assertEquals(4, before.codes.size)
        assertTrue(before.asText { it }.contains("SIMULACIÓN"))
        assertEquals(DiagnosticStatus.OK, reader.clear().status)
        val after = reader.read(VehicleProfile(), "Simulador", true, 1)
        assertEquals(listOf("P0420"), after.codes.map { it.code })
        assertFalse(after.monitors.single().milOn)
        assertTrue(after.freezeFrame.values.isEmpty())
        simulator.disconnect()
    }
}
