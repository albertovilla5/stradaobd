package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.AppLanguage
import com.torqueobd2.dash.i18n.tr
import java.util.UUID

enum class WidgetType {
    ANALOG_GAUGE,
    DIGITAL_HUD,
    HORIZONTAL_BAR,
    SPARKLINE_CHART,
    COMPACT_TILE;

    val displayName: String
        get() = when (this) {
            ANALOG_GAUGE -> tr("Reloj Analógico con Aguja", "Analog Needle Gauge")
            DIGITAL_HUD -> tr("Display Digital Grande", "Large Digital HUD")
            HORIZONTAL_BAR -> tr("Barra de Nivel con Graduación", "Graduated Level Bar")
            SPARKLINE_CHART -> tr("Gráfico Histórico en Tiempo Real", "Real-Time History Chart")
            COMPACT_TILE -> tr("Tarjeta Compacta", "Compact Tile")
        }
}

enum class WidgetSize {
    STANDARD,
    FULL_WIDTH;

    val displayName: String
        get() = when (this) {
            STANDARD -> tr("Tamaño Estándar (1x1)", "Standard Size (1x1)")
            FULL_WIDTH -> tr("Ancho Completo (2x1)", "Full Width (2x1)")
        }
}

enum class DashboardGridLayout(
    val id: String,
    private val esTitle: String,
    private val enTitle: String,
    private val esSubtitle: String,
    private val enSubtitle: String,
    val slotCount: Int,
    val badge: String
) {
    GRID_1X1("1x1", "Un indicador", "Single gauge", "Una lectura protagonista", "One featured reading", 1, "1 x 1"),
    GRID_1X2("1x2", "2 Columnas Grandes", "2 Large Columns", "2 relojes de gran tamaño", "2 large gauges", 2, "1 x 2"),
    GRID_1X3("1x3", "3 Columnas", "3 Columns", "3 relojes medianos en línea", "3 medium gauges in a row", 3, "1 x 3"),
    GRID_1X4("1x4", "4 Columnas", "4 Columns", "4 relojes clásicos de salpicadero", "4 classic dashboard gauges", 4, "1 x 4"),
    GRID_2X1("2x1", "2 Filas", "2 Rows", "Dos indicadores apilados", "Two stacked gauges", 2, "2 x 1"),
    GRID_2X2("2x2", "Matriz 2x2", "2x2 Grid", "4 cuadrantes simétricos", "4 symmetric quadrants", 4, "2 x 2"),
    GRID_2X3("2x3", "Matriz 2x3", "2x3 Grid", "6 indicadores compactos", "6 compact gauges", 6, "2 x 3"),
    GRID_2X4("2x4", "Matriz 2x4", "2x4 Grid", "8 indicadores", "8 gauges", 8, "2 x 4"),
    GRID_2X5("2x5", "Matriz 2x5", "2x5 Grid", "10 indicadores", "10 gauges", 10, "2 x 5"),
    GRID_2X6("2x6", "Matriz 2x6", "2x6 Grid", "12 indicadores en formato ancho", "12 gauges in wide format", 12, "2 x 6"),
    GRID_3X3("3x3", "Matriz 3x3", "3x3 Grid", "9 indicadores", "9 gauges", 9, "3 x 3"),
    GRID_3X4("3x4", "Matriz 3x4", "3x4 Grid", "12 indicadores", "12 gauges", 12, "3 x 4"),
    GRID_3X5("3x5", "Matriz 3x5", "3x5 Grid", "15 indicadores", "15 gauges", 15, "3 x 5"),
    GRID_3X6("3x6", "Matriz 3x6", "3x6 Grid", "18 indicadores", "18 gauges", 18, "3 x 6"),
    GRID_4X4("4x4", "Matriz 4x4", "4x4 Grid", "16 indicadores", "16 gauges", 16, "4 x 4"),
    GRID_4X5("4x5", "Matriz 4x5", "4x5 Grid", "20 indicadores", "20 gauges", 20, "4 x 5"),
    GRID_4X6("4x6", "Matriz 4x6", "4x6 Grid", "24 indicadores · Ideal en pantalla completa", "24 gauges · Ideal in full screen", 24, "4 x 6"),
    SPLIT_1_2("1+2", "1 Principal + 2 Laterales", "1 Main + 2 Side", "1 grande a la izquierda, 2 apilados a la derecha", "1 large on the left, 2 stacked on the right", 3, "1 + 2"),
    SPLIT_2_1("2+1", "2 Laterales + 1 Principal", "2 Side + 1 Main", "2 apilados a la izquierda, 1 grande a la derecha", "2 stacked on the left, 1 large on the right", 3, "2 + 1");

    val title: String get() = tr(esTitle, enTitle)
    val subtitle: String get() = tr(esSubtitle, enSubtitle)
    val preferredColumns: Int get() = if (id.contains('x')) id.substringAfter('x').toInt() else 2
    val isSplit: Boolean get() = id.contains('+')
    val preferredRows: Int get() = if (isSplit) 2 else slotCount / preferredColumns
    val carSupported: Boolean get() = preferredColumns <= 4 && preferredRows <= 2
    val carTitle: String
        get() {
            if (isSplit) return title
            val colLabel = if (preferredColumns == 1) tr("columna", "column") else tr("columnas", "columns")
            val rowLabel = if (preferredRows == 1) tr("fila", "row") else tr("filas", "rows")
            return "$preferredColumns $colLabel × $preferredRows $rowLabel"
        }
    val carBadge: String get() = if (isSplit) badge else "$preferredColumns × $preferredRows"
    fun forCar(): DashboardGridLayout = if (carSupported) this else carLayouts.first {
        !it.isSplit && it.preferredColumns == preferredColumns.coerceAtMost(4) && it.preferredRows == preferredRows.coerceAtMost(2)
    }
    companion object {
        val carLayouts: List<DashboardGridLayout> get() = listOf(GRID_1X1, GRID_1X2, GRID_1X3, GRID_1X4,
            GRID_2X1, GRID_2X2, GRID_2X3, GRID_2X4, SPLIT_1_2, SPLIT_2_1)
    }
}

