package com.torqueobd2.dash.data.model

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
        id.isBlank() || vehicleId.isBlank() || pidId.isBlank() -> "Selecciona un sensor."
        lowerLimit == null && upperLimit == null -> "Introduce al menos un límite."
        lowerLimit?.isFinite() == false || upperLimit?.isFinite() == false -> "Los límites deben ser números finitos."
        lowerLimit != null && upperLimit != null && lowerLimit >= upperLimit -> "El límite inferior debe ser menor que el superior."
        durationSeconds !in 0..3600 -> "El tiempo debe estar entre 0 y 3600 segundos."
        else -> null
    }
}

enum class SensorAlertStatus(val label: String) {
    DISABLED("Aviso desactivado"), UNAVAILABLE("Sin vigilancia"), NORMAL("Dentro de límites"),
    PENDING("Esperando persistencia"), ACTIVE("Aviso activo")
}

enum class SensorAlertSide { LOWER, UPPER }

data class SensorAlertState(
    val rule: SensorAlertRule,
    val status: SensorAlertStatus,
    val detail: String,
    val reading: ObdReading? = null,
    val side: SensorAlertSide? = null
)
