package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.*
import org.junit.Assert.*
import org.junit.Test

class DiagnosticResponseParserTest {
    private fun batch(raw: String, can: Boolean? = null, kind: DtcKind = DtcKind.CONFIRMED) =
        DiagnosticResponseParser.dtcs(kind, DiagnosticResponseParser.parse(kind.mode, raw, can))
    @Test fun decodesAllFamiliesAndHexDigits() {
        assertEquals("P0133", DiagnosticResponseParser.decodeCode(1, 0x33))
        assertEquals("C1234", DiagnosticResponseParser.decodeCode(0x52, 0x34))
        assertEquals("B2ABC", DiagnosticResponseParser.decodeCode(0xAA, 0xBC))
        assertEquals("U1016", DiagnosticResponseParser.decodeCode(0xD0, 0x16))
        assertNull(DiagnosticResponseParser.decodeCode(0, 0))
    }
    @Test fun legacyPairsAndPadding() {
        val result = batch("03\r43 01 33 03 00 00 00\r>", false)
        assertEquals(DiagnosticStatus.OK, result.status)
        assertEquals(listOf("P0133", "P0300"), result.codes.map { it.code })
        assertEquals(listOf("Sin identificar"), result.respondingEcus)
    }
    @Test fun legacyHeadersKeepModulesSeparate() {
        val result = batch("48 6B 10 43 01 33 00 00 00\r48 6B 18 43 03 00 00 00 00")
        assertEquals(listOf("10", "18"), result.codes.map { it.ecu })
        assertEquals(DiagnosticStatus.OK, result.status)
    }
    @Test fun kwpExplicitLength() {
        val result = batch("80 F1 10 03 43 01 33 00")
        assertEquals("P0133", result.codes.single().code)
        assertEquals("10", result.codes.single().ecu)
        assertEquals(DiagnosticStatus.INVALID, batch("80 F1 10 04 43 01 33 00").status)
    }
    @Test fun canCountsIgnoreTransportPadding() {
        val result = batch("7E8 06 43 02 03 00 01 33 00")
        assertEquals(DiagnosticStatus.OK, result.status)
        assertEquals(listOf("P0300", "P0133"), result.codes.map { it.code })
        assertEquals("7E8", result.codes.first().ecu)
    }
    @Test fun can29AndCompactAndDlc() {
        listOf("18DAF110 04 43 01 04 20 00 00 00", "18DAF1100443010420000000", "7E8 8 04 43 01 04 20 00 00 00").forEach {
            val result = batch(it)
            assertEquals(it, DiagnosticStatus.OK, result.status)
            assertEquals("P0420", result.codes.single().code)
        }
    }
    @Test fun validEmptyIsDistinctFromNoResponse() {
        assertEquals(DiagnosticStatus.OK, batch("7E8 02 43 00 00 00 00 00 00").status)
        assertTrue(batch("7E8 02 43 00 00 00 00 00 00").codes.isEmpty())
        assertEquals(DiagnosticStatus.OK, batch("43 00 00", false).status)
        assertEquals(DiagnosticStatus.NO_DATA, batch("NO DATA\r>").status)
        assertEquals(DiagnosticStatus.NO_DATA, batch("03\r>").status)
    }
    @Test fun unsupportedNegativeMode() {
        val result = batch("7E8 03 7F 0A 11 00 00 00 00", kind = DtcKind.PERMANENT)
        assertEquals(DiagnosticStatus.UNSUPPORTED, result.status)
        assertTrue(result.codes.isEmpty())
    }
    @Test fun mixedNegativeAndPositiveIsPartial() {
        val result = batch("7E8 04 43 01 03 00 00 00 00\r7E9 03 7F 03 11 00 00 00 00")
        assertEquals(DiagnosticStatus.PARTIAL, result.status)
        assertEquals("P0300", result.codes.single().code)
    }
    @Test fun multiFrameInterleavedByEcu() {
        val result = batch("7E8 10 0A 43 04 03 00 01 33\r7E9 04 43 01 01 71 00 00 00\r7E8 21 04 20 D0 16 00 00 00")
        assertEquals(DiagnosticStatus.OK, result.status)
        assertEquals(5, result.codes.size)
        assertEquals(setOf("P0300", "P0133", "P0420", "U1016"), result.codes.filter { it.ecu == "7E8" }.map { it.code }.toSet())
        assertEquals("P0171", result.codes.single { it.ecu == "7E9" }.code)
    }
    @Test fun truncatedAndWrongOrderNeverProduceCleanReport() {
        listOf("7E8 10 0A 43 04 03 00 01 33", "7E8 10 0A 43 04 03 00 01 33\r7E8 22 04 20 D0 16 00 00 00",
            "7E8 06 43 02 03 00", "43 01 03", "7E8 04 43 02 03 00 00 00 00", "7E8 04 43 01 00 00 00 00 00",
            "7E8 04 47 01 03 00 00 00 00", "BUS ERROR\r>", "?").forEach {
            assertEquals(it, DiagnosticStatus.INVALID, batch(it, true).status)
        }
    }
    @Test fun indexedElmMessagesValidateLengthAndOrder() {
        val valid = batch("00A\r0: 43 04 03 00 01 33\r1: 04 20 D0 16", true)
        assertEquals(DiagnosticStatus.OK, valid.status)
        assertEquals(4, valid.codes.size)
        assertEquals(DiagnosticStatus.INVALID, batch("00A\r0: 43 04 03 00 01 33", true).status)
        assertEquals(DiagnosticStatus.INVALID, batch("0: 43 04 03 00 01 33\r2: 04 20 D0 16", true).status)
    }
    @Test fun transportValidWithBadCountIsPartialWhenOtherEcuResponds() {
        val result = batch("7E8 04 43 01 03 00 00 00 00\r7E9 04 43 02 01 71 00 00 00")
        assertEquals(DiagnosticStatus.PARTIAL, result.status)
        assertEquals(listOf("7E8"), result.respondingEcus)
    }
    @Test fun freezeFrameRejectsWrongPidAndFrameNumber() {
        val frame = DiagnosticFrame("7E8", listOf(0x42, 0x0C, 0, 0x0C, 0x80), true)
        assertEquals(listOf(0x0C, 0x80), DiagnosticResponseParser.pidData(frame, 2, 12))
        assertNull(DiagnosticResponseParser.pidData(frame, 2, 12, 1))
        assertNull(DiagnosticResponseParser.pidData(frame, 2, 13))
        assertNull(DiagnosticResponseParser.pidData(frame, 1, 12))
    }
    @Test fun emptyLegacyPayloadIsInvalid() { assertEquals(DiagnosticStatus.INVALID, batch("43", false).status) }
    @Test fun untrustedOversizedResponseIsInvalid() { assertEquals(DiagnosticStatus.INVALID, batch("0".repeat(100_001)).status) }
}
