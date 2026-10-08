package com.torqueobd2.dash.data.model

/** IDs del catálogo estándar; los nombres son únicamente etiquetas de presentación. */
object SensorIds {
    const val RPM = "SAE_010C_RPM"
    const val SPEED = "SAE_010D_Velocidad"
    const val COOLANT = "SAE_0105_Temp Liq"
    const val VOLTAGE = "SAE_0142_Voltaje"
    val driving = setOf(RPM, SPEED, COOLANT, VOLTAGE)
}

/** Una referencia con ID nunca cambia de sensor por coincidencia de nombre. */
fun DashboardWidget.resolvePid(pids: List<ObdPid>): ObdPid? =
    if (pidId.isNotBlank()) pids.singleOrNull { it.id == pidId }
    else pids.filter { it.shortName == pidShortName }.let { matches ->
        matches.singleOrNull() ?: matches.singleOrNull { it.name == title }
    }

fun DashboardWidget.bindPid(pids: List<ObdPid>): DashboardWidget =
    if (pidId.isNotBlank()) this else resolvePid(pids)?.let { copy(pidId = it.id) } ?: this

fun CustomDashboardTabs.bindPids(pids: List<ObdPid>): CustomDashboardTabs = copy(
    dashboards = dashboards.map { config -> config.copy(
        slots = config.resolvedSlots().mapValues { (_, widget) -> widget.bindPid(pids) }, widgets = emptyList()
    ) }
)
