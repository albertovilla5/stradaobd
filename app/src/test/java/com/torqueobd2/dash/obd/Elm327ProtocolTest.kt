package com.torqueobd2.dash.obd

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327ProtocolTest {

    @Test
    fun testCleanResponse() {
        val raw = "SEARCHING...\r\n41 0C 1A F8\r\n>"
        val cleaned = Elm327Protocol.cleanResponse(raw)
        assertEquals("41 0C 1A F8", cleaned)
    }

    @Test
    fun testExtractDataBytesMode01Rpm() {
        // PID 010C (RPM) -> 41 0C 1A F8 (6904 decimal -> 1726 RPM)
        val raw = "SEARCHING...\r\n41 0C 1A F8\r\n>"
        val bytes = Elm327Protocol.extractDataBytes("010C", raw)
        assertArrayEquals(intArrayOf(0x1A, 0xF8), bytes)
    }

    @Test
    fun testExtractDataBytesMode22Extended() {
        // PID 222512 (DPF Diff Pressure en Honda) -> 62 25 12 00 24
        val raw = "62 25 12 00 24\r\n>"
        val bytes = Elm327Protocol.extractDataBytes("222512", raw)
        assertArrayEquals(intArrayOf(0x00, 0x24), bytes)
    }

    @Test
    fun testExtractDataBytesMultlineCan() {
        // Trama multilínea CAN ISO 15765-4
        val raw = "0: 62 25 12 01 02\r\n1: 03 04 05 06 07\r\n>"
        val bytes = Elm327Protocol.extractDataBytes("222512", raw)
        assertArrayEquals(intArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07), bytes)
    }

    @Test
    fun testDiagnoseResponseErrorUnableToConnect() {
        val err = Elm327Protocol.diagnoseResponseError("UNABLE TO CONNECT\r\n>")
        assertNotNull(err)
        assertTrue(err!!.contains("posición ON", ignoreCase = true))
    }

    @Test
    fun testDiagnoseResponseErrorBusInit() {
        val err = Elm327Protocol.diagnoseResponseError("BUS INIT: ... ERROR\r\n>")
        assertNotNull(err)
        assertTrue(err!!.contains("inicialización del bus OBD", ignoreCase = true))
    }

    @Test
    fun testDiagnoseResponseNoErrorOnValidData() {
        val ok = Elm327Protocol.diagnoseResponseError("41 00 BE 3E B8 11\r\n>")
        assertNull(ok)
    }
}
