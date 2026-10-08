package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.ObdReading
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.parser.EquationEvaluator
import com.torqueobd2.dash.data.parser.PidDecoder
import org.junit.Assert.*
import org.junit.Test

class StandardPidCatalogTest {
    private val catalog = DefaultPids.getStandardPids()
    private fun decode(name: String, vararg bytes: Int) = PidDecoder.decode(
        catalog.single { it.shortName == name }, EquationEvaluator.mapBytesToVariables(bytes))

    @Test fun catalogOffersBroadUniqueStableDefinitionsWithoutEstimatedPower() {
        assertTrue(catalog.size >= 170)
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        assertEquals(catalog.size, catalog.map { it.shortName }.toSet().size)
        assertEquals(catalog.map { it.id }, DefaultPids.getStandardPids().map { it.id })
        assertTrue(catalog.all { it.cleanObdCommand().matches(Regex("01[0-9A-F]{2}")) })
        assertFalse(catalog.any { it.shortName in setOf("Potencia", "Par") })
        assertTrue(catalog.all { it.minValue < it.maxValue && !it.supportChecked })
        println("Catálogo: ${catalog.size} métricas, ${catalog.map { it.cleanObdCommand() }.toSet().size} consultas")
    }

    @Test fun fuelAndEgrCorrectionsKeepNegativeValuesAndMeasuredZero() {
        assertEquals(-50.0, decode("STFT B1", 64), 0.0001)
        assertEquals(0.0, decode("LTFT B2", 128), 0.0001)
        assertEquals(99.21875, decode("Error EGR", 255), 0.0001)
        assertEquals(50.0, decode("O2 trim largo B4", 128, 192), 0.0001)
    }

    @Test fun railIgnitionOilAndEvapUseTheirProtocolUnits() {
        assertEquals(100000.0, decode("Rail diésel", 0x27, 0x10), 0.0001)
        assertEquals(79.0, decode("Rail relativo", 3, 232), 0.0001)
        assertEquals(-100.0, decode("Presión EVAP", 0xFE, 0x70), 0.0001)
        assertEquals(-0.25, decode("Presión EVAP", 255, 255), 0.0001)
        assertEquals(100.0, decode("Temp aceite", 140), 0.0001)
        assertEquals(26.0, decode("Avance encendido", 180), 0.0001)
        assertEquals(0.0, decode("Avance inyección", 0x69, 0), 0.0001)
    }

    @Test fun allEightOxygenChannelsDecodeVoltageRatioAndSignedCurrent() {
        for (i in 1..8) {
            assertEquals(0.45, decode("O2 $i V", 90, 128), 0.0001)
            assertEquals(0.0, decode("O2 $i STFT", 90, 128), 0.0001)
            assertEquals(1.0, decode("O2 $i equiv V", 128, 0, 32, 0), 0.0001)
            assertEquals(1.0, decode("O2 $i banda V", 128, 0, 32, 0), 0.0001)
            assertEquals(1.0, decode("O2 $i equiv I", 128, 0, 112, 0), 0.0001)
            assertEquals(-16.0, decode("O2 $i mA", 128, 0, 112, 0), 0.0001)
        }
    }

    @Test fun unusedOxygenTrimAndTruncatedWidebandNeverBecomeReadings() {
        assertThrows(IllegalArgumentException::class.java) { decode("O2 1 STFT", 100, 255) }
        assertEquals(0.5, decode("O2 1 V", 100, 255), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("O2 1 banda V", 128, 0, 32) }
    }

    @Test fun optionalMafSensorsRequirePresenceBitsEvenWhenTheirBytesAreZero() {
        assertEquals(10.0, decode("MAF A", 1, 1, 64, 0, 0), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("MAF B", 1, 1, 64, 0, 0) }
        assertEquals(0.0, decode("MAF B", 2, 0, 0, 0, 0), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("MAF A", 0, 0, 0, 0, 0) }
    }

    @Test fun intakeSensorsKeepBankAndPositionAndDoNotReadPresenceByteAsTemperature() {
        for (i in 0..5) {
            val name = "Admisión B${i / 3 + 1} S${i % 3 + 1}"
            assertEquals((i * 10).toDouble(), decode(name, 63, 40, 50, 60, 70, 80, 90), 0.0001)
            assertThrows(IllegalArgumentException::class.java) { decode(name, 0, 40, 50, 60, 70, 80, 90) }
        }
    }

