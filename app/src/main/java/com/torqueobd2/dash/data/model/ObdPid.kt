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
    val isSupported: Boolean = true, // Provisional hasta supportChecked; la ECU puede omitir campos individuales.
    val category: PidCategory = PidCategory.ENGINE,
    val supportChecked: Boolean = false,
    // Bits de presencia y sentinelas del protocolo: un campo ausente nunca equivale a cero.
    val availabilityEquation: String = "",
    val unavailableRawValues: Map<String, Set<Int>> = emptyMap(),
    val valueLabels: Map<Int, String> = emptyMap()
) {
    val supportDescription: String
        get() = when {
            !supportChecked -> "Por comprobar"
            isSupported -> "PID compatible"
            else -> "No disponible"
        }

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

enum class PidCategory(val title: String) {
    ENGINE("Motor"),
    TRANSMISSION("Transmisión"),
    EXHAUST_DPF("Escape / DPF"),
    FUEL("Combustible"),
    ELECTRICAL("Eléctrico / Híbrido"),
    CUSTOM("Personalizados"),
    OXYGEN("Oxígeno / Mezcla"),
    DIAGNOSTICS("Diagnóstico")
}
