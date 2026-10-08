package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import org.junit.Assert.*
import org.junit.Test

class SensorAlertEvaluatorTest {
    private val pid = ObdPid(id = "temperature", name = "Temperatura", shortName = "Temp",
        modeAndPid = "0105", equation = "A-40", units = "°C")
    private val rule = SensorAlertRule(id = "alert", vehicleId = "car", pidId = pid.id,
        sensorName = pid.name, units = pid.units, lowerLimit = 10.0, upperLimit = 100.0, durationSeconds = 3)
    private val connection = ConnectionState(status = ConnectionStatus.CONNECTED)
    private val engine = SensorAlertEvaluator()
    private fun reading(value: Double, seconds: Double, simulated: Boolean = false) = ObdReading(
        pid.id, pid.name, pid.shortName, value.toFloat(), pid.units, 0f, 150f,
        receivedAtNanos = (seconds * 1_000_000_000).toLong(), decodedValue = value, isSimulated = simulated)
    private fun evaluate(value: Double, seconds: Double, currentRule: SensorAlertRule = rule,
        currentPid: ObdPid = pid): SensorAlertState = engine.evaluate(listOf(currentRule), listOf(currentPid),
        VehicleState(readings = mapOf(pid.id to reading(value, seconds))), connection,
        (seconds * 1_000_000_000).toLong()).single()

