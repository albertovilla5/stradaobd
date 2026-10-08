package com.torqueobd2.dash.data.model

import java.util.UUID

enum class WidgetType(val displayName: String) {
    ANALOG_GAUGE("Reloj Analógico con Aguja"),
    DIGITAL_HUD("Display Digital Grande"),
    HORIZONTAL_BAR("Barra de Nivel con Graduación"),
    SPARKLINE_CHART("Gráfico Histórico en Tiempo Real"),
    COMPACT_TILE("Tarjeta Compacta")
}

enum class WidgetSize(val displayName: String) {
    STANDARD("Tamaño Estándar (1x1)"),
    FULL_WIDTH("Ancho Completo (2x1)")
}

enum class DashboardGridLayout(
    val id: String,
    val title: String,
    val subtitle: String,
    val slotCount: Int,
    val badge: String
) {
    GRID_1X1("1x1", "Un indicador", "Una lectura protagonista", 1, "1 x 1"),
    GRID_1X2("1x2", "2 Columnas Grandes", "2 relojes de gran tamaño", 2, "1 x 2"),
    GRID_1X3("1x3", "3 Columnas", "3 relojes medianos en línea", 3, "1 x 3"),
    GRID_1X4("1x4", "4 Columnas", "4 relojes clásicos de salpicadero", 4, "1 x 4"),
    GRID_2X1("2x1", "2 Filas", "Dos indicadores apilados", 2, "2 x 1"),
    GRID_2X2("2x2", "Matriz 2x2", "4 cuadrantes simétricos", 4, "2 x 2"),
    GRID_2X3("2x3", "Matriz 2x3", "6 indicadores compactos", 6, "2 x 3"),
    GRID_2X4("2x4", "Matriz 2x4", "8 indicadores", 8, "2 x 4"),
    GRID_2X5("2x5", "Matriz 2x5", "10 indicadores", 10, "2 x 5"),
    GRID_2X6("2x6", "Matriz 2x6", "12 indicadores en formato ancho", 12, "2 x 6"),
    GRID_3X3("3x3", "Matriz 3x3", "9 indicadores", 9, "3 x 3"),
    GRID_3X4("3x4", "Matriz 3x4", "12 indicadores", 12, "3 x 4"),
    GRID_3X5("3x5", "Matriz 3x5", "15 indicadores", 15, "3 x 5"),
    GRID_3X6("3x6", "Matriz 3x6", "18 indicadores", 18, "3 x 6"),
    GRID_4X4("4x4", "Matriz 4x4", "16 indicadores", 16, "4 x 4"),
    GRID_4X5("4x5", "Matriz 4x5", "20 indicadores", 20, "4 x 5"),
    GRID_4X6("4x6", "Matriz 4x6", "24 indicadores · Ideal en pantalla completa", 24, "4 x 6"),
    SPLIT_1_2("1+2", "1 Principal + 2 Laterales", "1 grande a la izquierda, 2 apilados a la derecha", 3, "1 + 2"),
    SPLIT_2_1("2+1", "2 Laterales + 1 Principal", "2 apilados a la izquierda, 1 grande a la derecha", 3, "2 + 1");

    val preferredColumns: Int get() = if (id.contains('x')) id.substringAfter('x').toInt() else 2
    val isSplit: Boolean get() = id.contains('+')
    val preferredRows: Int get() = if (isSplit) 2 else slotCount / preferredColumns
    val carSupported: Boolean get() = preferredColumns <= 4 && preferredRows <= 2
    val carTitle: String get() = if (isSplit) title else "$preferredColumns ${if (preferredColumns == 1) "columna" else "columnas"} × $preferredRows ${if (preferredRows == 1) "fila" else "filas"}"
    val carBadge: String get() = if (isSplit) badge else "$preferredColumns × $preferredRows"
    fun forCar(): DashboardGridLayout = if (carSupported) this else carLayouts.first {
        !it.isSplit && it.preferredColumns == preferredColumns.coerceAtMost(4) && it.preferredRows == preferredRows.coerceAtMost(2)
    }
    companion object {
        val carLayouts: List<DashboardGridLayout> get() = listOf(GRID_1X1, GRID_1X2, GRID_1X3, GRID_1X4,
            GRID_2X1, GRID_2X2, GRID_2X3, GRID_2X4, SPLIT_1_2, SPLIT_2_1)
    }
}

