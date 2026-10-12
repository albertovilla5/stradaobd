package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.repository.DashboardDesignCodec
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.i18n.trUnit
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.components.ResponsiveFormDialog
import com.torqueobd2.dash.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
internal fun DashboardTransferDialog(config: CustomDashboardConfig, source: Int, duplicate: Boolean,
    onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    ResponsiveDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().heightIn(max = minOf(maxHeight, 500.dp)), color = CardBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Text(if (duplicate) tr("Duplicar indicador", "Duplicate gauge") else tr("Mover / intercambiar", "Move / swap"), color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    item { Text("${config.getWidgetForSlot(source)?.localizedTitle} · ${tr("Ranura ${source + 1}", "Slot ${source + 1}")}", color = RacingOrange) }
                    item {
                        Text(
                            if (duplicate) tr("Selecciona una ranura vacía. La copia se puede editar por separado.", "Select an empty slot. The copy can be edited separately.")
                            else tr("Selecciona el destino. Si está ocupado, los indicadores intercambian sus posiciones.", "Select the destination. If occupied, the gauges will swap positions."),
                            color = TextMuted
                        )
                    }
                    items((0 until config.gridLayout.slotCount).filter { it != source }) { index ->
                        val target = config.getWidgetForSlot(index)
                        val enabled = !duplicate || target == null
                        val slotDesc = tr("Destino ranura ${index + 1}", "Target slot ${index + 1}")
                        Card(Modifier.fillMaxWidth().heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp).semantics { contentDescription = slotDesc }
                            .clickable(enabled = enabled) { onSelect(index) }, colors = CardDefaults.cardColors(containerColor = DarkSurface), border = BorderStroke(1.dp, BorderDark)) {
                            val targetLabel = target?.localizedTitle ?: tr("Vacía", "Empty")
                            Text(tr("Ranura ${index + 1} · $targetLabel", "Slot ${index + 1} · $targetLabel") + if (target != null && !duplicate) tr(" · Intercambiar", " · Swap") else "",
                                color = if (enabled) TextWhite else TextDim, modifier = Modifier.padding(12.dp))
                        }
                    }
                    if (duplicate && config.getSlotList().none { it == null }) item {
                        Text(tr("No hay ranuras vacías. Amplía la rejilla para crear una copia.", "No empty slots available. Expand the grid to create a copy."), color = WarningAmber)
                    }
                }
                TextButton(onDismiss, Modifier.align(androidx.compose.ui.Alignment.End).heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp)) { Text(tr("Cancelar", "Cancel")) }
            }
        }
    }
}

