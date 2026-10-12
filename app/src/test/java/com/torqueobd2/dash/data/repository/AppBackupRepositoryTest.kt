package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.AppLanguagePreference
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Proxy

class AppBackupRepositoryTest {

    private fun memoryPrefs(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> {
                    values[args!![0] as String] = args[1]
                    proxy
                }
                method.name == "remove" -> {
                    values.remove(args!![0] as String)
                    proxy
                }
                method.name == "clear" -> {
                    values.clear()
                    proxy
                }
                method.name == "apply" -> null
                method.name == "commit" -> true
                else -> error("Unexpected Editor method: ${method.name}")
            }
        }
        return Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString", "getInt", "getFloat", "getBoolean", "getLong" -> values[args!![0]] ?: args[1]
                "contains" -> values.containsKey(args!![0] as String)
                "getAll" -> values.toMap()
                "edit" -> editor
                else -> error("Unexpected SharedPreferences method: ${method.name}")
            }
        } as SharedPreferences
    }

    @Test
    fun `round-trip export and import preserves vehicles, dashboards, custom PIDs, alerts and interface settings`() {
        val srcVehiclePrefs = memoryPrefs()
        val srcSensorPrefs = memoryPrefs()
        val srcAlertPrefs = memoryPrefs()
        val srcInterfacePrefs = memoryPrefs()

        val vehicleRepo = VehicleProfileRepository(srcVehiclePrefs)
        val sensorRepo = SensorLibraryRepository(srcSensorPrefs)
        val alertRepo = SensorAlertRepository(srcAlertPrefs)
        val interfacePrefs = InterfacePreferences(srcInterfacePrefs)

        // 1. Setup multiple vehicles
        val civic = VehicleProfile(
            id = "veh-civic",
            name = "Honda Civic 2.2 i-DTEC",
            modelYear = "2013",
            engineDisplacementLiters = 2.2f,
            fuelTankCapacityLiters = 50f,
            vehicleWeightKg = 1460f,
            fuelType = FuelType.DIESEL,
            maxRpm = 5200f,
            maxPowerHp = 150f,
            maxTorqueNm = 350f,
            volumetricEfficiencyPercent = 87f,
            customInitCommands = "AT SP7\nAT SH DA10F1",
            hideUnsupportedSensors = true,
            boostPressureAdjustmentBar = 0.05f
        )
        val miata = VehicleProfile(
            id = "veh-miata",
            name = "Mazda MX-5 ND",
            modelYear = "2019",
            engineDisplacementLiters = 2.0f,
            fuelTankCapacityLiters = 45f,
            vehicleWeightKg = 1060f,
            fuelType = FuelType.GASOLINE,
            maxRpm = 7500f,
            maxPowerHp = 184f,
            maxTorqueNm = 205f,
            volumetricEfficiencyPercent = 92f,
            customInitCommands = "",
            hideUnsupportedSensors = false,
            boostPressureAdjustmentBar = 0.0f
        )
        vehicleRepo.saveVehicle(civic)
        vehicleRepo.saveVehicle(miata)
        vehicleRepo.setActiveVehicle(miata.id)

        // 2. Setup custom PIDs and modified standard PID enablement
        val customPid = ObdPid(
            id = "custom_dpf_soot",
            name = "DPF Soot Mass",
            shortName = "Soot",
            modeAndPid = "222512",
            equation = "((A*256)+B)/100",
            minValue = 0f,
            maxValue = 30f,
            units = "g",
            header = "DA10F1",
            isCustom = true,
            isEnabled = true,
            category = PidCategory.EXHAUST_DPF,
            availabilityEquation = "A&1",
            unavailableRawValues = mapOf("A" to setOf(255)),
            valueLabels = mapOf(0 to "Clean", 1 to "Regen")
        )
        val defaults = AppBackupRepository.defaultPidCatalog()
        val configuredPids = defaults.mapIndexed { idx, pid ->
            if (idx == 0) pid.copy(isEnabled = false) else pid
        } + customPid
        sensorRepo.save(configuredPids)

        // 3. Setup custom dashboard tabs with custom widget scales, decimals, and carGridLayout
        val tab1 = CustomDashboardConfig(
            id = "tab-main",
            name = "Circuito",
            gridLayout = DashboardGridLayout.GRID_3X4,
            carGridLayout = DashboardGridLayout.GRID_2X3,
            density = DashboardDensity.COMPACT,
            widgets = emptyList(),
            slots = mapOf(
                0 to DashboardWidget(
                    id = "w-soot",
                    pidId = customPid.id,
                    pidShortName = customPid.shortName,
                    title = "Soot Load",
                    type = WidgetType.DIGITAL_HUD,
                    colorHex = "#00E5FF",
                    size = WidgetSize.FULL_WIDTH,
                    decimals = 2,
                    scaleMin = 0f,
                    scaleMax = 25f
                ),
                11 to DashboardWidget(
                    id = "w-hidden-in-car",
                    pidId = SensorIds.RPM,
                    pidShortName = "RPM",
                    title = "RPM Oculto",
                    type = WidgetType.ANALOG_GAUGE
                )
            )
        )
        val tab2 = CustomDashboardConfig(
            id = "tab-dpf",
            name = "DPF Monitor",
            gridLayout = DashboardGridLayout.SPLIT_1_2,
            widgets = emptyList(),
            slots = mapOf(
                0 to DashboardWidget(
                    id = "w-coolant",
                    pidId = SensorIds.COOLANT,
                    pidShortName = "Temp Liq",
                    title = "Coolant"
                )
            )
        )
        val dashboardTabs = CustomDashboardTabs(listOf(tab1, tab2), selectedId = tab2.id)
        vehicleRepo.saveCustomDashboardTabs(dashboardTabs)

        // 4. Setup alert rules
        val alertRule = SensorAlertRule(
            id = "alert-1",
            vehicleId = miata.id,
            pidId = customPid.id,
            sensorName = customPid.name,
            units = customPid.units,
            lowerLimit = 1.5,
            upperLimit = 22.0,
            durationSeconds = 5,
            enabled = true
        )
        alertRepo.save(listOf(alertRule))

        // 5. Setup interface preferences
        interfacePrefs.carMode = false
        interfacePrefs.languagePreference = AppLanguagePreference.EN

        // Export to stream
        val srcBackupRepo = AppBackupRepository(
            vehiclePrefs = srcVehiclePrefs,
            sensorPrefs = srcSensorPrefs,
            alertPrefs = srcAlertPrefs,
            interfacePrefs = srcInterfacePrefs,
            nowMillis = { 1760000000000L }
        )
        val out = ByteArrayOutputStream()
        srcBackupRepo.exportToStream(out, defaults)
        val exportedBytes = out.toByteArray()
        val exportedJson = exportedBytes.toString(Charsets.UTF_8)

        // Ensure connection/hardware data is never present in exported JSON
        assertFalse(exportedJson.contains("last_address"))
        assertFalse(exportedJson.contains("wifi_host"))
        assertFalse(exportedJson.contains("simulation_enabled"))

        // Import into a brand new device's SharedPreferences
        val dstVehiclePrefs = memoryPrefs()
        val dstSensorPrefs = memoryPrefs()
        val dstAlertPrefs = memoryPrefs()
        val dstInterfacePrefs = memoryPrefs()
        val dstBackupRepo = AppBackupRepository(
            vehiclePrefs = dstVehiclePrefs,
            sensorPrefs = dstSensorPrefs,
            alertPrefs = dstAlertPrefs,
            interfacePrefs = dstInterfacePrefs
        )

        val snapshot = dstBackupRepo.importFromStream(ByteArrayInputStream(exportedBytes), defaults)

        assertEquals(listOf(civic, miata), snapshot.vehicles)
        assertEquals(miata, snapshot.activeVehicle)
        assertEquals(dashboardTabs.selectedId, snapshot.dashboardTabs.selectedId)
        assertEquals(2, snapshot.dashboardTabs.dashboards.size)
        val restoredTab1 = snapshot.dashboardTabs.dashboards.first { it.id == "tab-main" }
        assertEquals(DashboardGridLayout.GRID_3X4, restoredTab1.gridLayout)
        assertEquals(DashboardGridLayout.GRID_2X3, restoredTab1.carGridLayout)
        assertEquals(DashboardDensity.COMPACT, restoredTab1.density)
        assertEquals(2, restoredTab1.slots[0]?.decimals)
        assertEquals(0f, restoredTab1.slots[0]?.scaleMin)
        assertEquals(25f, restoredTab1.slots[0]?.scaleMax)
        assertEquals("RPM Oculto", restoredTab1.slots[11]?.title)

        val restoredCustomPid = snapshot.pids.single { it.id == "custom_dpf_soot" }
        assertEquals(customPid, restoredCustomPid)
        assertFalse(snapshot.pids.first { it.id == defaults.first().id }.isEnabled)

        assertEquals(listOf(alertRule), snapshot.alertRules)
        assertFalse(snapshot.carMode)
        assertEquals(AppLanguagePreference.EN, snapshot.languagePreference)
    }

    @Test
    fun `invalid JSON or foreign JSON is rejected without mutating existing preferences`() {
        val vehiclePrefs = memoryPrefs()
        val sensorPrefs = memoryPrefs()
        val alertPrefs = memoryPrefs()
        val interfacePrefs = memoryPrefs()

        val vehicleRepo = VehicleProfileRepository(vehiclePrefs)
        val existingCar = VehicleProfile(id = "existing-id", name = "Existing Car")
        vehicleRepo.saveVehicle(existingCar)

        val backupRepo = AppBackupRepository(vehiclePrefs, sensorPrefs, alertPrefs, interfacePrefs)

        assertThrows(IllegalArgumentException::class.java) {
            backupRepo.importFromJson("not-json-at-all")
        }
        assertThrows(IllegalArgumentException::class.java) {
            backupRepo.importFromJson("""{"appId": "com.other.app", "schemaVersion": 1, "vehicles": {}}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            backupRepo.importFromJson("""{"appId": "com.stradaobd.dash", "schemaVersion": 99, "vehicles": {}}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            backupRepo.importFromJson("""{"hello": "world"}""")
        }

        // Existing vehicle must remain intact
        assertEquals(listOf(existingCar), vehicleRepo.getAllVehicles())
    }

    @Test
    fun `android manifest and xml backup rules match the Play Store cloud backup allowlist strategy`() {
        val projectRoot = File(".").canonicalFile.let { dir ->
            if (File(dir, "app/src/main/AndroidManifest.xml").exists()) dir else dir.parentFile
        }
        val manifestText = File(projectRoot, "app/src/main/AndroidManifest.xml").readText()
        val backupRulesText = File(projectRoot, "app/src/main/res/xml/backup_rules.xml").readText()
        val extractionRulesText = File(projectRoot, "app/src/main/res/xml/data_extraction_rules.xml").readText()

        assertTrue(manifestText.contains("""android:allowBackup="true""""))
        assertTrue(manifestText.contains("""android:fullBackupContent="@xml/backup_rules""""))
        assertTrue(manifestText.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))

        val expectedIncluded = listOf(
            "torque_obd_prefs.xml",
            "torque_sensors.xml",
            "sensor_alerts.xml",
            "interface.xml"
        )
        for (file in expectedIncluded) {
            assertTrue("backup_rules.xml missing $file", backupRulesText.contains("""path="$file""""))
            assertTrue("data_extraction_rules.xml missing $file", extractionRulesText.contains("""path="$file""""))
        }

        assertFalse(backupRulesText.contains("""<include domain="sharedpref" path="obd_connection.xml""""))
        assertFalse(extractionRulesText.contains("""<include domain="sharedpref" path="obd_connection.xml""""))
    }
}
