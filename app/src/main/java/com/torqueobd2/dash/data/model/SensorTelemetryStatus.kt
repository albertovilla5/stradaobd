package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.tr

/** Compatibilidad y validez de una lectura son evidencias distintas. */
enum class SensorTelemetryStatus {
    AVAILABLE,
    SIMULATED,
    DISCONNECTED,
    DISABLED,
    NO_RESPONSE,
    WAITING,
    INVALID_RESPONSE,
    FIELD_UNAVAILABLE,
    STALE,
    INCOMPATIBLE,
    MISSING,
    PAUSED;

    val label: String
        get() = when (this) {
            AVAILABLE -> tr("Disponible", "Available")
            SIMULATED -> tr("Simulado", "Simulated")
            DISCONNECTED -> tr("Sin OBD", "No OBD")
            DISABLED -> tr("Desactivado", "Disabled")
            NO_RESPONSE -> tr("Sin respuesta", "No response")
            WAITING -> tr("Pendiente de consultar", "Waiting to poll")
            INVALID_RESPONSE -> tr("Respuesta inválida", "Invalid response")
            FIELD_UNAVAILABLE -> tr("Campo no disponible", "Field unavailable")
            STALE -> tr("Dato caducado", "Stale data")
            INCOMPATIBLE -> tr("Sensor incompatible", "Incompatible sensor")
            MISSING -> tr("Sensor no encontrado", "Sensor not found")
            PAUSED -> tr("Lectura pausada", "Polling paused")
        }

    val detail: String
        get() = when (this) {
            AVAILABLE -> tr("Lectura actual y validada de la ECU.", "Current validated reading from the ECU.")
            SIMULATED -> tr("Dato ficticio del simulador.", "Simulated test data.")
            DISCONNECTED -> tr("Conecta un adaptador para consultar el sensor.", "Connect an adapter to poll this sensor.")
            DISABLED -> tr("Activa el sensor para consultarlo.", "Enable the sensor to poll it.")
            NO_RESPONSE -> tr("La ECU no devolvió datos. Se volverá a consultar.", "The ECU returned no data. Will retry.")
            WAITING -> tr(
                "Aún no se ha recibido una lectura. En Solo pantalla se consultan los sensores visibles.",
                "No reading received yet. In Screen only mode, visible sensors are polled."
            )
            INVALID_RESPONSE -> tr(
                "La respuesta no corresponde al comando o no contiene un valor válido para este campo.",
                "Response does not match the command or contains no valid value for this field."
            )
            FIELD_UNAVAILABLE -> tr(
                "La ECU admite el comando, pero indica que este campo no tiene un dato actual.",
                "The ECU supports the command, but indicates this field has no current data."
            )
            STALE -> tr("Han pasado cinco segundos sin una actualización válida.", "Five seconds have elapsed without a valid update.")
            INCOMPATIBLE -> tr(
                "El mapa de soporte o una respuesta explícita de la ECU indica que no admite este sensor.",
                "The support bitmap or an explicit ECU response indicates this sensor is not supported."
            )
            MISSING -> tr(
                "La definición guardada no está en la biblioteca o su referencia antigua es ambigua. Selecciona el sensor de nuevo.",
                "Saved definition is missing from the library or its legacy reference is ambiguous. Select the sensor again."
            )
            PAUSED -> tr(
                "Las consultas están pausadas mientras la app está en segundo plano.",
                "Queries are paused while the app is in the background."
            )
        }
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