enum class DashboardDensity(val title: String) { COMFORTABLE("Normal"), COMPACT("Compacta") }

data class DashboardWidget(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType = WidgetType.ANALOG_GAUGE,
    val pidShortName: String = "RPM",
    val title: String = "Revoluciones",
    val colorHex: String = "#FF6D00",
    val size: WidgetSize = WidgetSize.STANDARD,
    val pidId: String = "", // Vacío solo para migrar cuadros antiguos.
    val decimals: Int? = null,
    val scaleMin: Float? = null,
    val scaleMax: Float? = null
) {
    fun displayValue(reading: ObdReading?): String {
        if (reading == null || !reading.value.isFinite() || !reading.decodedValue.isFinite()) return "—"
        if (reading.decodedValue == reading.decodedValue.toInt().toDouble()) reading.valueLabels[reading.decodedValue.toInt()]?.let { return it }
        return decimals?.takeIf { it in 0..4 }?.let { java.lang.String.format(java.util.Locale.US, "%.${it}f", reading.decodedValue) } ?: reading.formatted()
    }
    val hasCustomScale: Boolean get() = scaleMin != null && scaleMax != null && scaleMin.isFinite() && scaleMax.isFinite() && scaleMin < scaleMax && (scaleMax - scaleMin).isFinite()
    fun scale(pid: ObdPid?): Pair<Float, Float> = if (hasCustomScale) scaleMin!! to scaleMax!! else (pid?.minValue ?: 0f) to (pid?.maxValue ?: 100f)
}

data class CustomDashboardConfig(
    val name: String = "Mi Dashboard Personalizado",
    val gridLayout: DashboardGridLayout = DashboardGridLayout.GRID_1X4,
    val slots: Map<Int, DashboardWidget> = emptyMap(),
    val widgets: List<DashboardWidget> = getDefaultWidgets(),
    val id: String = UUID.randomUUID().toString(),
    val density: DashboardDensity = DashboardDensity.COMFORTABLE,
    val carGridLayout: DashboardGridLayout? = null
) {
    /** Presentación del coche independiente; las ranuras ocultas siguen guardadas para el móvil. */
    fun forCar(): CustomDashboardConfig = copy(gridLayout = (carGridLayout ?: gridLayout).forCar())

    /**
     * Obtiene el widget asignado a una ranura específica.
     */
    fun getWidgetForSlot(slotIndex: Int): DashboardWidget? {
        return slots[slotIndex] ?: widgets.getOrNull(slotIndex)
    }

    /**
     * Devuelve la lista exacta de ranuras según el gridLayout activo.
     */
    fun getSlotList(): List<DashboardWidget?> {
        return (0 until gridLayout.slotCount).map { index -> getWidgetForSlot(index) }
    }

    /** Materializa la configuración antigua sin desplazar los huecos al editar. */
    fun resolvedSlots(): Map<Int, DashboardWidget> =
        widgets.mapIndexed { index, widget -> index to widget }.toMap() + slots

    fun withSlot(index: Int, widget: DashboardWidget?): CustomDashboardConfig {
        val updated = resolvedSlots().toMutableMap()
        if (widget == null) updated.remove(index) else updated[index] = widget
        return copy(slots = updated, widgets = emptyList())
    }

    /** Mover intercambia ranuras ocupadas; duplicar conserva el original y requiere un hueco. */
    fun transferSlot(source: Int, destination: Int, duplicate: Boolean = false): CustomDashboardConfig {
        if (source !in 0 until gridLayout.slotCount || destination !in 0 until gridLayout.slotCount || source == destination) return this
        val widget = getWidgetForSlot(source) ?: return this
        val target = getWidgetForSlot(destination)
        if (duplicate && target != null) return this
        val updated = resolvedSlots().toMutableMap()
        updated[destination] = if (duplicate) widget.copy(id = UUID.randomUUID().toString()) else widget
        if (!duplicate) { if (target == null) updated.remove(source) else updated[source] = target }
        return copy(slots = updated, widgets = emptyList())
    }
    fun fillEmptySlots(additions: List<DashboardWidget>): CustomDashboardConfig {
        val empty = (0 until gridLayout.slotCount).filter { getWidgetForSlot(it) == null }
        if (empty.isEmpty() || additions.isEmpty()) return this
        val updated = resolvedSlots().toMutableMap()
        empty.zip(additions).forEach { (slot, widget) -> updated[slot] = widget }
        return copy(slots = updated, widgets = emptyList())
    }
    fun duplicateAs(name: String): CustomDashboardConfig = copy(id = UUID.randomUUID().toString(), name = name,
        widgets = emptyList(), slots = resolvedSlots().mapValues { it.value.copy(id = UUID.randomUUID().toString()) })

    companion object {
        fun getDefaultWidgets(): List<DashboardWidget> = listOf(
            DashboardWidget(pidId = SensorIds.RPM, pidShortName = "RPM", title = "Revoluciones"),
            DashboardWidget(pidId = SensorIds.SPEED, pidShortName = "Velocidad", title = "Velocidad", colorHex = "#00E5FF"),
            DashboardWidget(pidId = SensorIds.COOLANT, pidShortName = "Temp Liq", title = "Refrigerante", colorHex = "#00E676"),
            DashboardWidget(pidId = SensorIds.VOLTAGE, pidShortName = "Voltaje", title = "Voltaje ECU", colorHex = "#FFD600")
        )
    }
}

