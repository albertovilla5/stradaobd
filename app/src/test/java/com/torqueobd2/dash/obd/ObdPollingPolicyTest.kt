package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ObdPollingPolicyTest {
    private fun pid(name: String, command: String = "010C", header: String = "") =
        ObdPid(name = name, shortName = name, modeAndPid = command, equation = "A", header = header)

    @Test fun unavailablePowerWidgetsDoNotPollApproximationInputs() {
        assertEquals(setOf("Potencia"), ObdPollingPolicy.withDependencies(setOf("Potencia")))
        assertEquals(setOf("Par"), ObdPollingPolicy.withDependencies(setOf("Par")))
        assertEquals(setOf("RPM"), ObdPollingPolicy.withDependencies(setOf("RPM")))
    }

    @Test fun customDashboardOnlyUsesSlotsInCurrentLayout() {
        val widgets = listOf("RPM", "MAP", "EGT", "Voltaje").map { DashboardWidget(pidShortName = it, pidId = it) }
        val config = CustomDashboardConfig(gridLayout = DashboardGridLayout.GRID_1X2, widgets = widgets,
            slots = mapOf(1 to DashboardWidget(pidShortName = "Temp Liq", pidId = "Temp Liq"), 8 to DashboardWidget(pidShortName = "MAF")))
        assertEquals(setOf("RPM", "Temp Liq"), ObdPollingPolicy.screenSensors(1, config))
    }

    @Test fun sportFollowsTheUpdatedNavigation() {
        assertEquals(SensorIds.driving, ObdPollingPolicy.screenSensors(0, CustomDashboardConfig()))
    }

    @Test fun configurationAndTerminalScreensDoNotRequestTelemetry() {
        for (tab in listOf(2, 3, 4, 5, 6)) assertTrue(ObdPollingPolicy.screenSensors(tab, CustomDashboardConfig()).isEmpty())
    }

    @Test fun groupsSharedPayloadWithoutMixingEcusOrSendingCalculatedIds() {
        val pids = listOf(pid("Uno", "222512", "7E0"), pid("Dos", "222512"),
            pid("Tres", "222512", "7E1"), pid("Potencia", "CALC_HP"))
        val requests = ObdPollingPolicy.requests(pids, true, "DA10F1")
        assertEquals(3, requests.size)
        assertEquals("7E0", requests[0].header)
        assertEquals(listOf("Uno"), requests[0].pids.map { it.shortName })
        assertEquals("DA10F1", requests[1].header)
        assertEquals("7E1", requests[2].header)
    }

    @Test fun disabledAndUnsupportedSensorsRespectUserSelection() {
        val pids = listOf(pid("Activo"), pid("Apagado").copy(isEnabled = false), pid("No disponible").copy(isSupported = false))
        assertEquals(listOf("Activo"), ObdPollingPolicy.requests(pids, true, "").single().pids.map { it.shortName })
        assertEquals(listOf("Activo", "No disponible"), ObdPollingPolicy.requests(pids, false, "").single().pids.map { it.shortName })
    }

    @Test fun backgroundHasOneGlobalSlotEveryTwoSecondsAndRotatesSensors() {
        val requests = ObdPollingPolicy.requests(listOf(pid("RPM"), pid("Temp Liq", "0105"), pid("Voltaje", "0142")), true, "")
        val budget = BackgroundPollingBudget()
        assertEquals("010C", budget.next(requests, 0)?.command)
        assertNull(budget.next(requests, 0))
        assertNull(budget.next(requests, 1999))
        assertEquals("0105", budget.next(requests, 2000)?.command)
        assertEquals("0142", budget.next(requests, 4000)?.command)
        assertEquals("010C", budget.next(requests, 6000)?.command)
    }

    @Test fun emptyBackgroundDoesNotConsumeTheNextSlot() {
        val budget = BackgroundPollingBudget()
        assertNull(budget.next(emptyList(), 0))
        val request = ObdPollingPolicy.requests(listOf(pid("RPM")), true, "").single()
        assertEquals(request, budget.next(listOf(request), 0))
        assertNull(budget.next(listOf(request), 100))
    }

    @Test fun slowForegroundOrResponseCannotCauseBackgroundBurst() {
        val requests = ObdPollingPolicy.requests(listOf(pid("RPM"), pid("Voltaje", "0142")), true, "")
        val budget = BackgroundPollingBudget()
        assertEquals(requests[0], budget.next(requests, 0))
        budget.recordCompletion(5000)
        assertNull(budget.next(requests, 5000))
        assertNull(budget.next(requests, 6999))
        assertEquals(requests[1], budget.next(requests, 7000))
    }
}