    @Test fun turboBBitsSkipTheControlStatusBit() {
        assertEquals(20.0, decode("Turbo B objetivo", 8, 0, 0, 0, 0, 2, 128, 0, 0), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("Turbo B objetivo", 4, 0, 0, 0, 0, 2, 128, 0, 0) }
        assertEquals(30.0, decode("Turbo B actual", 16, 0, 0, 0, 0, 0, 0, 3, 192), 0.0001)
    }

    @Test fun fuelRailMultipleSensorsHaveDifferentByteOffsetsAndFlags() {
        val payload = intArrayOf(63, 0, 100, 0, 90, 100, 0, 80, 0, 70, 110)
        assertEquals(1000.0, decode("Rail A objetivo", *payload), 0.0001)
        assertEquals(900.0, decode("Rail A actual", *payload), 0.0001)
        assertEquals(60.0, decode("Temp fuel A", *payload), 0.0001)
        assertEquals(800.0, decode("Rail B objetivo", *payload), 0.0001)
        assertEquals(700.0, decode("Rail B actual", *payload), 0.0001)
        assertEquals(70.0, decode("Temp fuel B", *payload), 0.0001)
    }

    @Test fun hybridCurrentRemainsSignedAndDoesNotUseBattery12v() {
        assertEquals(350.0, decode("Voltaje HV", 6, 0, 0x57, 0x80, 0xFF, 0x38), 0.0001)
        assertEquals(-20.0, decode("Corriente HV", 6, 0, 0x57, 0x80, 0xFF, 0x38), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("Voltaje HV", 4, 0, 0, 0, 0, 0) }
    }

    @Test fun recommendedGearIsExplicitlyDifferentFromEngagedGear() {
        assertEquals(4.0, decode("Marcha recomendada", 16, 0x40), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("Marcha recomendada", 1, 0x40) }
        assertTrue(catalog.single { it.shortName == "Marcha recomendada" }.name.contains("no marcha engranada"))
    }

    @Test fun torquePercentAndReferenceCannotPopulateNmOrPowerCards() {
        assertEquals(25.0, decode("Par actual ECU", 150), 0.0001)
        assertEquals(400.0, decode("Par referencia", 1, 144), 0.0001)
        val readings = catalog.filter { it.shortName.startsWith("Par") }.associate { pid ->
            pid.id to ObdReading(pid.id, pid.name, pid.shortName, 25f, pid.units, pid.minValue, pid.maxValue)
        }
        assertTrue(VehicleState(readings).torqueNm.isNaN())
        assertTrue(VehicleState(readings).horsepowerCv.isNaN())
    }

    @Test fun odometersAndRunTimesPreserveUnsigned32BitPrecision() {
        assertEquals(429496729.5, decode("Odómetro ECU", 255, 255, 255, 255), 0.00001)
        assertEquals(429496729.5, decode("Odómetro motor", 255, 255, 255, 255), 0.00001)
        assertEquals(4294967295.0, decode("Tiempo total ECU", 1, 255, 255, 255, 255), 0.00001)
    }

    @Test fun certifiedBatteryFieldsRespectSeparateSupportBitsAndPercentUnits() {
        assertEquals(100.0, decode("Energía cert. ECU", 1, 255, 0), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { decode("Autonomía cert. ECU", 1, 255, 0) }
        assertEquals("%", catalog.single { it.shortName == "Autonomía cert. ECU" }.units)
    }

    @Test fun statusLabelsKeepUnknownCodesVisibleRatherThanGuessing() {
        val pid = catalog.single { it.shortName == "MIL" }
        val reading = ObdReading(pid.id, pid.name, pid.shortName, 1f, "", 0f, 1f, valueLabels = pid.valueLabels)
        assertEquals("Encendido", reading.formatted())
        assertEquals("Apagado", reading.copy(value = 0f, decodedValue = 0.0).formatted())
        assertEquals("3.00", reading.copy(value = 3f, decodedValue = 3.0).formatted())
    }
}
