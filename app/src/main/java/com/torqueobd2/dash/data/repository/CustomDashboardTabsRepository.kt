package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.tr

/** Una escritura conjunta conserva pestañas, selección, rejillas y ranuras entre sesiones. */
class CustomDashboardTabsRepository(private val prefs: SharedPreferences) {
    fun load(legacyDashboard: () -> CustomDashboardConfig): CustomDashboardTabs {
        val defaultTabName = tr("Mi dash", "My dash")
        val count = prefs.getInt("dash_tabs_count", 0)
        val dashboards = (0 until count).mapNotNull { index ->
            val key = "dash_tab_${index}_"
            val id = prefs.getString(key + "id", null)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = prefs.getString(key + "name", defaultTabName) ?: defaultTabName
            val layout = DashboardGridLayout.entries.find { it.id == prefs.getString(key + "grid", null) }
                ?: DashboardGridLayout.GRID_1X4
            val slots = mutableMapOf<Int, DashboardWidget>()
            for (slot in 0 until prefs.getInt(key + "slot_count", 0)) {
                val widgetKey = key + "slot_${slot}_"
                val widgetId = prefs.getString(widgetKey + "id", null) ?: continue
                slots[prefs.getInt(widgetKey + "index", slot)] = DashboardWidget(
                    id = widgetId,
                    pidId = prefs.getString(widgetKey + "pid_id", "") ?: "",
                    type = WidgetType.entries.find { it.name == prefs.getString(widgetKey + "type", null) } ?: WidgetType.ANALOG_GAUGE,
                    pidShortName = prefs.getString(widgetKey + "pid", "RPM") ?: "RPM",
                    title = prefs.getString(widgetKey + "title", "Sensor") ?: "Sensor",
                    colorHex = prefs.getString(widgetKey + "color", "#FF6D00") ?: "#FF6D00",
                    size = WidgetSize.entries.find { it.name == prefs.getString(widgetKey + "size", null) } ?: WidgetSize.STANDARD,
                    decimals = prefs.getInt(widgetKey + "decimals", -1).takeIf { it in 0..4 },
                    scaleMin = prefs.getString(widgetKey + "scale_min", null)?.toFloatOrNull()?.takeIf { it.isFinite() },
                    scaleMax = prefs.getString(widgetKey + "scale_max", null)?.toFloatOrNull()?.takeIf { it.isFinite() }
                )
            }
            CustomDashboardConfig(id = id, name = name, gridLayout = layout, slots = slots, widgets = emptyList(),
                carGridLayout = DashboardGridLayout.entries.find { it.id == prefs.getString(key + "car_grid", null) && it.carSupported },
                density = DashboardDensity.entries.find { it.name == prefs.getString(key + "density", null) } ?: DashboardDensity.COMFORTABLE)
        }.distinctBy { it.id }
        if (dashboards.isEmpty()) {
            val legacy = legacyDashboard()
            val migrated = legacy.copy(
                name = if (legacy.name == "Mi Dashboard Personalizado" || legacy.name == "My Custom Dashboard") defaultTabName else legacy.name,
                slots = legacy.resolvedSlots(), widgets = emptyList()
            )
            return CustomDashboardTabs(listOf(migrated)).also(::save)
        }
        val selectedId = prefs.getString("dash_tabs_selected", null)
        return CustomDashboardTabs(dashboards, selectedId?.takeIf { id -> dashboards.any { it.id == id } } ?: dashboards.first().id)
    }

    fun save(tabs: CustomDashboardTabs) {
        val editor = prefs.edit().putInt("dash_tabs_count", tabs.dashboards.size).putString("dash_tabs_selected", tabs.selectedId)
        tabs.dashboards.forEachIndexed { index, config ->
            val key = "dash_tab_${index}_"
            val slots = config.resolvedSlots().toSortedMap()
            editor.putString(key + "id", config.id).putString(key + "name", config.name)
                .putString(key + "car_grid", config.carGridLayout?.id)
                .putString(key + "density", config.density.name).putString(key + "grid", config.gridLayout.id).putInt(key + "slot_count", slots.size)
            slots.entries.forEachIndexed { position, (slot, widget) ->
                val widgetKey = key + "slot_${position}_"
                editor.putInt(widgetKey + "index", slot).putString(widgetKey + "id", widget.id)
                    .putString(widgetKey + "pid_id", widget.pidId)
                    .putString(widgetKey + "type", widget.type.name).putString(widgetKey + "pid", widget.pidShortName)
                    .putString(widgetKey + "title", widget.title).putString(widgetKey + "color", widget.colorHex)
                    .putString(widgetKey + "size", widget.size.name)
                    .putInt(widgetKey + "decimals", widget.decimals ?: -1)
                    .putString(widgetKey + "scale_min", widget.scaleMin?.toString())
                    .putString(widgetKey + "scale_max", widget.scaleMax?.toString())
            }
        }
        editor.apply()
    }
}
