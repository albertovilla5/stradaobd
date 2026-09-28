package com.torqueobd2.dash.data.model

import java.util.UUID

/**
 * Representa un PID OBD2 (parámetro de diagnóstico a bordo), compatible con
 * el formato estándar de Torque Pro y con bandera de disponibilidad en la ECU del coche.
 */
data class ObdPid(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortName: String,
    val modeAndPid: String, // ej. "0x222512" o "010C"
    val equation: String,   // ej. "INT16(V:W)/10" o "((A*256)+B)/4"
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val units: String = "",
    val header: String = "", // ej. "DA10F1" o "" (vacío usa el header actual)
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val isSupported: Boolean = true, // Indica si la ECU del coche soporta este sensor
    val category: PidCategory = PidCategory.ENGINE
) {
    /**
     * Devuelve el comando exacto para enviar al ELM327 (ej. "222512" o "010C").
     */
    fun cleanObdCommand(): String {
        var clean = modeAndPid.trim()
        if (clean.startsWith("0x", ignoreCase = true)) {
            clean = clean.substring(2)
        }
        return clean.uppercase()
    }
}

enum class PidCategory {
    ENGINE,
    TRANSMISSION,
    EXHAUST_DPF,
    FUEL,
    ELECTRICAL,
    CUSTOM
}