data class DashboardPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val gridLayout: DashboardGridLayout = DashboardGridLayout.GRID_1X4,
    val widgets: List<DashboardWidget>
)

object DashboardPresets {
    fun getAllPresets(): List<DashboardPreset> = listOf(
        DashboardPreset(
            id = "sport",
            title = "Conducción",
            subtitle = "RPM, velocidad, refrigerante y voltaje ECU",
            iconEmoji = "🏁",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = CustomDashboardConfig.getDefaultWidgets()
        ),
        DashboardPreset(
            id = "default",
            title = "Predeterminado Completo",
            subtitle = "Cuatro lecturas básicas para conducir",
            iconEmoji = "⚙️",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = CustomDashboardConfig.getDefaultWidgets()
        ),
        DashboardPreset(
            id = "diesel_dpf",
            title = "Diésel & Filtro DPF",
            subtitle = "Presión diferencial DPF, EGT, Refrigerante y Carga",
            iconEmoji = "🛢️",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "DPF diff.pres", title = "Presión DPF", colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "EGT", title = "Temp. Escape EGT", colorHex = "#FF1744"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp Liq", title = "Temp. Refrigerante", colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Carga", title = "Carga Motor", colorHex = "#FF6D00")
            )
        ),
        DashboardPreset(
            id = "turbo_intake",
            title = "Turbo y Admisión",
            subtitle = "Presión MAP, Flujo MAF, Temp Admisión y RPM",
            iconEmoji = "💨",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "MAP", title = "MAP absoluta", colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "MAF", title = "Flujo Aire MAF", colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp Adm", title = "Temp. Admisión", colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "RPM", title = "Tacómetro Motor", colorHex = "#FF1744")
            )
        ),
        DashboardPreset(
            id = "eco_trip",
            title = "Viaje y Eficiencia",
            subtitle = "Velocímetro digital, Consumo L/h, Depósito y Batería",
            iconEmoji = "🚗",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = listOf(
                DashboardWidget(type = WidgetType.DIGITAL_HUD, pidShortName = "Velocidad", title = "Velocidad", colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Consumo", title = "Consumo Instantáneo", colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.HORIZONTAL_BAR, pidShortName = "Nivel Dep", title = "Nivel de Depósito", colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.COMPACT_TILE, pidShortName = "Voltaje", title = "Voltaje Batería", colorHex = "#FFD600")
            )
        )
    )
    fun forPids(pids: List<ObdPid>): List<DashboardPreset> = getAllPresets().mapNotNull { preset ->
        val widgets = preset.widgets.mapNotNull { widget ->
            widget.resolvePid(pids)?.takeIf { !it.supportChecked || it.isSupported }
                ?.let { widget.copy(pidId = it.id) }
        }
        if (widgets.isEmpty()) null else preset.copy(widgets = widgets,
            subtitle = "${widgets.size} sensores · " +
                if (widgets.all { widget -> pids.single { it.id == widget.pidId }.supportChecked }) "Compatibles con la ECU"
                else "Compatibilidad por comprobar")
    }

}


