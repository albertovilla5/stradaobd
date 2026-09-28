package com.torqueobd2.dash.data.model

import java.util.Locale

/**
 * Lectura en tiempo real de un parámetro con su valor actual, mínimos, máximos
 * y formato visual.
 */
data class ObdReading(
    val pidId: String,
    val pidName: String,
    val shortName: String,
    val value: Float,
    val units: String,
    val minValue: Float,
    val maxValue: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formatted(): String {
        return when {
            kotlin.math.abs(value) >= 1000f -> String.format(Locale.US, "%.0f", value)
            kotlin.math.abs(value) >= 100f -> String.format(Locale.US, "%.1f", value)
            kotlin.math.abs(value) >= 10f -> String.format(Locale.US, "%.1f", value)
            else -> String.format(Locale.US, "%.2f", value)
        }
    }

    /**
     * Progreso normalizado entre 0.0 y 1.0 para barras y diales.
     */
    val progress: Float
        get() {
            val range = maxValue - minValue
            return if (range <= 0.0001f) 0f else ((value - minValue) / range).coerceIn(0f, 1f)
        }
}
