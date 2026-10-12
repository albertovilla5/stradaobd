package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.CarIconToggleButton as IconToggleButton
import com.torqueobd2.dash.ui.automotive.CarDropdownMenuItem as DropdownMenuItem
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.components.FittedValue
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged

/** Monitor virtualizado compartido por Sensores y la pestaña de sensores del vehículo. */
@Composable
fun GridMonitorScreen(
    vehicleState: VehicleState,
    availablePids: List<ObdPid>,
    connectionState: ConnectionState,
    isScanningSensors: Boolean,
    onScanSensors: () -> Unit,
    onTogglePid: (String, Boolean) -> Unit,
    onVisibleSensorsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    catalogAction: (@Composable () -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<PidCategory?>(null) }
    var onlyAvailable by rememberSaveable { mutableStateOf(false) }
    var onlyEnabled by rememberSaveable { mutableStateOf(false) }
    var onlyCompatible by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedPidId by rememberSaveable { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val now = System.nanoTime()
    val statuses = availablePids.associate { it.id to it.telemetryStatus(vehicleState, connectionState, now) }
    val availableCount = statuses.values.count { it == SensorTelemetryStatus.AVAILABLE }
    val candidates = remember(availablePids, query, category, onlyEnabled, onlyCompatible) {
        availablePids.filter { pid ->
            (category == null || pid.category == category) &&
                (!onlyEnabled || pid.isEnabled) &&
                (!onlyCompatible || pid.supportChecked && pid.isSupported) &&
                (query.isBlank() || listOf(pid.name, pid.shortName, pid.cleanObdCommand(), pid.units)
                    .any { it.contains(query, ignoreCase = true) })
        }
    }
    val rows = if (onlyAvailable) candidates.filter { statuses[it.id] == SensorTelemetryStatus.AVAILABLE } else candidates
    DisposableEffect(Unit) { onDispose { onVisibleSensorsChanged(emptySet()) } }
    LaunchedEffect(gridState, candidates, onlyAvailable) {
        snapshotFlow {
            // Con «Solo disponibles» también se consultan los candidatos para descubrir
            // lecturas nuevas y recuperar las caducadas, aunque el filtro deje la vista vacía.
            val ids = gridState.layoutInfo.visibleItemsInfo.map { it.key }.toSet()
            candidates.filter { it.isEnabled && (onlyAvailable || it.id in ids) }.map { it.id }.toSet()
        }.distinctUntilChanged().collect { onVisibleSensorsChanged(it) }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(CarbonBlack).padding(horizontal = 8.dp)) {
        val toolbarNarrow = maxWidth < 550.dp
        val fontScale = uiLayoutScale()
        val columns = (maxWidth / (if (LocalCarMode.current) 460.dp * LocalDensity.current.fontScale.coerceAtLeast(1f) else 330.dp * fontScale)).toInt().coerceIn(1, 3)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!LocalCarMode.current || !toolbarNarrow) Column(Modifier.weight(1f)) {
                    Text(
                        com.torqueobd2.dash.i18n.tr(
                            "${rows.size}/${availablePids.size} sensores · $availableCount disponibles",
                            "${rows.size}/${availablePids.size} sensors · $availableCount available"
                        ),
                        color = TextWhite,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(when {
                        connectionState.status != ConnectionStatus.CONNECTED -> com.torqueobd2.dash.i18n.tr("Conecta el OBD para comprobar lecturas", "Connect OBD to check readings")
                        connectionState.type == ConnectionType.SIMULATION -> com.torqueobd2.dash.i18n.tr("Simulación · datos ficticios", "Simulation · demo data")
                        else -> com.torqueobd2.dash.i18n.tr(
                            "${availablePids.count { it.isEnabled }} activos · Disponible = lectura ECU reciente",
                            "${availablePids.count { it.isEnabled }} active · Available = recent ECU reading"
                        )
                    }, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) query = "" }) {
                    Icon(Icons.Default.Search, com.torqueobd2.dash.i18n.tr("Buscar sensores", "Search sensors"), Modifier.size(18.dp), tint = if (showSearch) NeonCyan else TextMuted)
                }
                Box {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, com.torqueobd2.dash.i18n.tr("Filtrar sensores", "Filter sensors"), Modifier.size(18.dp),
                            tint = if (onlyAvailable || onlyEnabled || onlyCompatible || category != null) NeonCyan else TextMuted)
                    }
                    DropdownMenu(showFilters, { showFilters = false }) {
                        DropdownMenuItem(text = { Text(if (onlyAvailable) com.torqueobd2.dash.i18n.tr("✓ Solo disponibles", "✓ Available only") else com.torqueobd2.dash.i18n.tr("Solo disponibles", "Available only")) },
                            onClick = { onlyAvailable = !onlyAvailable; showFilters = false })
                        DropdownMenuItem(text = { Text(if (onlyEnabled) com.torqueobd2.dash.i18n.tr("✓ Solo activos", "✓ Active only") else com.torqueobd2.dash.i18n.tr("Solo activos", "Active only")) },
                            onClick = { onlyEnabled = !onlyEnabled; showFilters = false })
                        DropdownMenuItem(text = { Text(if (onlyCompatible) com.torqueobd2.dash.i18n.tr("✓ PIDs compatibles", "✓ Supported PIDs") else com.torqueobd2.dash.i18n.tr("PIDs compatibles", "Supported PIDs")) },
                            onClick = { onlyCompatible = !onlyCompatible; showFilters = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(if (category == null) com.torqueobd2.dash.i18n.tr("✓ Todas las categorías", "✓ All categories") else com.torqueobd2.dash.i18n.tr("Todas las categorías", "All categories")) },
                            onClick = { category = null; showFilters = false })
                        PidCategory.entries.forEach { entry ->
                            DropdownMenuItem(text = { Text((if (category == entry) "✓ " else "") + entry.title) },
                                onClick = { category = entry; showFilters = false })
                        }
                    }
                }
                IconButton(onClick = onScanSensors,
                    enabled = connectionState.status == ConnectionStatus.CONNECTED && !isScanningSensors) {
                    if (isScanningSensors) CircularProgressIndicator(Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, com.torqueobd2.dash.i18n.tr("Escanear ECU", "Scan ECU"), Modifier.size(18.dp))
                }
                catalogAction?.invoke()
            }
            if (LocalCarMode.current && toolbarNarrow) Text(
                com.torqueobd2.dash.i18n.tr("${rows.size} sensores · $availableCount disponibles", "${rows.size} sensors · $availableCount available"),
                color = TextMuted, fontSize = 24.sp
            )
            if (showSearch) {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), singleLine = true,
                    placeholder = { Text(com.torqueobd2.dash.i18n.tr("Buscar sensor, PID o unidad", "Search sensor, PID or unit"), fontSize = 12.sp) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, com.torqueobd2.dash.i18n.tr("Limpiar búsqueda", "Clear search"), Modifier.size(18.dp))
                    } })
            }
            if (onlyAvailable || onlyEnabled || onlyCompatible || category != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(listOfNotNull(
                        category?.title,
                        if (onlyAvailable) com.torqueobd2.dash.i18n.tr("Disponibles", "Available") else null,
                        if (onlyEnabled) com.torqueobd2.dash.i18n.tr("Activos", "Active") else null,
                        if (onlyCompatible) com.torqueobd2.dash.i18n.tr("Compatibles", "Supported") else null
                    ).joinToString(" · "),
                        Modifier.weight(1f), fontSize = 11.sp, color = NeonCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { onlyAvailable = false; onlyEnabled = false; onlyCompatible = false; category = null }) {
                        Text(com.torqueobd2.dash.i18n.tr("Quitar filtros", "Clear filters"), fontSize = 11.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!LocalCarMode.current) repeat(columns) { SensorTableHeader(Modifier.weight(1f)) }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(columns), state = gridState,
                horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(rows, key = { it.id }) { pid ->
                    Column {
                        CompactSensorRow(pid, pid.currentReading(vehicleState, connectionState, now), statuses.getValue(pid.id),
                            onToggle = { onTogglePid(pid.id, it) }, onClick = { selectedPidId = pid.id })
                        HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                    }
                }
                if (rows.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (onlyAvailable) com.torqueobd2.dash.i18n.tr("Todavía no hay lecturas disponibles con estos filtros.", "No readings available with these filters yet.")
                        else com.torqueobd2.dash.i18n.tr("No hay sensores que coincidan.", "No matching sensors found."),
                        Modifier.padding(vertical = 16.dp), color = TextMuted, fontSize = 12.sp
                    )
                }
            }
        }
    }

    availablePids.find { it.id == selectedPidId }?.let { pid ->
        val reading = pid.currentReading(vehicleState, connectionState, now)
        ResponsiveDialog(onDismissRequest = { selectedPidId = null }) {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight),
                shape = MaterialTheme.shapes.large, color = CardBackground) {
                Column(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pid.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${reading?.formatted() ?: "—"} ${com.torqueobd2.dash.i18n.trUnit(pid.units)}".trim(), color = TextWhite, fontSize = 22.sp)
                        Text(statuses.getValue(pid.id).label, color = statusColor(statuses.getValue(pid.id)))
                        Text(statuses.getValue(pid.id).detail, color = TextMuted, fontSize = 12.sp)
                        Text("ID: ${pid.id}", color = TextMuted, fontSize = 10.sp)
                        Text("${pid.category.title} · ${pid.supportDescription}", color = TextMuted, fontSize = 12.sp)
                        Text("PID ${pid.cleanObdCommand()} · ${if (pid.isCustom) com.torqueobd2.dash.i18n.tr("Personalizado / CSV", "Custom / CSV") else com.torqueobd2.dash.i18n.tr("OBD estándar", "Standard OBD")}", color = TextMuted, fontSize = 12.sp)
                        Text("${com.torqueobd2.dash.i18n.tr("Ecuación", "Equation")}: ${pid.equation}" + if (pid.header.isNotBlank()) "\n${com.torqueobd2.dash.i18n.tr("Cabecera ECU", "ECU Header")}: ${pid.header}" else "",
                            color = TextMuted, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(com.torqueobd2.dash.i18n.tr("Consultar este sensor", "Poll this sensor"), Modifier.weight(1f), color = TextWhite)
                            Switch(pid.isEnabled, { onTogglePid(pid.id, it) })
                        }
                    }
                    TextButton(onClick = { selectedPidId = null }, modifier = Modifier.align(Alignment.End)) { Text(com.torqueobd2.dash.i18n.tr("Cerrar", "Close")) }
                }
            }
        }
    }
}