/** El orden y la selección usan identidades estables, independientes del nombre. */
data class CustomDashboardTabs(
    val dashboards: List<CustomDashboardConfig> = listOf(CustomDashboardConfig(name = "Mi dash")),
    val selectedId: String = dashboards.first().id
) {
    init {
        require(dashboards.isNotEmpty())
        require(dashboards.map { it.id }.distinct().size == dashboards.size)
        require(dashboards.any { it.id == selectedId })
    }

    val selectedDashboard: CustomDashboardConfig get() = dashboards.first { it.id == selectedId }
    val selectedIndex: Int get() = dashboards.indexOfFirst { it.id == selectedId }

    fun select(id: String): CustomDashboardTabs =
        if (dashboards.any { it.id == id }) copy(selectedId = id) else this

    fun add(name: String, layout: DashboardGridLayout): CustomDashboardTabs {
        if (name.isBlank()) return this
        val dashboard = CustomDashboardConfig(name = name.trim(), gridLayout = layout, widgets = emptyList())
        return copy(dashboards = dashboards + dashboard, selectedId = dashboard.id)
    }

    fun rename(id: String, name: String): CustomDashboardTabs =
        if (name.isBlank()) this else copy(dashboards = dashboards.map { if (it.id == id) it.copy(name = name.trim()) else it })

    fun move(id: String, destination: Int): CustomDashboardTabs {
        val source = dashboards.indexOfFirst { it.id == id }
        if (source < 0 || destination !in dashboards.indices || source == destination) return this
        val reordered = dashboards.toMutableList()
        reordered.add(destination, reordered.removeAt(source))
        return copy(dashboards = reordered)
    }

    fun duplicate(id: String): CustomDashboardTabs {
        val original = dashboards.firstOrNull { it.id == id } ?: return this
        val duplicated = original.duplicateAs("${original.name.take(72)} · Copia")
        val updated = dashboards.toMutableList().apply { add(dashboards.indexOf(original) + 1, duplicated) }
        return copy(dashboards = updated, selectedId = duplicated.id)
    }
    fun delete(id: String): CustomDashboardTabs {
        if (dashboards.size == 1 || dashboards.none { it.id == id }) return this
        val index = dashboards.indexOfFirst { it.id == id }
        val updated = dashboards.filterNot { it.id == id }
        return copy(dashboards = updated, selectedId = if (selectedId == id) updated[index.coerceAtMost(updated.lastIndex)].id else selectedId)
    }
    fun appendImported(imported: List<CustomDashboardConfig>): CustomDashboardTabs {
        if (imported.isEmpty()) return this
        val copies = imported.map { it.duplicateAs(it.name) }
        return copy(dashboards = dashboards + copies, selectedId = copies.first().id)
    }

    fun updateSelected(config: CustomDashboardConfig): CustomDashboardTabs =
        copy(dashboards = dashboards.map { if (it.id == selectedId) config.copy(id = it.id, name = it.name) else it })
}
