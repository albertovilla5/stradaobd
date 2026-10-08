package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*

/** La persistencia se confirma con muestras nuevas, usando el reloj monotónico. */
class SensorAlertEvaluator {
    private data class Pending(val rule: SensorAlertRule, val definition: ObdPid,
        val side: SensorAlertSide, val sinceNanos: Long, val lastNanos: Long)
    private val pending = mutableMapOf<String, Pending>()

    fun reset() = pending.clear()

    fun evaluate(rules: List<SensorAlertRule>, pids: List<ObdPid>, vehicle: VehicleState,
        connection: ConnectionState, nowNanos: Long = System.nanoTime()): List<SensorAlertState> {
        pending.keys.retainAll(rules.map { it.id }.toSet())
        val definitions = pids.associateBy { it.id }
        return rules.map { rule ->
            val pid = definitions[rule.pidId]
            val invalid = when {
                !rule.enabled -> "El aviso está desactivado."
                rule.validationError() != null -> rule.validationError()
                pid == null -> "Sensor no encontrado. Edita el aviso para reasignarlo."
                pid.units != rule.units -> "La unidad del sensor ha cambiado. Revisa los límites."
                pid.currentReading(vehicle, connection, nowNanos) == null -> pid.telemetryStatus(vehicle, connection, nowNanos).label
                else -> null
            }
            if (invalid != null) {
                pending.remove(rule.id)
                return@map SensorAlertState(rule,
                    if (rule.enabled) SensorAlertStatus.UNAVAILABLE else SensorAlertStatus.DISABLED, invalid)
            }
            val reading = pid!!.currentReading(vehicle, connection, nowNanos)!!
            val side = when {
                rule.lowerLimit != null && reading.decodedValue < rule.lowerLimit -> SensorAlertSide.LOWER
                rule.upperLimit != null && reading.decodedValue > rule.upperLimit -> SensorAlertSide.UPPER
                else -> null
            }
            if (side == null) {
                pending.remove(rule.id)
                return@map SensorAlertState(rule, SensorAlertStatus.NORMAL, "Dentro de los límites configurados.", reading)
            }
            val definition = pid.copy(isEnabled = true, isSupported = true, supportChecked = false)
            val previous = pending[rule.id]?.takeIf {
                it.rule == rule && it.definition == definition && it.side == side &&
                    reading.receivedAtNanos >= it.lastNanos && reading.receivedAtNanos - it.lastNanos < 5_000_000_000L
            }
            // Una regla recién activada o editada no hereda tiempo anterior a su evaluación.
            val since = previous?.sinceNanos ?: maxOf(reading.receivedAtNanos, nowNanos)
            pending[rule.id] = Pending(rule, definition, side, since, reading.receivedAtNanos)
            // Un solo dato no demuestra que el límite se mantuvo durante el tiempo requerido.
            val active = rule.durationSeconds == 0 || reading.receivedAtNanos - since >= rule.durationSeconds * 1_000_000_000L
            SensorAlertState(rule, if (active) SensorAlertStatus.ACTIVE else SensorAlertStatus.PENDING,
                if (side == SensorAlertSide.LOWER) "Por debajo del límite inferior" else "Por encima del límite superior", reading, side)
        }
    }
}
