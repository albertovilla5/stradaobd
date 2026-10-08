package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.SensorIds
import com.torqueobd2.dash.data.model.resolvePid

internal data class PollingRequest(val header: String, val command: String, val pids: List<ObdPid>)

/** Selección por pantalla, dependencias de valores calculados y consultas compartidas. */
object ObdPollingPolicy {
    val backgroundSensors = SensorIds.driving

    fun screenSensors(tabIndex: Int, dashboard: CustomDashboardConfig, pids: List<ObdPid> = emptyList()): Set<String> = when (tabIndex) {
        0 -> SensorIds.driving
        1 -> dashboard.getSlotList().mapNotNull { widget ->
            widget?.let { if (it.pidId.isNotBlank()) it.pidId else it.resolvePid(pids)?.id }
        }.toSet()
        else -> emptySet() // Las listas notifican sus elementos visibles.
    }

    // Las tarjetas sin un PID real no activan cálculos ni consultas de respaldo.
    fun withDependencies(sensors: Set<String>): Set<String> = sensors

    internal fun requests(pids: List<ObdPid>, hideUnsupported: Boolean, initialHeader: String): List<PollingRequest> {
        val groups = linkedMapOf<Pair<String, String>, MutableList<ObdPid>>()
        for (pid in pids) {
            if (!pid.isEnabled || (hideUnsupported && !pid.isSupported)) continue
            val command = pid.cleanObdCommand()
            if (command.startsWith("CALC_")) continue
            val header = pid.header.ifBlank { initialHeader }.replace(" ", "").uppercase()
            groups.getOrPut(header to command) { mutableListOf() }.add(pid)
        }
        return groups.map { (key, sensors) -> PollingRequest(key.first, key.second, sensors) }
    }
}

/** El presupuesto de fondo es global: nunca una ráfaga de todos los sensores ocultos. */
internal class BackgroundPollingBudget {
    private var nextAllowedMillis: Long? = null
    private var cursor = 0

    fun next(candidates: List<PollingRequest>, nowMillis: Long): PollingRequest? {
        if (candidates.isEmpty() || (nextAllowedMillis?.let { nowMillis < it } == true)) return null
        val request = candidates[cursor % candidates.size]
        cursor = (cursor + 1) % candidates.size
        nextAllowedMillis = nowMillis + 2000L
        return request
    }

    fun recordCompletion(nowMillis: Long) {
        nextAllowedMillis = nowMillis + 2000L
    }
}