@Composable
internal fun DashboardDeleteDialog(name: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ResponsiveFormDialog(tr("Eliminar pestaña", "Delete tab"), tr("Eliminar", "Delete"), onDismiss, onDelete) {
        Text(
            tr(
                "Se eliminará «$name» con todos sus indicadores. Los sensores seguirán en la biblioteca.",
                "\"$name\" and all its gauges will be deleted. Sensors will remain in your library."
            ),
            color = TextWhite
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardDesignFilesDialog(tabs: CustomDashboardTabs, pids: List<ObdPid>, onImport: (List<CustomDashboardConfig>) -> Unit,
    onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportText by remember { mutableStateOf("") }
    var imported by remember { mutableStateOf<List<CustomDashboardConfig>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val text = exportText
            scope.launch {
                busy = true
                message = try {
                    withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(text) } }
                    tr("Diseños exportados", "Designs exported")
                } catch (e: Exception) { tr("No se pudo exportar: ${e.message}", "Could not export: ${e.message}") }
                busy = false
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; imported = null
            message = try {
                imported = withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val buffer = ByteArray(8192); val out = ByteArrayOutputStream()
                        while (true) {
                            val size = input.read(buffer); if (size < 0) break
                            require(out.size() + size <= DashboardDesignCodec.MAX_BYTES) { tr("El archivo supera 2 MB", "File exceeds 2 MB") }
                            out.write(buffer, 0, size)
                        }
                        DashboardDesignCodec.read(out.toString("UTF-8"))
                    }
                }
                tr("Archivo válido. Revisa las pestañas antes de importar.", "Valid file. Review tabs before importing.")
            } catch (e: Exception) { tr("No se pudo importar: ${e.message}", "Could not import: ${e.message}") }
            busy = false
        }
    }
    ResponsiveDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.fillMaxWidth().heightIn(max = maxHeight), color = CardBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Importar / exportar diseños", "Import / export designs"), color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        tr(
                            "Se guardan rejillas, presentación, sensores asignados, tipos, títulos, colores, escalas y decimales. Las lecturas y la configuración del adaptador quedan fuera del archivo.",
                            "Grids, layout density, assigned sensors, types, titles, colors, scales and decimals are saved. Live readings and adapter settings are not included in the file."
                        ),
                        color = TextMuted
                    )
                    OutlinedButton(onClick = {
                        exportText = DashboardDesignCodec.write(CustomDashboardTabs(listOf(tabs.selectedDashboard)))
                        save.launch("stradaobd-cuadro-${System.currentTimeMillis()}.json")
                    }, enabled = !busy) { Text(tr("Exportar esta pestaña", "Export this tab")) }
                    OutlinedButton(onClick = {
                        exportText = DashboardDesignCodec.write(tabs); save.launch("stradaobd-disenos-${System.currentTimeMillis()}.json")
                    }, enabled = !busy) { Text(tr("Exportar todas las pestañas", "Export all tabs")) }
                    OutlinedButton(onClick = { open.launch(arrayOf("*/*")) }, enabled = !busy) { Text(tr("Abrir archivo de diseños", "Open designs file")) }
                    message?.let { Text(it, color = if (imported != null) NeonGreen else WarningAmber) }
                    imported?.let { configs ->
                        configs.forEach { config ->
                            Text(
                                "${config.localizedName} · ${config.gridLayout.badge} · ${tr("${config.resolvedSlots().size} indicadores", "${config.resolvedSlots().size} gauges")} · ${config.density.title}",
                                color = TextWhite
                            )
                        }
                        val missing = configs.flatMap { it.resolvedSlots().values }.count { it.resolvePid(pids) == null }
                        if (missing > 0) Text(
                            tr(
                                "$missing indicadores sin sensor encontrado. Podrás asignarlos desde el editor después de importar.",
                                "$missing gauges without a matching sensor. You can assign them in the editor after importing."
                            ),
                            color = WarningAmber
                        )
                        Text(tr("Se añadirán como pestañas nuevas. Tus cuadros actuales se conservan.", "They will be added as new tabs. Your existing dashboards are preserved."), color = TextMuted)
                        Button(onClick = { onImport(configs); imported = null; message = tr("Diseños importados como pestañas nuevas", "Designs imported as new tabs") }, enabled = !busy) {
                            Text(tr("Importar como pestañas nuevas", "Import as new tabs"))
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text(tr("Cerrar", "Close")) }
            }
        }
    }
}

@Composable
internal fun DashboardBulkSensorsDialog(config: CustomDashboardConfig, pids: List<ObdPid>, onDismiss: () -> Unit, onAdd: (List<String>) -> Unit) {
    val limit = config.getSlotList().count { it == null }
    val existing = config.getSlotList().mapNotNull { it?.resolvePid(pids)?.id }.toSet()
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val candidates = pids.filter { it.id !in existing && (!it.supportChecked || it.isSupported) &&
        "${it.name} ${it.shortName} ${it.modeAndPid} ${it.header}".contains(search.trim(), true) }
    ResponsiveDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().height(minOf(maxHeight, 600.dp)), color = CardBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Añadir varios indicadores", "Add multiple gauges"), color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        tr(
                            "${selected.size} seleccionados · $limit ranuras vacías. Se añadirán como tarjetas compactas.",
                            "${selected.size} selected · $limit empty slots. Will be added as compact tiles."
                        ),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text(tr("Buscar sensores", "Search sensors")) }, singleLine = true)
                    TextButton(onClick = { selected = if (selected.isEmpty()) candidates.take(limit).map { it.id }.toSet() else emptySet() }) {
                        Text(if (selected.isEmpty()) tr("Seleccionar hasta $limit", "Select up to $limit") else tr("Quitar selección", "Clear selection"))
                    }
                    LazyColumn(Modifier.fillMaxWidth().height(176.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (candidates.isEmpty()) item { Text(tr("Sin sensores para añadir. Cambia la búsqueda o revisa la biblioteca.", "No sensors to add. Change your search or check your library."), color = TextMuted) }
                        items(candidates, key = { it.id }) { pid ->
                            val checked = pid.id in selected
                            val enabled = checked || selected.size < limit
                            Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { selected = if (checked) selected - pid.id else selected + pid.id },
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked, { value -> selected = if (value) selected + pid.id else selected - pid.id }, enabled = enabled)
                                Column(Modifier.weight(1f)) {
                                    Text(pid.name, color = if (enabled) TextWhite else TextDim, fontSize = 13.sp)
                                    Text("${pid.modeAndPid} · ${trUnit(pid.units)} · ${pid.supportDescription}", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onDismiss, Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                        com.torqueobd2.dash.ui.components.FittedValue(tr("Cancelar", "Cancel"),13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Button(onClick = { onAdd(selected.toList()) }, enabled = selected.isNotEmpty(), modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                        com.torqueobd2.dash.ui.components.FittedValue(tr("Añadir seleccionados", "Add selected"),13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}
