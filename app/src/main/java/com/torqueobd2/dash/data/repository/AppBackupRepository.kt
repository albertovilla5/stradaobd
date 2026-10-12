package com.torqueobd2.dash.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.AppLanguagePreference
import com.torqueobd2.dash.i18n.tr
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Snapshot inmutable del estado restaurado o exportado por [AppBackupRepository].
 * Excluye deliberadamente `obd_connection.xml` (direcciones MAC Bluetooth, IP WiFi y estado de simulación)
 * y las grabaciones CSV de telemetría.
 */
data class AppBackupSnapshot(
    val vehicles: List<VehicleProfile>,
    val activeVehicle: VehicleProfile,
    val dashboardTabs: CustomDashboardTabs,
    val pids: List<ObdPid>,
    val alertRules: List<SensorAlertRule>,
    val carMode: Boolean,
    val languagePreference: AppLanguagePreference
)

/**
 * Gestiona la exportación e importación manual de copias de seguridad completas en formato JSON.
 * Complementa a Android Auto Backup permitiendo transferir vehículos, dashboards a medida,
 * catálogo de sensores/PIDs, alertas e interfaz hacia pantallas Android de coche (Head Units)
 * sin necesidad de cuenta de Google ni permisos de almacenamiento.
 */
class AppBackupRepository(
    private val vehicleProfileRepository: VehicleProfileRepository,
    private val sensorLibraryRepository: SensorLibraryRepository,
    private val sensorAlertRepository: SensorAlertRepository,
    private val interfacePreferences: InterfacePreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    constructor(
        vehiclePrefs: SharedPreferences,
        sensorPrefs: SharedPreferences,
        alertPrefs: SharedPreferences,
        interfacePrefs: SharedPreferences,
        nowMillis: () -> Long = System::currentTimeMillis
    ) : this(
        vehicleProfileRepository = VehicleProfileRepository(vehiclePrefs),
        sensorLibraryRepository = SensorLibraryRepository(sensorPrefs),
        sensorAlertRepository = SensorAlertRepository(alertPrefs),
        interfacePreferences = InterfacePreferences(interfacePrefs),
        nowMillis = nowMillis
    )

    constructor(context: Context) : this(
        vehicleProfileRepository = VehicleProfileRepository(context.applicationContext),
        sensorLibraryRepository = SensorLibraryRepository(
            context.applicationContext.getSharedPreferences("torque_sensors", Context.MODE_PRIVATE)
        ),
        sensorAlertRepository = SensorAlertRepository(
            context.applicationContext.getSharedPreferences("sensor_alerts", Context.MODE_PRIVATE)
        ),
        interfacePreferences = InterfacePreferences(context.applicationContext)
    )

    fun exportToJson(
        defaultPids: List<ObdPid> = defaultPidCatalog()
    ): String {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("appId", APP_ID)
        root.put("exportedAtUtc", utcFormat.format(Date(nowMillis())))

        // 1. Vehículos
        val vehicles = vehicleProfileRepository.getAllVehicles()
        val activeVehicleId = vehicleProfileRepository.getActiveVehicleId()
        val vehiclesJson = JSONObject().apply {
            put("activeVehicleId", activeVehicleId)
            val profilesArray = JSONArray()
            vehicles.forEach { profilesArray.put(it.toJson()) }
            put("profiles", profilesArray)
        }
        root.put("vehicles", vehiclesJson)

        // 2. Dashboards personalizados
        val tabs = vehicleProfileRepository.getCustomDashboardTabs()
        val dashboardsJson = JSONObject().apply {
            put("selectedId", tabs.selectedId)
            val tabsArray = JSONArray()
            tabs.dashboards.forEach { tab ->
                tabsArray.put(dashboardTabToJson(tab))
            }
            put("tabs", tabsArray)
        }
        root.put("dashboards", dashboardsJson)

        // 3. Biblioteca de sensores y PIDs personalizados
        val pids = sensorLibraryRepository.load(defaultPids)
        val sensorsArray = JSONArray()
        pids.forEach { pid ->
            sensorsArray.put(pidToJson(pid))
        }
        root.put("sensors", sensorsArray)

        // 4. Reglas de alertas de sensores
        val alerts = sensorAlertRepository.load()
        val alertsArray = JSONArray()
        alerts.forEach { rule ->
            alertsArray.put(alertRuleToJson(rule))
        }
        root.put("alerts", alertsArray)

        // 5. Preferencias de interfaz (sin datos de hardware/conexión)
        val interfaceJson = JSONObject().apply {
            put("carMode", interfacePreferences.carMode)
            put("language", interfacePreferences.languagePreference.code)
        }
        root.put("interface", interfaceJson)

        return root.toString(2)
    }

    fun exportToStream(
        output: OutputStream,
        defaultPids: List<ObdPid> = defaultPidCatalog()
    ) {
        val json = exportToJson(defaultPids)
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    fun importFromStream(
        input: InputStream,
        defaultPids: List<ObdPid> = defaultPidCatalog()
    ): AppBackupSnapshot {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return importFromJson(text, defaultPids)
    }

    fun importFromJson(
        jsonText: String,
        defaultPids: List<ObdPid> = defaultPidCatalog()
    ): AppBackupSnapshot {
        val root = try {
            JSONObject(jsonText.trim())
        } catch (_: Exception) {
            throw IllegalArgumentException(
                tr(
                    "El archivo no contiene un formato JSON válido.",
                    "The file does not contain valid JSON."
                )
            )
        }

        val appId = root.optString("appId", "")
        if (appId.isNotEmpty() && appId !in SUPPORTED_APP_IDS) {
            throw IllegalArgumentException(
                tr(
                    "El archivo no pertenece a una copia de seguridad de StradaOBD.",
                    "The file does not belong to a StradaOBD backup."
                )
            )
        }

        val version = root.optInt("schemaVersion", CURRENT_SCHEMA_VERSION)
        if (version !in 1..CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException(
                tr(
                    "Versión de copia de seguridad no compatible (v$version).",
                    "Unsupported backup schema version (v$version)."
                )
            )
        }

        val hasRecognizedSection = root.has("vehicles") || root.has("dashboards") ||
            root.has("sensors") || root.has("alerts") || root.has("interface")
        if (!hasRecognizedSection) {
            throw IllegalArgumentException(
                tr(
                    "No se encontraron datos de configuración de StradaOBD en el archivo.",
                    "No StradaOBD configuration data found in the file."
                )
            )
        }

        // 1. Parsear preferencias de interfaz primero para localizar nombres de sensores integrados al idioma restaurado
        val interfaceObj = root.optJSONObject("interface")
        val parsedCarMode = interfaceObj?.optBoolean("carMode", interfacePreferences.carMode)
            ?: interfacePreferences.carMode
        val parsedLanguage = interfaceObj?.optString("language", "")
            ?.takeIf { it.isNotBlank() }
            ?.let { AppLanguagePreference.fromCode(it) }
            ?: interfacePreferences.languagePreference
        val targetLanguage = parsedLanguage.resolve(com.torqueobd2.dash.i18n.AppI18n.getSystemLocale())

        // 2. Parsear sensores para poder vincular widgets de dashboard con sus PIDs
        val parsedPids = com.torqueobd2.dash.i18n.AppI18n.withLanguage(targetLanguage) {
            val localizedDefaults = defaultPids.ifEmpty { emptyList() }
            val builtInById = DefaultPids.getAllBuiltInPidsById()
            val localizedDefaultList = localizedDefaults.map { def ->
                val builtIn = builtInById[def.id]
                if (builtIn != null) {
                    def.copy(name = builtIn.name, shortName = builtIn.shortName, valueLabels = builtIn.valueLabels)
                } else def
            }
            val lookup = builtInById + localizedDefaultList.associateBy { it.id }
            if (root.has("sensors")) {
                val array = root.optJSONArray("sensors") ?: JSONArray()
                val list = (0 until array.length()).mapNotNull { i ->
                    array.optJSONObject(i)?.let { pidFromJson(it, lookup) }
                }.filterNot { it.cleanObdCommand().startsWith("CALC_") }.distinctBy { it.id }
                (list + localizedDefaultList.filter { def -> list.none { it.id == def.id } })
            } else {
                sensorLibraryRepository.load(localizedDefaultList)
            }
        }

        // 3. Parsear vehículos
        val vehiclesObj = root.optJSONObject("vehicles")
        val parsedVehicles: List<VehicleProfile>
        val parsedActiveVehicleId: String?
        if (vehiclesObj != null) {
            val profilesArray = vehiclesObj.optJSONArray("profiles") ?: JSONArray()
            parsedVehicles = (0 until profilesArray.length()).mapNotNull { i ->
                profilesArray.optJSONObject(i)?.let {
                    runCatching { VehicleProfile.fromJson(it) }.getOrNull()
                }
            }.distinctBy { it.id }
            parsedActiveVehicleId = vehiclesObj.optString("activeVehicleId", "").takeIf { it.isNotBlank() }
        } else {
            parsedVehicles = vehicleProfileRepository.getAllVehicles()
            parsedActiveVehicleId = vehicleProfileRepository.getActiveVehicleId()
        }

        // 4. Parsear dashboards
        val dashboardsObj = root.optJSONObject("dashboards")
        val parsedTabs: CustomDashboardTabs = if (dashboardsObj != null) {
            val tabsArray = dashboardsObj.optJSONArray("tabs") ?: JSONArray()
            val configs = (0 until tabsArray.length()).mapNotNull { i ->
                tabsArray.optJSONObject(i)?.let(::dashboardTabFromJson)
            }.distinctBy { it.id }
            if (configs.isNotEmpty()) {
                val selectedId = dashboardsObj.optString("selectedId", "")
                    .takeIf { id -> configs.any { it.id == id } }
                    ?: configs.first().id
                CustomDashboardTabs(configs, selectedId).bindPids(parsedPids)
            } else {
                vehicleProfileRepository.getCustomDashboardTabs().bindPids(parsedPids)
            }
        } else {
            vehicleProfileRepository.getCustomDashboardTabs().bindPids(parsedPids)
        }

        // 5. Parsear reglas de alertas
        val alertsArray = root.optJSONArray("alerts")
        val parsedAlerts: List<SensorAlertRule> = if (alertsArray != null) {
            (0 until alertsArray.length()).mapNotNull { i ->
                alertsArray.optJSONObject(i)?.let(::alertRuleFromJson)
            }.filter { it.validationError() == null }.distinctBy { it.id }.take(1000)
        } else {
            sensorAlertRepository.load()
        }

        // Persistir de forma atómica una vez validados todos los bloques
        sensorLibraryRepository.save(parsedPids)
        val activeVehicle = if (parsedVehicles.isNotEmpty()) {
            vehicleProfileRepository.replaceAllVehicles(parsedVehicles, parsedActiveVehicleId)
        } else {
            vehicleProfileRepository.getActiveVehicle()
        }
        vehicleProfileRepository.saveCustomDashboardTabs(parsedTabs)
        sensorAlertRepository.save(parsedAlerts)
        interfacePreferences.carMode = parsedCarMode
        interfacePreferences.languagePreference = parsedLanguage

        return AppBackupSnapshot(
            vehicles = vehicleProfileRepository.getAllVehicles(),
            activeVehicle = activeVehicle,
            dashboardTabs = parsedTabs,
            pids = parsedPids,
            alertRules = parsedAlerts,
            carMode = parsedCarMode,
            languagePreference = parsedLanguage
        )
    }

    private fun dashboardTabToJson(tab: CustomDashboardConfig): JSONObject = JSONObject().apply {
        put("id", tab.id)
        put("name", tab.name)
        put("gridLayout", tab.gridLayout.id)
        if (tab.carGridLayout != null) {
            put("carGridLayout", tab.carGridLayout.id)
        }
        put("density", tab.density.name)
        val slotsArray = JSONArray()
        tab.resolvedSlots().toSortedMap().forEach { (slotIndex, widget) ->
            slotsArray.put(JSONObject().apply {
                put("slot", slotIndex)
                put("id", widget.id)
                put("pidId", widget.pidId)
                put("pidShortName", widget.pidShortName)
                put("title", widget.title)
                put("type", widget.type.name)
                put("colorHex", widget.colorHex)
                put("size", widget.size.name)
                if (widget.decimals != null) put("decimals", widget.decimals)
                if (widget.scaleMin != null && widget.scaleMin.isFinite()) put("scaleMin", widget.scaleMin.toDouble())
                if (widget.scaleMax != null && widget.scaleMax.isFinite()) put("scaleMax", widget.scaleMax.toDouble())
            })
        }
        put("slots", slotsArray)
    }

    private fun dashboardTabFromJson(obj: JSONObject): CustomDashboardConfig? {
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: return null
        val defaultTabName = tr("Mi dash", "My dash")
        val name = obj.optString("name", defaultTabName).ifBlank { defaultTabName }
        val gridId = obj.optString("gridLayout", DashboardGridLayout.GRID_1X4.id)
        val gridLayout = DashboardGridLayout.entries.find { it.id == gridId } ?: DashboardGridLayout.GRID_1X4
        val carGridId = obj.optString("carGridLayout", "").takeIf { it.isNotBlank() }
        val carGridLayout = carGridId?.let { cid ->
            DashboardGridLayout.entries.find { it.id == cid && it.carSupported }
        }
        val densityName = obj.optString("density", DashboardDensity.COMFORTABLE.name)
        val density = DashboardDensity.entries.find { it.name == densityName } ?: DashboardDensity.COMFORTABLE

        val slots = mutableMapOf<Int, DashboardWidget>()
        val slotsArray = obj.optJSONArray("slots") ?: JSONArray()
        for (i in 0 until slotsArray.length()) {
            val slotObj = slotsArray.optJSONObject(i) ?: continue
            val slotIndex = slotObj.optInt("slot", i)
            if (slotIndex !in 0..63) continue
            val widgetId = slotObj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
            val typeName = slotObj.optString("type", WidgetType.ANALOG_GAUGE.name)
            val sizeName = slotObj.optString("size", WidgetSize.STANDARD.name)
            val decimals = if (slotObj.has("decimals") && !slotObj.isNull("decimals")) {
                slotObj.optInt("decimals", -1).takeIf { it in 0..4 }
            } else null
            val scaleMin = if (slotObj.has("scaleMin") && !slotObj.isNull("scaleMin")) {
                slotObj.optDouble("scaleMin", Double.NaN).toFloat().takeIf { it.isFinite() }
            } else null
            val scaleMax = if (slotObj.has("scaleMax") && !slotObj.isNull("scaleMax")) {
                slotObj.optDouble("scaleMax", Double.NaN).toFloat().takeIf { it.isFinite() }
            } else null

            slots[slotIndex] = DashboardWidget(
                id = widgetId,
                pidId = slotObj.optString("pidId", ""),
                pidShortName = slotObj.optString("pidShortName", "RPM"),
                title = slotObj.optString("title", "Sensor"),
                type = WidgetType.entries.find { it.name == typeName } ?: WidgetType.ANALOG_GAUGE,
                colorHex = slotObj.optString("colorHex", "#FF6D00"),
                size = WidgetSize.entries.find { it.name == sizeName } ?: WidgetSize.STANDARD,
                decimals = decimals,
                scaleMin = scaleMin,
                scaleMax = scaleMax
            )
        }

        return CustomDashboardConfig(
            id = id,
            name = name,
            gridLayout = gridLayout,
            carGridLayout = carGridLayout,
            density = density,
            slots = slots,
            widgets = emptyList()
        )
    }

    private fun pidToJson(pid: ObdPid): JSONObject = JSONObject().apply {
        put("id", pid.id)
        put("name", pid.name)
        put("shortName", pid.shortName)
        put("modeAndPid", pid.modeAndPid)
        put("equation", pid.equation)
        put("minValue", pid.minValue.toDouble())
        put("maxValue", pid.maxValue.toDouble())
        put("units", pid.units)
        put("header", pid.header)
        put("isCustom", pid.isCustom)
        put("isEnabled", pid.isEnabled)
        put("category", pid.category.name)
        put("availabilityEquation", pid.availabilityEquation)

        val unavailableObj = JSONObject()
        pid.unavailableRawValues.forEach { (varName, values) ->
            val arr = JSONArray()
            values.sorted().forEach { arr.put(it) }
            unavailableObj.put(varName, arr)
        }
        put("unavailableRawValues", unavailableObj)

        val labelsObj = JSONObject()
        pid.valueLabels.forEach { (k, v) ->
            labelsObj.put(k.toString(), v)
        }
        put("valueLabels", labelsObj)
    }

    private fun pidFromJson(obj: JSONObject, builtInById: Map<String, ObdPid>): ObdPid? {
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: return null
        val command = obj.optString("modeAndPid", "").takeIf { it.isNotBlank() } ?: return null
        val builtIn = builtInById[id]

        val unavailable = mutableMapOf<String, Set<Int>>()
        obj.optJSONObject("unavailableRawValues")?.let { unavObj ->
            val keys = unavObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = unavObj.optJSONArray(key) ?: continue
                val set = (0 until arr.length()).mapNotNull { idx ->
                    if (arr.isNull(idx)) null else arr.optInt(idx)
                }.toSet()
                unavailable[key] = set
            }
        }

        val labels = mutableMapOf<Int, String>()
        obj.optJSONObject("valueLabels")?.let { labelsObj ->
            val keys = labelsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val intKey = key.toIntOrNull() ?: continue
                labels[intKey] = labelsObj.optString(key, "")
            }
        }

        val categoryName = obj.optString("category", PidCategory.CUSTOM.name)
        val category = PidCategory.entries.find { it.name == categoryName } ?: PidCategory.CUSTOM

        return ObdPid(
            id = id,
            name = builtIn?.name ?: obj.optString("name", "Sensor").ifBlank { "Sensor" },
            shortName = builtIn?.shortName ?: obj.optString("shortName", "Sensor").ifBlank { "Sensor" },
            modeAndPid = command,
            equation = obj.optString("equation", ""),
            minValue = obj.optDouble("minValue", 0.0).toFloat(),
            maxValue = obj.optDouble("maxValue", 100.0).toFloat(),
            units = obj.optString("units", ""),
            header = obj.optString("header", ""),
            isCustom = obj.optBoolean("isCustom", false),
            isEnabled = obj.optBoolean("isEnabled", true),
            isSupported = true,
            supportChecked = false,
            category = category,
            availabilityEquation = obj.optString("availabilityEquation", ""),
            unavailableRawValues = unavailable,
            valueLabels = builtIn?.valueLabels ?: labels
        )
    }

    private fun alertRuleToJson(rule: SensorAlertRule): JSONObject = JSONObject().apply {
        put("id", rule.id)
        put("vehicleId", rule.vehicleId)
        put("pidId", rule.pidId)
        put("sensorName", rule.sensorName)
        put("units", rule.units)
        if (rule.lowerLimit != null && rule.lowerLimit.isFinite()) put("lowerLimit", rule.lowerLimit)
        if (rule.upperLimit != null && rule.upperLimit.isFinite()) put("upperLimit", rule.upperLimit)
        put("durationSeconds", rule.durationSeconds)
        put("enabled", rule.enabled)
    }

    private fun alertRuleFromJson(obj: JSONObject): SensorAlertRule? {
        val lower = if (obj.has("lowerLimit") && !obj.isNull("lowerLimit")) {
            obj.optDouble("lowerLimit", Double.NaN).takeIf { it.isFinite() }
        } else null
        val upper = if (obj.has("upperLimit") && !obj.isNull("upperLimit")) {
            obj.optDouble("upperLimit", Double.NaN).takeIf { it.isFinite() }
        } else null

        val rule = SensorAlertRule(
            id = obj.optString("id", ""),
            vehicleId = obj.optString("vehicleId", ""),
            pidId = obj.optString("pidId", ""),
            sensorName = obj.optString("sensorName", "Sensor"),
            units = obj.optString("units", ""),
            lowerLimit = lower,
            upperLimit = upper,
            durationSeconds = obj.optInt("durationSeconds", 3),
            enabled = obj.optBoolean("enabled", true)
        )
        return rule.takeIf { it.validationError() == null }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_ID = "com.stradaobd.dash"
        private val SUPPORTED_APP_IDS = setOf("com.stradaobd.dash", "com.torqueobd2.dash")

        fun defaultPidCatalog(): List<ObdPid> =
            DefaultPids.getStandardPids() + DefaultPids.getHondaIdtecPids().map { it.copy(isEnabled = false) }
    }
}
