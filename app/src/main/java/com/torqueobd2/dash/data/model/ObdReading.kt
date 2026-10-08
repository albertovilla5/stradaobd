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
    val timestamp: Long = System.currentTimeMillis(),
    val decodedValue: Double = value.toDouble(),
    val isSimulated: Boolean = false,
    val receivedAtNanos: Long = System.nanoTime(),
    val valueLabels: Map<Int, String> = emptyMap()
) {
    fun formatted(): String {
        if (!value.isFinite() || !decodedValue.isFinite()) return "—"
        if (decodedValue == decodedValue.toInt().toDouble()) valueLabels[decodedValue.toInt()]?.let { return it }
        return when {
            kotlin.math.abs(decodedValue) >= 1000 -> String.format(Locale.US, "%.0f", decodedValue)
            kotlin.math.abs(decodedValue) >= 10 -> String.format(Locale.US, "%.1f", decodedValue)
            else -> String.format(Locale.US, "%.2f", decodedValue)
        }
    }

    /**
     * Progreso normalizado entre 0.0 y 1.0 para barras y diales.
     */
    val progress: Float
        get() {
            val range = maxValue - minValue
            return if (!value.isFinite() || range <= 0.0001f) 0f else ((value - minValue) / range).coerceIn(0f, 1f)
        }
}
