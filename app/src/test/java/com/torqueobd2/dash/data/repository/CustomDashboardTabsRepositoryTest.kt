package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class CustomDashboardTabsRepositoryTest {
    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }
                method.name == "apply" -> null
                else -> error("Operación no esperada: ${method.name}")
            }
        }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getInt" -> values[args!![0]] ?: args[1]
                "edit" -> editor
                else -> error("Operación no esperada: ${method.name}")
            }
        } as SharedPreferences
    }

    @Test fun migratesExistingDashboardOnceWithoutLosingGridOrWidgets() {
        val repository = CustomDashboardTabsRepository(preferences())
        val original = CustomDashboardConfig(gridLayout = DashboardGridLayout.SPLIT_1_2,
            widgets = listOf(DashboardWidget(pidShortName = "RPM"), DashboardWidget(pidShortName = "MAP")),
            slots = mapOf(1 to DashboardWidget(pidShortName = "EGT")))
        val migrated = repository.load { original }
        assertEquals(original.id, migrated.selectedId)
        assertEquals("Mi dash", migrated.selectedDashboard.name)
        assertEquals(original.gridLayout, migrated.selectedDashboard.gridLayout)
        assertEquals(listOf("RPM", "EGT", null), migrated.selectedDashboard.getSlotList().map { it?.pidShortName })
        assertEquals(migrated, repository.load { error("No debe volver a migrar") })
    }

    @Test fun persistsIndependentNamesGridsWidgetsOrderAndSelection() {
        val repository = CustomDashboardTabsRepository(preferences())
        var tabs = repository.load { CustomDashboardConfig() }
        val firstId = tabs.selectedId
        tabs = tabs.rename(firstId, "Motor").add("Turbo", DashboardGridLayout.GRID_2X2)
        val turboId = tabs.selectedId
        tabs = tabs.updateSelected(tabs.selectedDashboard.withSlot(3, DashboardWidget(pidShortName = "MAP", size = WidgetSize.FULL_WIDTH)))
        tabs = tabs.move(turboId, 0)
        repository.save(tabs)
        val reopened = repository.load { error("No debe migrar") }
        assertEquals(tabs, reopened)
        assertEquals(turboId, reopened.selectedId)
        assertEquals(listOf("Turbo", "Motor"), reopened.dashboards.map { it.name })
        assertEquals(DashboardGridLayout.GRID_2X2, reopened.selectedDashboard.gridLayout)
        assertNull(reopened.selectedDashboard.getWidgetForSlot(0))
        assertEquals("MAP", reopened.selectedDashboard.getWidgetForSlot(3)?.pidShortName)
        assertEquals(WidgetSize.FULL_WIDTH, reopened.selectedDashboard.getWidgetForSlot(3)?.size)
        assertEquals("RPM", reopened.select(firstId).selectedDashboard.getWidgetForSlot(0)?.pidShortName)
    }

    @Test fun emptySlotsStayEmptyAndEditingOneDoesNotEraseOtherDefaults() {
        val original = CustomDashboardConfig()
        val changed = original.withSlot(2, DashboardWidget(pidShortName = "EGT"))
        assertEquals(original.getWidgetForSlot(0), changed.getWidgetForSlot(0))
        assertEquals(original.getWidgetForSlot(3), changed.getWidgetForSlot(3))
        val cleared = changed.withSlot(1, null)
        assertNull(cleared.getWidgetForSlot(1))
        assertEquals("EGT", cleared.getWidgetForSlot(2)?.pidShortName)
        val repository = CustomDashboardTabsRepository(preferences())
        repository.save(CustomDashboardTabs(listOf(cleared)))
        assertNull(repository.load { error("No debe migrar") }.selectedDashboard.getWidgetForSlot(1))
    }

    @Test fun changingGridsKeepsHiddenSlotsAndEditingDoesNotRenameTheTab() {
        var tabs = CustomDashboardTabs().add("Diagnóstico", DashboardGridLayout.GRID_2X3)
        tabs = tabs.updateSelected(tabs.selectedDashboard.withSlot(5, DashboardWidget(pidShortName = "EGT")))
        tabs = tabs.updateSelected(tabs.selectedDashboard.copy(gridLayout = DashboardGridLayout.GRID_1X2, name = "Plantilla"))
        assertEquals("Diagnóstico", tabs.selectedDashboard.name)
        assertEquals(2, tabs.selectedDashboard.getSlotList().size)
        val repository = CustomDashboardTabsRepository(preferences())
        repository.save(tabs)
        val larger = repository.load { error("No debe migrar") }.selectedDashboard.copy(gridLayout = DashboardGridLayout.GRID_2X3)
        assertEquals("EGT", larger.getWidgetForSlot(5)?.pidShortName)
    }

    @Test fun invalidMovesAndBlankNamesKeepTabsIntactAndDuplicateNamesRemainIndependent() {
        val tabs = CustomDashboardTabs().add("Mi dash", DashboardGridLayout.GRID_1X2)
        assertEquals(tabs, tabs.move(tabs.selectedId, -1))
        assertEquals(tabs, tabs.move("missing", 0))
        assertEquals(tabs, tabs.rename(tabs.selectedId, "  "))
        assertEquals(tabs, tabs.add("  ", DashboardGridLayout.GRID_1X2))
        assertEquals(tabs, tabs.select("missing"))
        assertEquals(2, tabs.dashboards.map { it.id }.distinct().size)
    }

    @Test fun persistsDenseGridsScalesDecimalsAndHiddenSlots() {
        val repository = CustomDashboardTabsRepository(preferences())
        val widget = DashboardWidget(pidId = "real-id", decimals = 3, scaleMin = -40f, scaleMax = 120f, colorHex = "#123456")
        val config = CustomDashboardConfig(gridLayout = DashboardGridLayout.GRID_4X6, density = DashboardDensity.COMPACT,
            slots = mapOf(0 to widget, 23 to widget.copy(id = "second")), widgets = emptyList())
        val tabs = CustomDashboardTabs(listOf(config))
        repository.save(tabs)
        assertEquals(tabs,repository.load { error("No debe migrar") })
        repository.save(CustomDashboardTabs(listOf(config.copy(gridLayout = DashboardGridLayout.GRID_1X1))))
        val reopened = repository.load { error("No debe migrar") }.selectedDashboard
        assertEquals(widget.copy(id = "second"),reopened.slots[23])
        assertEquals(DashboardDensity.COMPACT,reopened.density)
        repository.save(CustomDashboardTabs(listOf(reopened.withSlot(0,widget.copy(decimals=null,scaleMin=null,scaleMax=null)))))
        val reset = repository.load { error("No debe migrar") }.selectedDashboard.slots[0]!!
        assertNull(reset.decimals); assertNull(reset.scaleMin); assertNull(reset.scaleMax)
    }

    @Test fun carGridIsStoredIndependentlyAndClearingItDoesNotResurrectOldChoice() {
        val repository=CustomDashboardTabsRepository(preferences())
        val config=CustomDashboardConfig(gridLayout=DashboardGridLayout.GRID_4X6,carGridLayout=DashboardGridLayout.GRID_2X3,
            widgets=emptyList(),slots=mapOf(23 to DashboardWidget(title="Oculto")))
        repository.save(CustomDashboardTabs(listOf(config)))
        val loaded=repository.load { error("No migrar") }.selectedDashboard
        assertEquals(config,loaded)
        assertEquals(DashboardGridLayout.GRID_2X3,loaded.forCar().gridLayout)
        repository.save(CustomDashboardTabs(listOf(loaded.copy(carGridLayout=null))))
        val cleared=repository.load { error("No migrar") }.selectedDashboard
        assertNull(cleared.carGridLayout)
        assertEquals(DashboardGridLayout.GRID_2X4,cleared.forCar().gridLayout)
        assertEquals(config.slots,cleared.slots)
    }
    @Test fun missingSavedSelectionFallsBackToFirstTab() {
        val storage = preferences()
        val repository = CustomDashboardTabsRepository(storage)
        val tabs = CustomDashboardTabs().add("Turbo", DashboardGridLayout.GRID_1X2)
        repository.save(tabs)
        storage.edit().putString("dash_tabs_selected", "missing").apply()
        assertEquals(tabs.dashboards.first().id, repository.load { error("No debe migrar") }.selectedId)
    }
}