enum class DashboardDensity {
    COMFORTABLE, COMPACT;

    val title: String
        get() = when (this) {
            COMFORTABLE -> tr("Normal", "Normal")
            COMPACT -> tr("Compacta", "Compact")
        }
}

data class DashboardWidget(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType = WidgetType.ANALOG_GAUGE,
    val pidShortName: String = "RPM",
    val title: String = tr("Revoluciones", "Engine RPM"),
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

    val localizedTitle: String
        get() = defaultTitlePairs
            .firstOrNull { (es, en) -> AppLanguage.entries.any { lang -> tr(es, en, lang) == title } }
            ?.let { (es, en) -> tr(es, en) }
            ?: title

    companion object {
        private val defaultTitlePairs = listOf(
            "Revoluciones" to "Engine RPM",
            "Velocidad" to "Speed",
            "Refrigerante" to "Coolant",
            "Voltaje ECU" to "ECU Voltage",
            "Presión DPF" to "DPF Pressure",
            "Temp. Escape EGT" to "Exhaust Temp EGT",
            "Temp. Refrigerante" to "Coolant Temp",
            "Carga Motor" to "Engine Load",
            "MAP absoluta" to "Absolute MAP",
            "Flujo Aire MAF" to "MAF Air Flow",
            "Temp. Admisión" to "Intake Temp",
            "Tacómetro Motor" to "Engine Tachometer",
            "Consumo Instantáneo" to "Instant Fuel Rate",
            "Nivel de Depósito" to "Fuel Tank Level",
            "Voltaje Batería" to "Battery Voltage",
            "Presión Dif. DPF B1" to "DPF Diff. Pressure B1",
            "Temp. EGT B1 S1" to "EGT Temp B1 S1",
            "Temp. Entrada DPF" to "DPF Inlet Temp",
            "Presión Rail" to "Rail Pressure",
            "Temp. Aceite Motor" to "Engine Oil Temp",
            "Temp. Ambiente" to "Ambient Temp",
            "Corrección Corta B1" to "Short Fuel Trim B1",
            "Corrección Larga B1" to "Long Fuel Trim B1",
            "Avance Encendido" to "Timing Advance",
            "Equivalencia Lambda" to "Commanded Equiv. Ratio"
        )
    }
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
    val localizedName: String
        get() = listOf(
            "Mi dash" to "My dash",
            "Mi Dashboard Personalizado" to "My Custom Dashboard",
            "Principal" to "Main"
        ).firstOrNull { (es, en) -> AppLanguage.entries.any { lang -> tr(es, en, lang) == name } }
            ?.let { (es, en) -> tr(es, en) }
            ?: name

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
            DashboardWidget(pidId = SensorIds.RPM, pidShortName = "RPM", title = tr("Revoluciones", "Engine RPM")),
            DashboardWidget(pidId = SensorIds.SPEED, pidShortName = "Velocidad", title = tr("Velocidad", "Speed"), colorHex = "#00E5FF"),
            DashboardWidget(pidId = SensorIds.COOLANT, pidShortName = "Temp Liq", title = tr("Refrigerante", "Coolant"), colorHex = "#00E676"),
            DashboardWidget(pidId = SensorIds.VOLTAGE, pidShortName = "Voltaje", title = tr("Voltaje ECU", "ECU Voltage"), colorHex = "#FFD600")
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
            title = tr("Conducción", "Driving"),
            subtitle = tr("RPM, velocidad, refrigerante y voltaje ECU", "RPM, speed, coolant and ECU voltage"),
            iconEmoji = "🏁",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = CustomDashboardConfig.getDefaultWidgets()
        ),
        DashboardPreset(
            id = "default",
            title = tr("Predeterminado Completo", "Full Default"),
            subtitle = tr("Cuatro lecturas básicas para conducir", "Four essential driving readings"),
            iconEmoji = "⚙️",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = CustomDashboardConfig.getDefaultWidgets()
        ),
        DashboardPreset(
            id = "diesel_dpf",
            title = tr("Diésel & Filtro DPF", "Diesel & DPF Filter"),
            subtitle = tr("Presión diferencial DPF, EGT, Refrigerante y Carga", "DPF differential pressure, EGT, Coolant and Load"),
            iconEmoji = "🛢️",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "DPF diff.pres", title = tr("Presión DPF", "DPF Pressure"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "EGT", title = tr("Temp. Escape EGT", "Exhaust Temp EGT"), colorHex = "#FF1744"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp Liq", title = tr("Temp. Refrigerante", "Coolant Temp"), colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Carga", title = tr("Carga Motor", "Engine Load"), colorHex = "#FF6D00")
            )
        ),
        DashboardPreset(
            id = "turbo_intake",
            title = tr("Turbo y Admisión", "Turbo & Intake"),
            subtitle = tr("Presión MAP, Flujo MAF, Temp Admisión y RPM", "MAP pressure, MAF flow, Intake Temp and RPM"),
            iconEmoji = "💨",
            gridLayout = DashboardGridLayout.GRID_1X4,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "MAP", title = tr("MAP absoluta", "Absolute MAP"), colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "MAF", title = tr("Flujo Aire MAF", "MAF Air Flow"), colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp Adm", title = tr("Temp. Admisión", "Intake Temp"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "RPM", title = tr("Tacómetro Motor", "Engine Tachometer"), colorHex = "#FF1744")
            )
        ),
        DashboardPreset(
            id = "eco_trip",
            title = tr("Viaje y Eficiencia", "Trip & Efficiency"),
            subtitle = tr("Velocímetro digital, Consumo L/h, Depósito y Batería", "Digital speedometer, Fuel rate L/h, Tank and Battery"),
            iconEmoji = "🚗",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = listOf(
                DashboardWidget(type = WidgetType.DIGITAL_HUD, pidShortName = "Velocidad", title = tr("Velocidad", "Speed"), colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Consumo", title = tr("Consumo Instantáneo", "Instant Fuel Rate"), colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.HORIZONTAL_BAR, pidShortName = "Nivel Dep", title = tr("Nivel de Depósito", "Fuel Tank Level"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.COMPACT_TILE, pidShortName = "Voltaje", title = tr("Voltaje Batería", "Battery Voltage"), colorHex = "#FFD600")
            )
        ),
        DashboardPreset(
            id = "diesel_sae",
            title = tr("Diésel & DPF (OBD Estándar)", "Diesel & DPF (Standard OBD)"),
            subtitle = tr("Presión dif. DPF, EGT B1 S1, Temp. DPF y Rail diésel", "DPF diff. pressure, EGT B1 S1, DPF Temp and Diesel Rail"),
            iconEmoji = "♻️",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "DPF B1 dif. pres", title = tr("Presión Dif. DPF B1", "DPF Diff. Pressure B1"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "EGT B1 S1", title = tr("Temp. EGT B1 S1", "EGT Temp B1 S1"), colorHex = "#FF1744"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp DPF B1 ent", title = tr("Temp. Entrada DPF", "DPF Inlet Temp"), colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.DIGITAL_HUD, pidShortName = "Rail diésel", title = tr("Presión Rail", "Rail Pressure"), colorHex = "#00E5FF")
            )
        ),
        DashboardPreset(
            id = "thermal_control",
            title = tr("Control Térmico", "Thermal Control"),
            subtitle = tr("Refrigerante, Aceite Motor, Admisión y Ambiente", "Coolant, Engine Oil, Intake and Ambient"),
            iconEmoji = "🌡️",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = listOf(
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp Liq", title = tr("Temp. Refrigerante", "Coolant Temp"), colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Temp aceite", title = tr("Temp. Aceite Motor", "Engine Oil Temp"), colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.COMPACT_TILE, pidShortName = "Temp Adm", title = tr("Temp. Admisión", "Intake Temp"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.COMPACT_TILE, pidShortName = "Temp ambiente", title = tr("Temp. Ambiente", "Ambient Temp"), colorHex = "#FFD600")
            )
        ),
        DashboardPreset(
            id = "fuel_injection",
            title = tr("Inyección y Mezcla", "Injection & Mixture"),
            subtitle = tr("Correcciones STFT/LTFT B1, Avance y Presión Rail", "STFT/LTFT B1 trims, Timing Advance and Lambda"),
            iconEmoji = "⛽",
            gridLayout = DashboardGridLayout.GRID_2X2,
            widgets = listOf(
                DashboardWidget(type = WidgetType.HORIZONTAL_BAR, pidShortName = "STFT B1", title = tr("Corrección Corta B1", "Short Fuel Trim B1"), colorHex = "#00E5FF"),
                DashboardWidget(type = WidgetType.HORIZONTAL_BAR, pidShortName = "LTFT B1", title = tr("Corrección Larga B1", "Long Fuel Trim B1"), colorHex = "#00E676"),
                DashboardWidget(type = WidgetType.ANALOG_GAUGE, pidShortName = "Avance encendido", title = tr("Avance Encendido", "Timing Advance"), colorHex = "#FF6D00"),
                DashboardWidget(type = WidgetType.COMPACT_TILE, pidShortName = "Equiv objetivo", title = tr("Equivalencia Lambda", "Commanded Equiv. Ratio"), colorHex = "#FFD600")
            )
        )
    )
    fun forPids(pids: List<ObdPid>): List<DashboardPreset> = getAllPresets().mapNotNull { preset ->
        val widgets = preset.widgets.mapNotNull { widget ->
            widget.resolvePid(pids)?.takeIf { !it.supportChecked || it.isSupported }
                ?.let { widget.copy(pidId = it.id) }
        }
        if (widgets.isEmpty()) null else preset.copy(widgets = widgets,
            subtitle = tr("${widgets.size} sensores · ", "${widgets.size} sensors · ") +
                if (widgets.all { widget -> pids.single { it.id == widget.pidId }.supportChecked }) {
                    tr("Compatibles con la ECU", "Compatible with ECU")
                } else {
                    tr("Compatibilidad por comprobar", "Compatibility unverified")
                })
    }

}


/** El orden y la selección usan identidades estables, independientes del nombre. */
data class CustomDashboardTabs(
    val dashboards: List<CustomDashboardConfig> = listOf(CustomDashboardConfig(name = tr("Mi dash", "My dash"))),
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
        val duplicated = original.duplicateAs("${original.name.take(72)} · ${tr("Copia", "Copy")}")
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
