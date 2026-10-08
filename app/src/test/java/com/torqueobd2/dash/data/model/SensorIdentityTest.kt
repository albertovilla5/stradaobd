package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.data.parser.TorqueCsvParser
import com.torqueobd2.dash.obd.ObdPollingPolicy
import org.junit.Assert.*
import org.junit.Test

class SensorIdentityTest {
    private val a = ObdPid(id = "a", name = "Temperatura A", shortName = "TEMP", modeAndPid = "221001", equation = "A", header = "7E0")
    private val b = a.copy(id = "b", name = "Temperatura B", header = "7E1")

    @Test fun widgetIdWinsEvenWithDuplicateNamesAndRenamedLabels() {
        val widget = DashboardWidget(pidId = b.id, pidShortName = "TEMP")
        assertEquals(b, widget.resolvePid(listOf(a, b)))
        assertEquals(b.copy(shortName = "Renombrado"), widget.resolvePid(listOf(a, b.copy(shortName = "Renombrado"))))
        assertNull(widget.resolvePid(listOf(a)))
        val config = CustomDashboardConfig(widgets = listOf(widget))
        assertEquals(setOf(b.id), ObdPollingPolicy.screenSensors(1, config, listOf(a, b)))
    }

    @Test fun migrationBindsUniqueLegacyReferenceAndPreservesHiddenSlotsAndSelection() {
        val legacy = CustomDashboardConfig(widgets = emptyList(), slots = mapOf(0 to DashboardWidget(pidShortName = "TEMP"),
            9 to DashboardWidget(pidShortName = "TEMP", title = b.name)))
        val tabs = CustomDashboardTabs(listOf(legacy)).bindPids(listOf(a, b))
        assertEquals(legacy.id, tabs.selectedId)
        assertEquals("", tabs.selectedDashboard.slots.getValue(0).pidId)
        assertEquals(b.id, tabs.selectedDashboard.slots.getValue(9).pidId)
        assertNull(tabs.selectedDashboard.slots.getValue(0).resolvePid(listOf(a, b)))
        assertEquals(a.id, CustomDashboardTabs(listOf(legacy)).bindPids(listOf(a)).selectedDashboard.slots.getValue(0).pidId)
    }

    @Test fun freshDefaultsAreFourUsefulExplicitStandardReferences() {
        val widgets = CustomDashboardConfig.getDefaultWidgets()
        val pids = DefaultPids.getStandardPids()
        assertEquals(SensorIds.driving, widgets.map { it.pidId }.toSet())
        assertEquals(4, widgets.size)
        assertTrue(widgets.all { it.resolvePid(pids) != null })
        assertTrue(widgets.none { it.pidShortName in setOf("Potencia", "Par") })
    }

    @Test fun presetsExcludeProvenIncompatibleSensorsAndLabelUnknownSupport() {
        val pids = DefaultPids.getStandardPids().map { it.copy(supportChecked = true, isSupported = it.id != SensorIds.VOLTAGE) }
        val driving = DashboardPresets.forPids(pids).single { it.id == "sport" }
        assertEquals(SensorIds.driving - SensorIds.VOLTAGE, driving.widgets.map { it.pidId }.toSet())
        assertTrue(driving.subtitle.contains("Compatibles"))
        assertTrue(DashboardPresets.forPids(DefaultPids.getStandardPids()).single { it.id == "sport" }.subtitle.contains("por comprobar"))
        assertTrue(DashboardPresets.forPids(pids.map { it.copy(isSupported = false) }).isEmpty())
    }

    @Test fun csvImportsHaveStableIdsAndDifferentEcusOrEquationsRemainDistinct() {
        val csv = "\"Temperatura\",\"TEMP\",\"221001\",\"A\",0,100,\"C\",\"7E0\""
        val first = TorqueCsvParser.parseString(csv).pids.single()
        assertEquals(first.id, TorqueCsvParser.parseString(csv).pids.single().id)
        assertNotEquals(first.id, TorqueCsvParser.parseString(csv.replace("7E0", "7E1")).pids.single().id)
        assertNotEquals(first.id, TorqueCsvParser.parseString(csv.replace("\"A\"", "\"A*2\"")).pids.single().id)
        assertEquals(DefaultPids.getHondaIdtecPids().map { it.id }, DefaultPids.getHondaIdtecPids().map { it.id })
    }
}
