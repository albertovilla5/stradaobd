package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.tr
import java.util.UUID

/** Límites elegidos por el usuario; nunca representan un testigo de la ECU. */
data class SensorAlertRule(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val pidId: String,
    val sensorName: String,
    val units: String,
    val lowerLimit: Double? = null,
    val upperLimit: Double? = null,
    val durationSeconds: Int = 3,
    val enabled: Boolean = true
) {
    fun validationError(): String? = when {
        id.isBlank() || vehicleId.isBlank() || pidId.isBlank() -> tr("Selecciona un sensor.", "Select a sensor.")
        lowerLimit == null && upperLimit == null -> tr("Introduce al menos un límite.", "Enter at least one limit.")
        lowerLimit?.isFinite() == false || upperLimit?.isFinite() == false -> tr("Los límites deben ser números finitos.", "Limits must be finite numbers.")
        lowerLimit != null && upperLimit != null && lowerLimit >= upperLimit -> tr("El límite inferior debe ser menor que el superior.", "Lower limit must be less than upper limit.")
        durationSeconds !in 0..3600 -> tr("El tiempo debe estar entre 0 y 3600 segundos.", "Duration must be between 0 and 3600 seconds.")
        else -> null
    }
}

enum class SensorAlertStatus {
    DISABLED, UNAVAILABLE, NORMAL, PENDING, ACTIVE;

    val label: String
        get() = when (this) {
            DISABLED -> tr("Aviso desactivado", "Alert disabled")
            UNAVAILABLE -> tr("Sin vigilancia", "Not monitored")
            NORMAL -> tr("Dentro de límites", "Within limits")
            PENDING -> tr("Esperando persistencia", "Waiting for duration")
            ACTIVE -> tr("Aviso activo", "Active alert")
        }
}

enum class SensorAlertSide { LOWER, UPPER }

data class SensorAlertState(
    val rule: SensorAlertRule,
    val status: SensorAlertStatus,
    val detail: String,
    val reading: ObdReading? = null,
    val side: SensorAlertSide? = null
)
