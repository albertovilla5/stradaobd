package com.torqueobd2.dash.data.model

/** Compatibilidad y validez de una lectura son evidencias distintas. */
enum class SensorTelemetryStatus(val label: String, val detail: String) {
    AVAILABLE("Disponible", "Lectura actual y validada de la ECU."),
    SIMULATED("Simulado", "Dato ficticio del simulador."),
    DISCONNECTED("Sin OBD", "Conecta un adaptador para consultar el sensor."),
    DISABLED("Desactivado", "Activa el sensor para consultarlo."),
    NO_RESPONSE("Sin respuesta", "La ECU no devolvió datos. Se volverá a consultar."),
    WAITING("Pendiente de consultar", "Aún no se ha recibido una lectura. En Solo pantalla se consultan los sensores visibles."),
    INVALID_RESPONSE("Respuesta inválida", "La respuesta no corresponde al comando o no contiene un valor válido para este campo."),
    FIELD_UNAVAILABLE("Campo no disponible", "La ECU admite el comando, pero indica que este campo no tiene un dato actual."),
    STALE("Dato caducado", "Han pasado cinco segundos sin una actualización válida."),
    INCOMPATIBLE("Sensor incompatible", "El mapa de soporte o una respuesta explícita de la ECU indica que no admite este sensor."),
    MISSING("Sensor no encontrado", "La definición guardada no está en la biblioteca o su referencia antigua es ambigua. Selecciona el sensor de nuevo."),
    PAUSED("Lectura pausada", "Las consultas están pausadas mientras la app está en segundo plano.")
}

fun ObdPid.currentReading(vehicleState: VehicleState, connection: ConnectionState,
    nowNanos: Long = System.nanoTime()): ObdReading? {
    if (!isEnabled || !isSupported || connection.status != ConnectionStatus.CONNECTED) return null
    return vehicleState.readings[id]?.takeIf {
        val age = nowNanos - it.receivedAtNanos
        it.pidId == id && it.units == units && it.value.isFinite() && it.decodedValue.isFinite() &&
            age >= 0 && age < 5_000_000_000L &&
            it.isSimulated == (connection.type == ConnectionType.SIMULATION)
    }
}

fun ObdPid.telemetryStatus(vehicleState: VehicleState, connection: ConnectionState,
    nowNanos: Long = System.nanoTime()): SensorTelemetryStatus = when {
    !isEnabled -> SensorTelemetryStatus.DISABLED
    connection.status != ConnectionStatus.CONNECTED -> SensorTelemetryStatus.DISCONNECTED
    supportChecked && !isSupported -> SensorTelemetryStatus.INCOMPATIBLE
    currentReading(vehicleState, connection, nowNanos) != null ->
        if (connection.type == ConnectionType.SIMULATION) SensorTelemetryStatus.SIMULATED else SensorTelemetryStatus.AVAILABLE
    vehicleState.readings[id]?.let { nowNanos - it.receivedAtNanos >= 5_000_000_000L } == true -> SensorTelemetryStatus.STALE
    vehicleState.readings[id] != null -> SensorTelemetryStatus.INVALID_RESPONSE
    else -> vehicleState.sensorStatuses[id] ?: SensorTelemetryStatus.WAITING
}
