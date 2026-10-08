package com.torqueobd2.dash.data.model

import org.junit.Assert.*
import org.junit.Test

class SensorTelemetryStatusTest {
    private val now = 10_000_000_000L
    private val pid = ObdPid(id = "rpm", name = "Revoluciones", shortName = "RPM", modeAndPid = "010C", equation = "INT16(A:B)/4", units = "RPM")
    private val connection = ConnectionState(status = ConnectionStatus.CONNECTED)
    private val reading = ObdReading("rpm", "Revoluciones", "RPM", 0f, "RPM", 0f, 8000f, receivedAtNanos = now)
    private fun state(reading: ObdReading = this.reading) = VehicleState(mapOf(pid.id to reading))

    @Test fun measuredZeroWithRealObdIsExplicitlyAvailable() {
        assertEquals(SensorTelemetryStatus.AVAILABLE, pid.telemetryStatus(state(), connection, now))
        assertEquals(0f, pid.currentReading(state(), connection, now)?.value)
    }

    @Test fun supportBitmapAloneCannotMarkSensorAvailable() {
        assertEquals(SensorTelemetryStatus.WAITING,
            pid.copy(supportChecked = true, isSupported = true).telemetryStatus(VehicleState(), connection, now))
        assertEquals(SensorTelemetryStatus.INCOMPATIBLE,
            pid.copy(supportChecked = true, isSupported = false).telemetryStatus(VehicleState(), connection, now))
    }

    @Test fun expiredOrInvalidReadingsLoseAvailabilityAndValue() {
        for (invalid in listOf(reading.copy(receivedAtNanos = now - 5_000_000_000L),
            reading.copy(value = Float.NaN), reading.copy(decodedValue = Double.NaN),
            reading.copy(pidId = "other"), reading.copy(units = "km/h"), reading.copy(receivedAtNanos = now + 1))) {
            assertEquals(if (invalid.receivedAtNanos <= now - 5_000_000_000L) SensorTelemetryStatus.STALE
                else SensorTelemetryStatus.INVALID_RESPONSE, pid.telemetryStatus(state(invalid), connection, now))
            assertNull(pid.currentReading(state(invalid), connection, now))
        }
    }

    @Test fun disconnectedOrDisabledSensorsNeverDisplayCachedValues() {
        val disconnected = connection.copy(status = ConnectionStatus.DISCONNECTED)
        assertEquals(SensorTelemetryStatus.DISCONNECTED, pid.telemetryStatus(state(), disconnected, now))
        assertNull(pid.currentReading(state(), disconnected, now))
        val disabled = pid.copy(isEnabled = false)
        assertEquals(SensorTelemetryStatus.DISABLED, disabled.telemetryStatus(state(), connection, now))
        assertNull(disabled.currentReading(state(), connection, now))
    }

    @Test fun simulationIsClearlyLabeledAndCannotBecomeRealAvailability() {
        val simulatedState = state(reading.copy(isSimulated = true))
        val simulation = connection.copy(type = ConnectionType.SIMULATION)
        assertEquals(SensorTelemetryStatus.SIMULATED, pid.telemetryStatus(simulatedState, simulation, now))
        assertEquals(SensorTelemetryStatus.INVALID_RESPONSE, pid.telemetryStatus(simulatedState, connection, now))
        assertNull(pid.currentReading(simulatedState, connection, now))
        assertNull(pid.currentReading(state(), simulation, now))
    }

    @Test fun retainedReasonsSurviveValueRemovalAndSuccessfulReadingWins() {
        for (reason in listOf(SensorTelemetryStatus.NO_RESPONSE, SensorTelemetryStatus.INVALID_RESPONSE,
            SensorTelemetryStatus.STALE, SensorTelemetryStatus.PAUSED)) {
            val failed = VehicleState(sensorStatuses = mapOf(pid.id to reason))
            assertEquals(reason, pid.telemetryStatus(failed, connection, now))
            assertEquals(SensorTelemetryStatus.AVAILABLE,
                pid.telemetryStatus(failed.copy(readings = state().readings), connection, now))
        }
    }

    @Test fun sameNameDifferentIdsKeepReadingsAndStatusesIndependent() {
        val other = pid.copy(id = "rpm-other", header = "7E1")
        val state = VehicleState(mapOf(pid.id to reading, other.id to reading.copy(pidId = other.id, value = 123f)))
        assertEquals(0f, pid.currentReading(state, connection, now)?.value)
        assertEquals(123f, other.currentReading(state, connection, now)?.value)
        val failed = state.copy(readings = state.readings - pid.id,
            sensorStatuses = mapOf(pid.id to SensorTelemetryStatus.INVALID_RESPONSE))
        assertEquals(SensorTelemetryStatus.INVALID_RESPONSE, pid.telemetryStatus(failed, connection, now))
        assertEquals(SensorTelemetryStatus.AVAILABLE, other.telemetryStatus(failed, connection, now))
    }
}
