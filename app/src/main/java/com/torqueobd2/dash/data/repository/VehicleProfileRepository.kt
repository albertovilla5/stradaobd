package com.torqueobd2.dash.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.CustomDashboardTabs
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardGridLayout
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.FuelType
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.WidgetSize
import com.torqueobd2.dash.data.model.WidgetType
import com.torqueobd2.dash.i18n.tr
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositorio para la persistencia del listado de vehículos guardados (CRUD),
 * el vehículo activo seleccionado y la configuración del dashboard personalizado.
 */
class VehicleProfileRepository(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("torque_obd_prefs", Context.MODE_PRIVATE))

    @Synchronized
    fun getAllVehicles(): List<VehicleProfile> {
        val jsonStr = prefs.getString("saved_vehicles_json", null)
        if (prefs.contains("saved_vehicles_json")) {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                (0 until array.length()).mapNotNull { index ->
                    runCatching { VehicleProfile.fromJson(array.getJSONObject(index)) }.getOrNull()
                }.distinctBy { it.id }
            } catch (_: Exception) { emptyList() }
        }
        // Solo migrar una ficha antigua real; una instalación nueva no inventa un coche.
        if (!prefs.contains("veh_name") || prefs.getString("veh_name", "").isNullOrBlank()) return emptyList()
        val initialVehicle = getLegacyVehicleProfileOrDefault()
        return listOf(initialVehicle).also {
            saveAllVehicles(it)
            prefs.edit().putString("active_vehicle_id", initialVehicle.id).apply()
        }
    }

    private fun getLegacyVehicleProfileOrDefault(): VehicleProfile {
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
            maxPowerHp = if (disp >= 2.0f) 150f else 115f,
            maxTorqueNm = if (disp >= 2.0f) 350f else 250f,
            volumetricEfficiencyPercent = ve,
            hideUnsupportedSensors = hideUnsupported
        )
    }

    private fun saveAllVehicles(vehicles: List<VehicleProfile>) {
        val array = JSONArray()
        vehicles.forEach { array.put(it.toJson()) }
        prefs.edit().putString("saved_vehicles_json", array.toString()).apply()
    }

    @Synchronized
    fun getActiveVehicleId(): String {
        val currentId = prefs.getString("active_vehicle_id", null)
        val vehicles = getAllVehicles()
        if (currentId != null && vehicles.any { it.id == currentId }) {
            return currentId
        }
        val fallbackId = vehicles.firstOrNull()?.id ?: ""
        if (fallbackId.isNotEmpty()) {
            prefs.edit().putString("active_vehicle_id", fallbackId).apply()
        }
        return fallbackId
    }

    @Synchronized
    fun getActiveVehicle(): VehicleProfile {
        val activeId = getActiveVehicleId()
        val vehicles = getAllVehicles()
        return vehicles.find { it.id == activeId } ?: vehicles.firstOrNull()
            ?: VehicleProfile(name = tr("Sin vehículo", "No vehicle"), customInitCommands = "")
    }

    @Synchronized
    fun saveVehicle(profile: VehicleProfile): VehicleProfile {
        val vehicles = getAllVehicles().toMutableList()
        val index = vehicles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            vehicles[index] = profile
        } else {
            vehicles.add(profile)
        }
        saveAllVehicles(vehicles)

        // Si es el único vehículo o es el activo actualmente, actualizar activo y campos legacy
        val activeId = getActiveVehicleId()
        if (activeId == profile.id || vehicles.size == 1) {
            setActiveVehicle(profile.id)
        }

        return profile
    }

    @Synchronized
    fun setActiveVehicle(profileId: String): VehicleProfile {
        val vehicles = getAllVehicles()
        val target = vehicles.find { it.id == profileId } ?: vehicles.firstOrNull()
            ?: VehicleProfile(name = tr("Sin vehículo", "No vehicle"), customInitCommands = "")
        prefs.edit()
            .putString("active_vehicle_id", target.id)
            .putString("veh_name", target.name)
            .putString("veh_year", target.modelYear)
            .putFloat("veh_disp", target.engineDisplacementLiters)
            .putFloat("veh_tank", target.fuelTankCapacityLiters)
            .putFloat("veh_weight", target.vehicleWeightKg)
            .putString("veh_fuel", target.fuelType.name)
            .putFloat("veh_max_rpm", target.maxRpm)
            .putFloat("veh_ve", target.volumetricEfficiencyPercent)
            .putBoolean("veh_hide_unsupported", target.hideUnsupportedSensors)
            .apply()
        return target
    }

    @Synchronized
    fun replaceAllVehicles(vehicles: List<VehicleProfile>, activeVehicleId: String? = null): VehicleProfile {
        val distinct = vehicles.distinctBy { it.id }
        saveAllVehicles(distinct)
        val targetId = activeVehicleId?.takeIf { id -> distinct.any { it.id == id } }
            ?: distinct.firstOrNull()?.id
            ?: ""
        return setActiveVehicle(targetId)
    }

    @Synchronized
    fun deleteVehicle(profileId: String): Boolean {
        val vehicles = getAllVehicles().toMutableList()
        if (vehicles.size <= 1) {
            // No permitir borrar el único vehículo existente
            return false
        }
        val removed = vehicles.removeAll { it.id == profileId }
        if (removed) {
            saveAllVehicles(vehicles)
            val activeId = prefs.getString("active_vehicle_id", null)
            if (activeId == profileId) {
                // Si borramos el activo, activar automáticamente el primer vehículo disponible
                val nextActive = vehicles.first()
                setActiveVehicle(nextActive.id)
            }
            return true
        }
        return false
    }

    // Compatibilidad retroactiva
    fun getVehicleProfile(): VehicleProfile = getActiveVehicle()

    fun saveVehicleProfile(profile: VehicleProfile) {
        saveVehicle(profile)
    }

    private val dashboardTabsRepository = CustomDashboardTabsRepository(prefs)

    fun getCustomDashboardTabs(): CustomDashboardTabs = dashboardTabsRepository.load(::getCustomDashboard)

    fun saveCustomDashboardTabs(tabs: CustomDashboardTabs) = dashboardTabsRepository.save(tabs)

    private fun getCustomDashboard(): CustomDashboardConfig {
        val gridLayoutId = prefs.getString("custom_grid_layout", DashboardGridLayout.GRID_1X4.id) ?: DashboardGridLayout.GRID_1X4.id
        val gridLayout = DashboardGridLayout.entries.find { it.id == gridLayoutId } ?: DashboardGridLayout.GRID_1X4

        val count = prefs.getInt("custom_widget_count", -1)
        if (count == -1) {
            return CustomDashboardConfig(gridLayout = gridLayout)
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
                        pidShortName = pid, pidId = prefs.getString("widget_${i}_pid_id", "") ?: "",
                        title = title,
                        colorHex = color,
                        size = size
                    )
                )
            }
        }

        val slots = mutableMapOf<Int, DashboardWidget>()
        for (i in 0 until 12) {
            val hasSlot = prefs.getBoolean("slot_${i}_present", false)
            if (hasSlot) {
                val id = prefs.getString("slot_${i}_id", "") ?: ""
                val typeStr = prefs.getString("slot_${i}_type", WidgetType.ANALOG_GAUGE.name) ?: WidgetType.ANALOG_GAUGE.name
                val pid = prefs.getString("slot_${i}_pid", "RPM") ?: "RPM"
                val title = prefs.getString("slot_${i}_title", "Sensor") ?: "Sensor"
                val color = prefs.getString("slot_${i}_color", "#FF6D00") ?: "#FF6D00"
                val sizeStr = prefs.getString("slot_${i}_size", WidgetSize.STANDARD.name) ?: WidgetSize.STANDARD.name
                val type = try { WidgetType.valueOf(typeStr) } catch (_: Exception) { WidgetType.ANALOG_GAUGE }
                val size = try { WidgetSize.valueOf(sizeStr) } catch (_: Exception) { WidgetSize.STANDARD }
                if (id.isNotBlank()) {
                    slots[i] = DashboardWidget(id = id, type = type, pidShortName = pid, pidId = prefs.getString("slot_${i}_pid_id", "") ?: "", title = title, colorHex = color, size = size)
                }
            }
        }

        return CustomDashboardConfig(
            gridLayout = gridLayout,
            slots = slots,
            widgets = widgets
        )
    }

    fun saveCustomDashboard(config: CustomDashboardConfig) {
        val editor = prefs.edit()
        editor.putString("custom_grid_layout", config.gridLayout.id)
        editor.putInt("custom_widget_count", config.widgets.size)
        config.widgets.forEachIndexed { i, w ->
            editor.putString("widget_${i}_id", w.id)
            editor.putString("widget_${i}_type", w.type.name)
            editor.putString("widget_${i}_pid", w.pidShortName)
            editor.putString("widget_${i}_pid_id", w.pidId)
            editor.putString("widget_${i}_title", w.title)
            editor.putString("widget_${i}_color", w.colorHex)
            editor.putString("widget_${i}_size", w.size.name)
        }
        for (i in 0 until 12) {
            val w = config.slots[i]
            if (w != null) {
                editor.putBoolean("slot_${i}_present", true)
                editor.putString("slot_${i}_id", w.id)
                editor.putString("slot_${i}_type", w.type.name)
                editor.putString("slot_${i}_pid", w.pidShortName)
                editor.putString("slot_${i}_pid_id", w.pidId)
                editor.putString("slot_${i}_title", w.title)
                editor.putString("slot_${i}_color", w.colorHex)
                editor.putString("slot_${i}_size", w.size.name)
            } else {
                editor.putBoolean("slot_${i}_present", false)
            }
        }
        editor.apply()
    }
}
