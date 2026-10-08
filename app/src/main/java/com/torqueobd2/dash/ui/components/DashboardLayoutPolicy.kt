package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.data.model.DashboardGridLayout
import com.torqueobd2.dash.data.model.DashboardDensity

/** Geometría móvil adaptable y rejilla fija del coche limitada a cuatro columnas y dos filas. */
object DashboardLayoutPolicy {
    data class Metrics(val columns: Int, val rows: Int, val minimumHeight: Float, val gap: Float)
    data class CarGrid(val columns: Int, val rows: Int, val slots: List<Int>, val gap: Float, val split: Boolean)

    /** Resolución y tipografía cambian el tamaño de las celdas, nunca la disposición elegida. */
    fun carGrid(layout: DashboardGridLayout, width: Float, height: Float): CarGrid {
        val chosen = layout.forCar()
        val gap = minOf(16f, width.coerceAtLeast(0f) / (chosen.preferredColumns * 16),
            height.coerceAtLeast(0f) / (chosen.preferredRows * 16))
        return CarGrid(chosen.preferredColumns, chosen.preferredRows, (0 until chosen.slotCount).toList(), gap, chosen.isSplit)
    }

    fun measure(layout: DashboardGridLayout, width: Float, fontScale: Float,
        density: DashboardDensity = DashboardDensity.COMFORTABLE, carMode: Boolean = false): Metrics {
        val scale = fontScale.coerceAtLeast(1f)
        if (carMode) {
            val grid = carGrid(layout, width, Float.MAX_VALUE)
            return Metrics(grid.columns, grid.rows, grid.rows * 192f * scale + (grid.rows - 1) * grid.gap, grid.gap)
        }
        val compact = density == DashboardDensity.COMPACT
        val gap = if (carMode) 24f else if (compact) 4f else 8f
        val minimumWidth = if (carMode) 240f * scale else if (compact) 70f + 18f * scale else 100f + 28f * scale
        val columns = ((width.coerceAtLeast(1f) + gap) / (minimumWidth + gap)).toInt().coerceIn(1, layout.preferredColumns)
        val rows = if (layout.isSplit && columns == 2) 2 else (layout.slotCount + columns - 1) / columns
        val cellHeight = if (carMode) 192f * scale else if (compact) 30f + 40f * scale else 34f + 44f * scale
        return Metrics(columns, rows, rows * cellHeight + (rows - 1) * gap, gap)
    }
}
