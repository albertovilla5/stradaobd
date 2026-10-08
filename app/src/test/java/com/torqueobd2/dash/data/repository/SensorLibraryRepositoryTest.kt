package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class SensorLibraryRepositoryTest {
    private fun prefs(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }
                method.name == "apply" -> null
                else -> error(method.name)
            }
        }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when {
                method.name.startsWith("get") -> values[args!![0]] ?: args[1]
                method.name == "edit" -> editor
                else -> error(method.name)
            }
        } as SharedPreferences
    }

    @Test fun restartPreservesDuplicateNamesIdsSelectionAndAllDecoderMetadata() {
        val storage = prefs()
        val a = ObdPid(id = "a", name = "Sensor", shortName = "TEMP", modeAndPid = "221001", equation = "B/2",
            minValue = -40f, maxValue = 125f, units = "C", header = "7E0", isCustom = true, isEnabled = false,
            category = PidCategory.EXHAUST_DPF, availabilityEquation = "A&1",
            unavailableRawValues = mapOf("B" to setOf(0, 255)), valueLabels = mapOf(1 to "Activo", 0 to "Inactivo"))
        val b = a.copy(id = "b", header = "7E1", isEnabled = true)
        SensorLibraryRepository(storage).save(listOf(a, b))
        assertEquals(listOf(a, b), SensorLibraryRepository(storage).load(emptyList()))
        val tabsStorage = prefs()
        val tabsRepository = CustomDashboardTabsRepository(tabsStorage)
        tabsRepository.save(CustomDashboardTabs(listOf(CustomDashboardConfig(widgets = listOf(
            DashboardWidget(pidId = b.id, pidShortName = b.shortName))))))
        val reopened = tabsRepository.load { error("Already saved") }
        val widget = reopened.selectedDashboard.getWidgetForSlot(0)!!
        assertEquals(b, widget.resolvePid(SensorLibraryRepository(storage).load(emptyList())))
    }

    @Test fun compatibilityIsNotRestoredForAnotherEcuAndCatalogUpdatesKeepUserSelection() {
        val storage = prefs()
        val original = DefaultPids.getStandardPids().first().copy(isEnabled = false, supportChecked = true, isSupported = false)
        SensorLibraryRepository(storage).save(listOf(original))
        val loaded = SensorLibraryRepository(storage).load(DefaultPids.getStandardPids())
        val restored = loaded.single { it.id == original.id }
        assertFalse(restored.isEnabled)
        assertTrue(restored.isSupported)
        assertFalse(restored.supportChecked)
        assertEquals(DefaultPids.getStandardPids().size, loaded.size)
    }
}
