package com.torqueobd2.dash.data.model

import org.junit.Assert.*
import org.junit.Test

class VehicleStateTest {
    private fun reading(name: String, value: Float, unit: String = "") =
        ObdReading(name, name, name, value, unit, 0f, 100f)

    @Test fun absentTelemetryDisplaysDashWhileMeasuredZeroRemainsZero() {
        assertEquals("—", VehicleState().rpm.telemetryText())
        assertEquals("—", reading("RPM", Float.NaN).formatted())
        assertEquals(0f, reading("RPM", Float.NaN).progress)
        val state = VehicleState(readings = mapOf(SensorIds.RPM to reading(SensorIds.RPM, 0f)))
        assertEquals("0", state.rpm.telemetryText())
    }

    @Test fun customNamedRpmCannotOverwriteStandardRpm() {
        val custom = reading("custom-rpm", 999f).copy(shortName = "RPM")
        val state = VehicleState(readings = mapOf(SensorIds.RPM to reading(SensorIds.RPM, 800f), custom.pidId to custom))
        assertEquals(800f, state.rpm)
        assertEquals(999f, state.readings.getValue("custom-rpm").value)
        assertTrue(VehicleState(mapOf("custom-rpm" to custom)).rpm.isNaN())
    }

    @Test fun regenerationCodesPreserveAllThirtyTwoBitsWhenFormatted() {
        val reading = reading("DPF reg.state", 4294967295.0.toFloat()).copy(decodedValue = 4294967295.0)
        assertEquals("4294967295", reading.formatted())
    }
}
