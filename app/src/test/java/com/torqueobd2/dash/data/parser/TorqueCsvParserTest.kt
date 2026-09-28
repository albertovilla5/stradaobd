package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.repository.DefaultPids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorqueCsvParserTest {

    @Test
    fun testParseSampleHondaIdtecCsv() {
        val result = TorqueCsvParser.parseString(DefaultPids.SAMPLE_HONDA_IDTEC_CSV)

        // Verificar comandos init string detectados en el comentario
        assertEquals(2, result.initCommands.size)
        assertEquals("AT SP7", result.initCommands[0])
        assertEquals("AT SH DA10F1", result.initCommands[1])

        // Debe haber exactamente 10 PIDs en este archivo
        assertEquals(10, result.pids.size)

        // Verificar el primer PID: "iDTEC Distance from last DPF regeneration success"
        val distPid = result.pids.find { it.shortName == "DPF dist a/reg" }
        assertNotNull(distPid)
        assertEquals("iDTEC Distance from last DPF regeneration success", distPid?.name)
        assertEquals("0x222512", distPid?.modeAndPid)
        assertEquals("222512", distPid?.cleanObdCommand())
        assertEquals("INT16(V:W)/10", distPid?.equation)
        assertEquals(0f, distPid?.minValue)
        assertEquals(6553f, distPid?.maxValue)
        assertEquals("km", distPid?.units)

        // Verificar EGT
        val egtPid = result.pids.find { it.shortName == "EGT" }
        assertNotNull(egtPid)
        assertEquals("O*4", egtPid?.equation)
        assertEquals("°C", egtPid?.units)
        assertEquals(1000f, egtPid?.maxValue)

        // Verificar Nivel de aceite
        val oilPid = result.pids.find { it.shortName == "Eng.oil lvl f" }
        assertNotNull(oilPid)
        assertEquals("AS/2", oilPid?.equation)
        assertEquals("mm", oilPid?.units)
        assertEquals("0x222510", oilPid?.modeAndPid)
    }

    @Test
    fun testCleanObdCommandPrefixRemoval() {
        val pidHex = com.torqueobd2.dash.data.model.ObdPid(
            name = "Test",
            shortName = "TST",
            modeAndPid = "0x222512",
            equation = "A"
        )
        assertEquals("222512", pidHex.cleanObdCommand())

        val pidPlain = com.torqueobd2.dash.data.model.ObdPid(
            name = "Test2",
            shortName = "TST2",
            modeAndPid = "010C",
            equation = "A"
        )
        assertEquals("010C", pidPlain.cleanObdCommand())
    }
}
