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
                        Text(if (duplicate) "Duplicar indicador" else "Mover / intercambiar", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    item { Text("${config.getWidgetForSlot(source)?.title} · Ranura ${source + 1}", color = RacingOrange) }
                    item {
                        Text(if (duplicate) "Selecciona una ranura vacía. La copia se puede editar por separado." else "Selecciona el destino. Si está ocupado, los indicadores intercambian sus posiciones.", color = TextMuted)
                    }
                    items((0 until config.gridLayout.slotCount).filter { it != source }) { index ->
                        val target = config.getWidgetForSlot(index)
                        val enabled = !duplicate || target == null
                        Card(Modifier.fillMaxWidth().heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp).semantics { contentDescription = "Destino ranura ${index + 1}" }
                            .clickable(enabled = enabled) { onSelect(index) }, colors = CardDefaults.cardColors(containerColor = DarkSurface), border = BorderStroke(1.dp, BorderDark)) {
                            Text("Ranura ${index + 1} · ${target?.title ?: "Vacía"}" + if (target != null && !duplicate) " · Intercambiar" else "",
                                color = if (enabled) TextWhite else TextDim, modifier = Modifier.padding(12.dp))
                        }
                    }
                    if (duplicate && config.getSlotList().none { it == null }) item {
                        Text("No hay ranuras vacías. Amplía la rejilla para crear una copia.", color = WarningAmber)
                    }
                }
                TextButton(onDismiss, Modifier.align(androidx.compose.ui.Alignment.End).heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp)) { Text("Cancelar") }
            }
        }
    }
}

@Composable
internal fun DashboardDeleteDialog(name: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ResponsiveFormDialog("Eliminar pestaña", "Eliminar", onDismiss, onDelete) {
        Text("Se eliminará «$name» con todos sus indicadores. Los sensores seguirán en la biblioteca.", color = TextWhite)
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
                    "Diseños exportados"
                } catch (e: Exception) { "No se pudo exportar: ${e.message}" }
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
                            require(out.size() + size <= DashboardDesignCodec.MAX_BYTES) { "El archivo supera 2 MB" }
                            out.write(buffer, 0, size)
                        }
                        DashboardDesignCodec.read(out.toString("UTF-8"))
                    }
                }
                "Archivo válido. Revisa las pestañas antes de importar."
            } catch (e: Exception) { "No se pudo importar: ${e.message}" }
            busy = false
        }
    }
    ResponsiveDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.fillMaxWidth().heightIn(max = maxHeight), color = CardBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Importar / exportar diseños", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Se guardan rejillas, presentación, sensores asignados, tipos, títulos, colores, escalas y decimales. Las lecturas y la configuración del adaptador quedan fuera del archivo.", color = TextMuted)
                    OutlinedButton(onClick = {
                        exportText = DashboardDesignCodec.write(CustomDashboardTabs(listOf(tabs.selectedDashboard)))
                        save.launch("torque-cuadro-${System.currentTimeMillis()}.json")
                    }, enabled = !busy) { Text("Exportar esta pestaña") }
                    OutlinedButton(onClick = {
                        exportText = DashboardDesignCodec.write(tabs); save.launch("torque-disenos-${System.currentTimeMillis()}.json")
                    }, enabled = !busy) { Text("Exportar todas las pestañas") }
                    OutlinedButton(onClick = { open.launch(arrayOf("*/*")) }, enabled = !busy) { Text("Abrir archivo de diseños") }
                    message?.let { Text(it, color = if (imported != null) NeonGreen else WarningAmber) }
                    imported?.let { configs ->
                        configs.forEach { config -> Text("${config.name} · ${config.gridLayout.badge} · ${config.resolvedSlots().size} indicadores · ${config.density.title}", color = TextWhite) }
                        val missing = configs.flatMap { it.resolvedSlots().values }.count { it.resolvePid(pids) == null }
                        if (missing > 0) Text("$missing indicadores sin sensor encontrado. Podrás asignarlos desde el editor después de importar.", color = WarningAmber)
                        Text("Se añadirán como pestañas nuevas. Tus cuadros actuales se conservan.", color = TextMuted)
                        Button(onClick = { onImport(configs); imported = null; message = "Diseños importados como pestañas nuevas" }, enabled = !busy) { Text("Importar como pestañas nuevas") }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text("Cerrar") }
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
                    Text("Añadir varios indicadores", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${selected.size} seleccionados · $limit ranuras vacías. Se añadirán como tarjetas compactas.", color = TextMuted, fontSize = 12.sp)
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Buscar sensores") }, singleLine = true)
                    TextButton(onClick = { selected = if (selected.isEmpty()) candidates.take(limit).map { it.id }.toSet() else emptySet() }) {
                        Text(if (selected.isEmpty()) "Seleccionar hasta $limit" else "Quitar selección")
                    }
                    LazyColumn(Modifier.fillMaxWidth().height(176.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (candidates.isEmpty()) item { Text("Sin sensores para añadir. Cambia la búsqueda o revisa la biblioteca.", color = TextMuted) }
                        items(candidates, key = { it.id }) { pid ->
                            val checked = pid.id in selected
                            val enabled = checked || selected.size < limit
                            Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { selected = if (checked) selected - pid.id else selected + pid.id },
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked, { value -> selected = if (value) selected + pid.id else selected - pid.id }, enabled = enabled)
                                Column(Modifier.weight(1f)) {
                                    Text(pid.name, color = if (enabled) TextWhite else TextDim, fontSize = 13.sp)
                                    Text("${pid.modeAndPid} · ${pid.units} · ${pid.supportDescription}", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onDismiss, Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                        com.torqueobd2.dash.ui.components.FittedValue("Cancelar",13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Button(onClick = { onAdd(selected.toList()) }, enabled = selected.isNotEmpty(), modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                        com.torqueobd2.dash.ui.components.FittedValue("Añadir seleccionados",13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}
