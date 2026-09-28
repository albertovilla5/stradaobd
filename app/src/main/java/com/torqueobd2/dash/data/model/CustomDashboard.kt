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

data class DashboardWidget(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType = WidgetType.ANALOG_GAUGE,
    val pidShortName: String = "RPM",
    val title: String = "Revoluciones",
    val colorHex: String = "#FF6D00",
    val size: WidgetSize = WidgetSize.STANDARD
)

data class CustomDashboardConfig(
    val name: String = "Mi Dashboard Personalizado",
    val widgets: List<DashboardWidget> = getDefaultWidgets()
) {
    companion object {
        fun getDefaultWidgets(): List<DashboardWidget> = listOf(
            DashboardWidget(
                type = WidgetType.ANALOG_GAUGE,
                pidShortName = "RPM",
                title = "Tacómetro Motor",
                colorHex = "#FF6D00",
                size = WidgetSize.STANDARD
            ),
            DashboardWidget(
                type = WidgetType.ANALOG_GAUGE,
                pidShortName = "Velocidad",
                title = "Velocímetro",
                colorHex = "#00E5FF",
                size = WidgetSize.STANDARD
            ),
            DashboardWidget(
                type = WidgetType.ANALOG_GAUGE,
                pidShortName = "MAP",
                title = "Presión de Turbo",
                colorHex = "#FF6D00",
                size = WidgetSize.STANDARD
            ),
            DashboardWidget(
                type = WidgetType.ANALOG_GAUGE,
                pidShortName = "Temp Liq",
                title = "Temp. Refrigerante",
                colorHex = "#00E5FF",
                size = WidgetSize.STANDARD
            ),
            DashboardWidget(
                type = WidgetType.DIGITAL_HUD,
                pidShortName = "Velocidad",
                title = "Velocímetro Digital",
                colorHex = "#00E5FF",
                size = WidgetSize.FULL_WIDTH
            ),
            DashboardWidget(
                type = WidgetType.SPARKLINE_CHART,
                pidShortName = "RPM",
                title = "Historial RPM",
                colorHex = "#FF6D00",
                size = WidgetSize.FULL_WIDTH
            )
        )
    }
}
