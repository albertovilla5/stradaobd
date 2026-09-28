package com.torqueobd2.dash.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.FuelType
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.WidgetSize
import com.torqueobd2.dash.data.model.WidgetType

/**
 * Repositorio para la persistencia del perfil del vehículo y la configuración
 * del dashboard personalizado del usuario.
 */
class VehicleProfileRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("torque_obd_prefs", Context.MODE_PRIVATE)

    fun getVehicleProfile(): VehicleProfile {
        val name = prefs.getString("veh_name", "Honda Civic 2.2 i-DTEC") ?: "Honda Civic 2.2 i-DTEC"
        val year = prefs.getString("veh_year", "2012") ?: "2012"
        val disp = prefs.getFloat("veh_disp", 2.2f)
        val tank = prefs.getFloat("veh_tank", 50.0f)
        val weight = prefs.getFloat("veh_weight", 1450f)
        val fuelTypeName = prefs.getString("veh_fuel", FuelType.DIESEL.name) ?: FuelType.DIESEL.name
        val fuelType = try { FuelType.valueOf(fuelTypeName) } catch (_: Exception) { FuelType.DIESEL }
        val maxRpm = prefs.getFloat("veh_max_rpm", 5000f)
        val ve = prefs.getFloat("veh_ve", 85f)
        val hideUnsupported = prefs.getBoolean("veh_hide_unsupported", true)

        return VehicleProfile(
            name = name,
            modelYear = year,
            engineDisplacementLiters = disp,
            fuelTankCapacityLiters = tank,
            vehicleWeightKg = weight,
            fuelType = fuelType,
            maxRpm = maxRpm,
            volumetricEfficiencyPercent = ve,
            hideUnsupportedSensors = hideUnsupported
        )
    }

    fun saveVehicleProfile(profile: VehicleProfile) {
        prefs.edit()
            .putString("veh_name", profile.name)
            .putString("veh_year", profile.modelYear)
            .putFloat("veh_disp", profile.engineDisplacementLiters)
            .putFloat("veh_tank", profile.fuelTankCapacityLiters)
            .putFloat("veh_weight", profile.vehicleWeightKg)
            .putString("veh_fuel", profile.fuelType.name)
            .putFloat("veh_max_rpm", profile.maxRpm)
            .putFloat("veh_ve", profile.volumetricEfficiencyPercent)
            .putBoolean("veh_hide_unsupported", profile.hideUnsupportedSensors)
            .apply()
    }

    fun getCustomDashboard(): CustomDashboardConfig {
        val count = prefs.getInt("custom_widget_count", -1)
        if (count == -1) {
            // Primera vez: retornar widgets predeterminados
            return CustomDashboardConfig()
        }

        val widgets = mutableListOf<DashboardWidget>()
        for (i in 0 until count) {
            val id = prefs.getString("widget_${i}_id", "") ?: ""
            val typeStr = prefs.getString("widget_${i}_type", WidgetType.ANALOG_GAUGE.name) ?: WidgetType.ANALOG_GAUGE.name
            val pid = prefs.getString("widget_${i}_pid", "RPM") ?: "RPM"
            val title = prefs.getString("widget_${i}_title", "Sensor") ?: "Sensor"
            val color = prefs.getString("widget_${i}_color", "#FF6D00") ?: "#FF6D00"
            val sizeStr = prefs.getString("widget_${i}_size", WidgetSize.STANDARD.name) ?: WidgetSize.STANDARD.name

            val type = try { WidgetType.valueOf(typeStr) } catch (_: Exception) { WidgetType.ANALOG_GAUGE }
            val size = try { WidgetSize.valueOf(sizeStr) } catch (_: Exception) { WidgetSize.STANDARD }

            if (id.isNotBlank()) {
                widgets.add(
                    DashboardWidget(
                        id = id,
                        type = type,
                        pidShortName = pid,
                        title = title,
                        colorHex = color,
                        size = size
                    )
                )
            }
        }
        return CustomDashboardConfig(widgets = widgets)
    }

    fun saveCustomDashboard(config: CustomDashboardConfig) {
        val editor = prefs.edit()
        editor.putInt("custom_widget_count", config.widgets.size)
        config.widgets.forEachIndexed { i, w ->
            editor.putString("widget_${i}_id", w.id)
            editor.putString("widget_${i}_type", w.type.name)
            editor.putString("widget_${i}_pid", w.pidShortName)
            editor.putString("widget_${i}_title", w.title)
            editor.putString("widget_${i}_color", w.colorHex)
            editor.putString("widget_${i}_size", w.size.name)
        }
        editor.apply()
    }
}