    @Test fun sustainedNewSamplesActivateAtDurationAndRecoveryClearsImmediately() {
        assertEquals(SensorAlertStatus.PENDING, evaluate(101.0, 0.0).status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 2.9).status)
        assertEquals(SensorAlertStatus.ACTIVE, evaluate(111.0, 3.0).status)
        assertEquals(SensorAlertStatus.ACTIVE, evaluate(112.0, 4.0).status)
        assertEquals(SensorAlertStatus.NORMAL, evaluate(100.0, 5.0).status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(101.0, 6.0).status)
    }

    @Test fun repeatedOldReadingCannotProvePersistenceAndExpires() {
        val vehicle = VehicleState(readings = mapOf(pid.id to reading(110.0, 0.0)))
        assertEquals(SensorAlertStatus.PENDING, engine.evaluate(listOf(rule), listOf(pid), vehicle, connection, 0).single().status)
        assertEquals(SensorAlertStatus.PENDING, engine.evaluate(listOf(rule), listOf(pid), vehicle, connection, 4_000_000_000).single().status)
        assertEquals(SensorAlertStatus.UNAVAILABLE, engine.evaluate(listOf(rule), listOf(pid), vehicle, connection, 5_000_000_000).single().status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 6.0).status)
    }

    @Test fun changingSideAndSingleSpikesRestartPersistence() {
        evaluate(110.0, 0.0)
        assertEquals(SensorAlertStatus.PENDING, evaluate(9.0, 3.0).status)
        assertEquals(SensorAlertStatus.ACTIVE, evaluate(8.0, 6.0).status)
        assertEquals(SensorAlertSide.LOWER, evaluate(8.0, 6.5).side)
        assertEquals(SensorAlertStatus.NORMAL, evaluate(10.0, 7.0).status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 8.0).status)
    }

    @Test fun invalidMissingOrDisconnectedSamplesResetAnActiveAlert() {
        val badSamples = listOf(
            VehicleState(sensorStatuses = mapOf(pid.id to SensorTelemetryStatus.NO_RESPONSE)),
            VehicleState(sensorStatuses = mapOf(pid.id to SensorTelemetryStatus.FIELD_UNAVAILABLE)),
            VehicleState(readings = mapOf(pid.id to reading(Double.NaN, 4.0))),
            VehicleState(readings = mapOf(pid.id to reading(110.0, 4.0).copy(units = "F"))),
            VehicleState(readings = mapOf(pid.id to reading(110.0, 4.0, simulated = true)))
        )
        for (sample in badSamples) {
            engine.reset()
            evaluate(110.0, 0.0); assertEquals(SensorAlertStatus.ACTIVE, evaluate(110.0, 3.0).status)
            assertEquals(SensorAlertStatus.UNAVAILABLE, engine.evaluate(listOf(rule), listOf(pid), sample, connection, 4_000_000_000).single().status)
            assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 5.0).status)
        }
        val disconnected = connection.copy(status = ConnectionStatus.DISCONNECTED)
        assertEquals(SensorAlertStatus.UNAVAILABLE, engine.evaluate(listOf(rule), listOf(pid),
            VehicleState(readings = mapOf(pid.id to reading(110.0, 8.0))), disconnected, 8_000_000_000).single().status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 9.0).status)
    }

    @Test fun disabledRuleSensorIncompatibleAndMissingSensorDoNotAlert() {
        assertEquals(SensorAlertStatus.DISABLED, evaluate(110.0, 0.0, rule.copy(enabled = false)).status)
        assertEquals(SensorAlertStatus.UNAVAILABLE, evaluate(110.0, 0.0, currentPid = pid.copy(isEnabled = false)).status)
        assertEquals(SensorAlertStatus.UNAVAILABLE, evaluate(110.0, 0.0, currentPid = pid.copy(isSupported = false, supportChecked = true)).status)
        assertEquals(SensorAlertStatus.UNAVAILABLE, engine.evaluate(listOf(rule), emptyList(), VehicleState(), connection, 0).single().status)
    }

    @Test fun editingRulesDefinitionsOrUnitsRequiresFreshPersistence() {
        evaluate(110.0, 0.0)
        val changed = rule.copy(upperLimit = 105.0)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 3.0, changed).status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 6.0, changed, pid.copy(equation = "A")).status)
        assertEquals(SensorAlertStatus.UNAVAILABLE, evaluate(110.0, 9.0, changed, pid.copy(units = "F")).status)
    }

    @Test fun identityIsByPidIdAndPreciseDecodedValueIsUsed() {
        val precise = rule.copy(upperLimit = 100.0000001, durationSeconds = 0)
        val result = evaluate(100.0000002, 0.0, precise)
        assertEquals(100f, result.reading!!.value)
        assertEquals(SensorAlertStatus.ACTIVE, result.status)
        val homonym = pid.copy(id = "another", header = "7E1")
        assertEquals(SensorAlertStatus.UNAVAILABLE, engine.evaluate(listOf(precise), listOf(pid, homonym),
            VehicleState(readings = mapOf(homonym.id to reading(110.0, 1.0).copy(pidId = homonym.id))), connection, 1_000_000_000).single().status)
    }

    @Test fun simulationIsExplicitAndSessionResetCannotCarryPersistence() {
        val result = engine.evaluate(listOf(rule.copy(durationSeconds = 0)), listOf(pid),
            VehicleState(readings = mapOf(pid.id to reading(110.0, 0.0, simulated = true))),
            connection.copy(type = ConnectionType.SIMULATION), 0).single()
        assertEquals(SensorAlertStatus.ACTIVE, result.status)
        assertTrue(result.reading!!.isSimulated)
        engine.reset()
        evaluate(110.0, 0.0)
        engine.reset()
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 4.0).status)
    }

    @Test fun longGapAndRemovedRulesDoNotCarryPersistence() {
        evaluate(110.0, 0.0)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 5.0).status)
        assertTrue(engine.evaluate(emptyList(), listOf(pid), VehicleState(), connection, 6_000_000_000).isEmpty())
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 8.0).status)
    }

    @Test fun validationRejectsInvalidLimitsAndDuration() {
        assertNull(rule.validationError())
        assertNull(rule.copy(lowerLimit = null, durationSeconds = 0).validationError())
        assertNotNull(rule.copy(lowerLimit = null, upperLimit = null).validationError())
        assertNotNull(rule.copy(lowerLimit = 100.0).validationError())
        assertNotNull(rule.copy(upperLimit = Double.POSITIVE_INFINITY).validationError())
        assertNotNull(rule.copy(lowerLimit = Double.NaN).validationError())
        assertNotNull(rule.copy(durationSeconds = -1).validationError())
        assertNotNull(rule.copy(durationSeconds = 3601).validationError())
    }

    @Test fun enablingOrEditingAnAlertStartsItsDurationNowEvenWithAFreshOlderReading() {
        val old = VehicleState(readings = mapOf(pid.id to reading(110.0, 0.0)))
        assertEquals(SensorAlertStatus.PENDING, engine.evaluate(listOf(rule), listOf(pid), old, connection, 2_000_000_000).single().status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 3.0).status)
        assertEquals(SensorAlertStatus.ACTIVE, evaluate(110.0, 5.0).status)
        val changed = rule.copy(upperLimit = 105.0)
        val retained = VehicleState(readings = mapOf(pid.id to reading(110.0, 5.0)))
        assertEquals(SensorAlertStatus.PENDING, engine.evaluate(listOf(changed), listOf(pid), retained, connection, 6_000_000_000).single().status)
        assertEquals(SensorAlertStatus.PENDING, evaluate(110.0, 8.0, changed).status)
        assertEquals(SensorAlertStatus.ACTIVE, evaluate(110.0, 9.0, changed).status)
        assertEquals(SensorAlertStatus.ACTIVE, engine.evaluate(listOf(changed.copy(durationSeconds = 0)), listOf(pid), retained, connection, 6_000_000_000).single().status)
    }
}