@Composable
private fun SensorTableHeader(modifier: Modifier) {
    Row(modifier.padding(end = 6.dp, top = 5.dp, bottom = 5.dp)) {
        Text("ON", Modifier.width(48.dp), color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
        Text("SENSOR / PID", Modifier.weight(0.46f), color = TextMuted, fontSize = 9.sp)
        Text(com.torqueobd2.dash.i18n.tr("VALOR", "VALUE"), Modifier.weight(0.27f), color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text(com.torqueobd2.dash.i18n.tr("ESTADO", "STATUS"), Modifier.weight(0.27f), color = TextMuted, fontSize = 9.sp)
    }
}

private fun statusColor(status: SensorTelemetryStatus): Color = when (status) {
    SensorTelemetryStatus.AVAILABLE -> NeonGreen
    SensorTelemetryStatus.SIMULATED -> NeonCyan
    SensorTelemetryStatus.NO_RESPONSE, SensorTelemetryStatus.INVALID_RESPONSE,
    SensorTelemetryStatus.STALE, SensorTelemetryStatus.INCOMPATIBLE -> WarningAmber
    else -> TextMuted
}

@Composable
private fun CompactSensorRow(
    pid: ObdPid, reading: ObdReading?, status: SensorTelemetryStatus,
    onToggle: (Boolean) -> Unit, onClick: () -> Unit
) {
    if (LocalCarMode.current) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 112.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconToggleButton(pid.isEnabled, onToggle) {
                Icon(if (pid.isEnabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    if (pid.isEnabled) com.torqueobd2.dash.i18n.tr("Desactivar ${pid.shortName}", "Disable ${pid.shortName}")
                    else com.torqueobd2.dash.i18n.tr("Activar ${pid.shortName}", "Enable ${pid.shortName}"),
                    tint = if (pid.isEnabled) NeonCyan else TextMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pid.shortName, color = TextWhite, fontSize = 24.sp, maxLines = 2)
                Text(status.label, color = statusColor(status), fontSize = 24.sp)
            }
            Column(Modifier.widthIn(min = 76.dp, max = 116.dp), horizontalAlignment = Alignment.End) {
                FittedValue(reading?.formatted() ?: "—", 32.sp, color = TextWhite, textAlign = TextAlign.End)
                Text(com.torqueobd2.dash.i18n.trUnit(pid.units), color = TextMuted, fontSize = 24.sp)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth().background(if (status == SensorTelemetryStatus.AVAILABLE) NeonGreen.copy(alpha = 0.035f) else Color.Transparent)
        .clickable(onClick = onClick).heightIn(min = 48.dp).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(checked = pid.isEnabled, onCheckedChange = onToggle, modifier = Modifier.size(48.dp)) {
            Icon(if (pid.isEnabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                if (pid.isEnabled) com.torqueobd2.dash.i18n.tr("Desactivar ${pid.shortName}", "Disable ${pid.shortName}")
                else com.torqueobd2.dash.i18n.tr("Activar ${pid.shortName}", "Enable ${pid.shortName}"),
                Modifier.size(18.dp), tint = if (pid.isEnabled) NeonCyan else TextMuted)
        }
        Column(Modifier.weight(0.46f).padding(vertical = 3.dp)) {
            Text(pid.shortName, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pid.cleanObdCommand(), color = TextMuted, fontSize = 9.sp, maxLines = 1)
        }
        FittedValue("${reading?.formatted() ?: "—"} ${com.torqueobd2.dash.i18n.trUnit(pid.units)}".trim(), 14.sp, Modifier.weight(0.27f),
            color = if (reading != null) TextWhite else TextMuted, textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text(status.label, Modifier.weight(0.27f), color = statusColor(status), fontSize = 10.sp,
            fontWeight = if (status == SensorTelemetryStatus.AVAILABLE) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
