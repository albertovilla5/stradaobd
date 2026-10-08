package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarFilterChip as FilterChip
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import com.torqueobd2.dash.ui.automotive.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.repository.DtcCatalog
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DtcDiagnosticsScreen(state: DtcDiagnosticState, connection: ConnectionState, catalog: DtcCatalog,
    onRead: () -> Unit, onCancel: () -> Unit, onClear: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val report = state.report
    val connected = connection.status == ConnectionStatus.CONNECTED
    val car = LocalCarMode.current
    var actions by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("codes") }
    BackHandler(enabled = car && (actions || section != "codes")) { actions = false; section = "codes" }
    var filter by remember { mutableStateOf<DtcKind?>(null) }
    var search by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<TroubleCode?>(null) }
    var confirmation by remember { mutableStateOf<DiagnosticReport?>(null) }
    var engineStopped by remember { mutableStateOf(false) }
    var showMonitors by remember { mutableStateOf(false) }
    var showFreeze by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val saveReport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val text = exportText
            scope.launch {
                notice = try {
                    withContext(Dispatchers.IO) {
                        requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(text) }
                    }
                    "Informe guardado"
                } catch (e: Exception) { "No se pudo guardar: ${e.message}" }
            }
        }
    }
    LaunchedEffect(report?.capturedAt, connected) {
        if (confirmation?.capturedAt != report?.capturedAt || !connected) confirmation = null
        detail = null
    }
    Column(Modifier.fillMaxSize()) {
    if (car) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onRead, enabled = connected && !state.busy, modifier = Modifier.weight(1f)) {
            Text("Leer averías", fontSize = 24.sp)
        }
        if (state.busy) CarIconButton(onCancel) { CarIcon(Icons.Default.Close, "Cancelar consulta") }
        CarIconButton({ actions = !actions }) { CarIcon(if (actions) Icons.Default.Close else Icons.Default.MoreHoriz,
            if (actions) "Cerrar opciones de averías" else "Opciones de averías", tint = CockpitAccent) }
    }
    if (car && actions) CarMenu(listOf(
        CarMenuEntry("Códigos de error", if (report == null) "Confirmados, pendientes y permanentes" else "${report.codes.size} códigos en la última consulta", Icons.Default.CarRepair) { section = "codes"; actions = false },
        CarMenuEntry("Buscar códigos", "Filtrar por tipo, descripción o ECU", Icons.Default.Search, enabled = report != null) { section = "filter"; actions = false },
        CarMenuEntry("Informe", "Captura, copiar y guardar TXT", Icons.Default.Description, enabled = report != null) { section = "report"; actions = false },
        CarMenuEntry("Monitores y testigo", "Estado MIL y comprobaciones ECU", Icons.Default.Speed, enabled = report != null) { section = "monitors"; showMonitors = true; actions = false },
        CarMenuEntry("Datos congelados", "Condiciones cuando apareció el fallo", Icons.Default.AcUnit, enabled = report != null) { section = "freeze"; showFreeze = true; actions = false },
        CarMenuEntry("Borrar averías", "Requiere confirmación; guarda antes el informe", Icons.Default.Delete, enabled = connected && !state.busy && report != null) {
            confirmation = report; engineStopped = false; actions = false
        }
    ), Modifier.weight(1f)) else
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!car || report == null) item {
            Text(if (car) "Códigos de error OBD" else "Diagnóstico OBD de emisiones", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (!car) Text("Consulta las ECU que responden al OBD estándar. Guarda una captura con códigos, condiciones y respuestas originales.", color = TextMuted)
        }
        if (connection.type == ConnectionType.SIMULATION) item { Notice(if (car) "SIMULACIÓN · Datos de prueba" else "SIMULACIÓN · Averías ficticias para probar el diagnóstico", WarningAmber) }
        item {
            if (!car) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onRead, enabled = connected && !state.busy) { Text(if (report == null) "Leer averías" else "Volver a leer") }
                OutlinedButton(onClick = { confirmation = report; engineStopped = false }, enabled = connected && !state.busy && report != null) { Text("Borrar averías") }
                if (state.busy) OutlinedButton(onClick = onCancel) { Text("Cancelar consulta") }
            }
            if (!connected) Text("Conecta el adaptador en Conexión para consultar la ECU.", color = TextMuted)
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = RacingOrange)
                Text(state.progress, color = RacingOrange, modifier = Modifier.padding(top = 8.dp))
            }
        }
        state.error?.let { item { Notice(it, DangerRed) } }
        state.clearResult?.let { result -> item { Notice(result.message, if (result.status == DiagnosticStatus.OK) NeonGreen else WarningAmber) } }
        notice?.let { item { Notice(it, NeonCyan) } }
        if (report != null) {
            if (!car || section == "report") item {
                DiagnosticCard {
                    Text("Última captura · ${if (report.complete) "Lectura completa" else "Revisar resultados parciales"}", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("${report.vehicleName} · ${report.adapterName}", color = TextMuted)
                    Text(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(report.capturedAt)), color = TextMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val text = report.asText { catalog.describe(it).text }
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Informe DTC", text))
                            notice = "Informe copiado"
                        }) { Text("Copiar informe") }
                        TextButton(onClick = {
                            exportText = report.asText { catalog.describe(it).text }
                            saveReport.launch("averias-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(report.capturedAt))}.txt")
                        }) { Text("Guardar TXT") }
                    }
                }
            }
            if (!car || section == "report") item {
                if (car) Text(report.batches.joinToString(" · ") { "${it.codes.size} ${it.kind.label.lowercase()}" }, color = CockpitMuted, fontSize = 24.sp)
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.batches.forEach { batch -> DiagnosticCard(Modifier.weight(1f)) {
                        Text(batch.codes.size.toString(), color = kindColor(batch.kind), fontSize = 28.sp)
                        Text(batch.kind.label, color = TextWhite)
                        Text(batch.status.label, color = TextMuted, fontSize = 12.sp)
                    } }
                }
            }
            if (!car || section == "filter") item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(filter == null, { filter = null }, label = { Text("Todos") })
                    DtcKind.entries.forEach { kind -> FilterChip(filter == kind, { filter = kind }, label = { Text(kind.label) }) }
                }
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Buscar código, descripción o ECU") }, singleLine = true)
                if (car) Button({ section = "codes" }, Modifier.fillMaxWidth()) { Text("Ver códigos", fontSize = 24.sp) }
            }
            if (!car || section == "codes") report.batches.filter { filter == null || it.kind == filter }.forEach { batch ->
                item(key = "batch-${batch.kind}") {
                    Text(batch.kind.label, color = kindColor(batch.kind), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!car) Text(batch.kind.explanation, color = TextMuted, fontSize = 13.sp)
                    if (batch.codes.isEmpty()) Text(if (batch.status == DiagnosticStatus.OK) "Sin códigos en las ECU que respondieron: ${batch.respondingEcus.joinToString()}" else batch.status.label, color = TextWhite)
                    batch.issues.forEach { Text(it, color = WarningAmber, fontSize = 13.sp) }
                }
                val matching = batch.codes.filter { code ->
                    "${code.code} ${code.ecu} ${catalog.describe(code.code).text}".contains(search.trim(), ignoreCase = true)
                }
                if (matching.isEmpty() && batch.codes.isNotEmpty()) item { Text("Sin coincidencias para esta búsqueda", color = TextMuted) }
                items(matching, key = { "${it.kind}-${it.ecu}-${it.code}" }) { code ->
                    Card(onClick = { detail = code }, colors = CardDefaults.cardColors(containerColor = if (car) CockpitSurface else CardBackground),
                        shape = RoundedCornerShape(if (car) 28.dp else 12.dp), border = if (car) null else BorderStroke(1.dp, BorderDark)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${code.code} · ECU ${code.ecu}", color = kindColor(code.kind), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(catalog.describe(code.code).text, color = TextWhite)
                            Text("${catalog.describe(code.code).source} · Ver detalle", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (!car || section == "monitors") item {
                DiagnosticCard {
                    Text("Testigo MIL · ${report.monitorStatus.label}", color = TextWhite, fontWeight = FontWeight.Bold)
                    report.monitors.forEach { ecu -> Text("ECU ${ecu.ecu}: ${if (ecu.milOn) "MIL encendido" else "MIL apagado"} · ${ecu.confirmedCount} DTC declarados", color = if (ecu.milOn) WarningAmber else NeonGreen) }
                    TextButton(onClick = { showMonitors = !showMonitors }) { Text(if (showMonitors) "Ocultar monitores" else "Ver monitores desde el último borrado") }
                    if (showMonitors) report.monitors.forEach { ecu ->
                        Text("ECU ${ecu.ecu}", color = TextWhite)
                        if (ecu.monitors.isEmpty()) Text("La ECU no declara monitores compatibles", color = TextMuted)
                        ecu.monitors.forEach { monitor -> Text("${monitor.name}: ${if (monitor.complete) "Completado" else "Pendiente"}", color = if (monitor.complete) NeonGreen else WarningAmber) }
                    }
                }
            }
            if (!car || section == "freeze") item {
                val freeze = report.freezeFrame
                DiagnosticCard {
                    Text("Datos congelados · Cuadro ${freeze.frameNumber}", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(freeze.status.label, color = if (freeze.status == DiagnosticStatus.OK) TextMuted else WarningAmber)
                    if (freeze.status == DiagnosticStatus.OK && freeze.triggeringCodes.isEmpty()) Text("La ECU comunica que no hay cuadro congelado.", color = TextMuted)
                    freeze.triggeringCodes.forEach { (ecu, code) -> Text("ECU $ecu · $code", color = NeonCyan) }
                    TextButton(onClick = { showFreeze = !showFreeze }) { Text(if (showFreeze) "Ocultar datos congelados" else "Ver datos congelados") }
                    if (showFreeze) {
                        freeze.values.forEach { Text("ECU ${it.ecu} · ${it.name}: ${String.format(Locale.getDefault(), "%.1f", it.value)} ${it.unit}", color = TextWhite) }
                        freeze.issues.forEach { Text(it, color = WarningAmber, fontSize = 13.sp) }
                    }
                }
            }
            item { Notice("Los permanentes solo los elimina la ECU tras verificar la reparación. Un código describe una condición detectada; no confirma qué pieza sustituir.", TextMuted) }
        } else if (!state.busy) item {
            DiagnosticCard {
                Text("Prepara la consulta", color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Conecta el ELM327 y enciende el contacto. Pulsa Leer averías. Los resultados se conservan durante esta conexión hasta la siguiente lectura.", color = TextMuted)
                Text("ABS, airbag y otros módulos con protocolos del fabricante requieren diagnóstico específico.", color = TextMuted)
            }
        }
        item { Text("Catálogo sin conexión · Definiciones genéricas. Traducciones en español para códigos habituales; otras definiciones en inglés. Fuente: Wal33D/dtc-database · Licencia MIT.", color = TextDim, fontSize = 12.sp) }
    }
    }
    detail?.let { code ->
        if (car) CarDiagnosticDialog(code.code, { detail = null }, footer = {
            Button({ detail = null }, Modifier.fillMaxWidth()) { Text("Cerrar detalle", fontSize = 24.sp) }
        }) {
            Text("${code.kind.label} · ECU ${code.ecu}", color = kindColor(code.kind), fontSize = 24.sp)
            Text(catalog.describe(code.code).text, color = TextWhite, fontSize = 24.sp)
            Text(code.kind.explanation, color = CockpitMuted, fontSize = 24.sp)
            Text(catalog.describe(code.code).source, color = CockpitMuted, fontSize = 24.sp)
        } else ResponsiveDialog(onDismissRequest = { detail = null }) {
            DiagnosticCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(code.code, color = kindColor(code.kind), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("${code.kind.label} · ECU ${code.ecu}", color = TextMuted)
                Text(catalog.describe(code.code).text, color = TextWhite)
                Text(catalog.describe(code.code).source, color = TextMuted)
                Text(code.kind.explanation, color = TextWhite)
                Text("Anota las condiciones, revisa los datos congelados y consulta el procedimiento del fabricante antes de reparar. La misma condición puede tener varias causas.", color = TextMuted)
                TextButton(onClick = { detail = null }) { Text("Cerrar detalle") }
            }
        }
    }
    confirmation?.let { captured ->
        if (car) CarDiagnosticDialog("Confirmar borrado", { confirmation = null }, footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ confirmation = null }, Modifier.weight(1f)) { Text("Cancelar", fontSize = 24.sp) }
                Button({ confirmation = null; onClear(captured.session) }, Modifier.weight(1f),
                    enabled = engineStopped && connected && !state.busy && captured == report,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text("Confirmar", fontSize = 24.sp) }
            }
        }) {
            Text("Confirmar borrado de averías", color = DangerRed, fontSize = 28.sp)
            Text("Guarda el informe antes. Se borrarán los códigos almacenados y pendientes, los datos congelados y el estado de los monitores. Los permanentes los elimina la ECU tras verificar la reparación.", color = TextWhite, fontSize = 24.sp)
            Text("El borrado no repara la avería. Contacto encendido y motor apagado.", color = CockpitMuted, fontSize = 24.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(engineStopped, { engineStopped = it })
                Text("Confirmo que el motor está apagado y quiero borrar los datos de averías.", Modifier.weight(1f), color = TextWhite, fontSize = 24.sp)
            }
        } else ResponsiveDialog(onDismissRequest = { confirmation = null }) {
            DiagnosticCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Confirmar borrado de averías", color = DangerRed, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Se enviará el modo OBD 04 a las ECU de emisiones. Borra códigos almacenados y pendientes, datos congelados y el estado de los monitores. Los permanentes permanecen hasta que la ECU verifica la reparación.", color = TextWhite)
                Text("Guarda el informe antes de borrar. El borrado no repara la avería. Contacto encendido y motor apagado.", color = TextMuted)
                Row {
                    Checkbox(engineStopped, { engineStopped = it })
                    Text("Confirmo que el motor está apagado y quiero borrar los datos de averías.", color = TextWhite, modifier = Modifier.padding(top = 10.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { confirmation = null }) { Text("Cancelar borrado") }
                    Button(onClick = { confirmation = null; onClear(captured.session) }, enabled = engineStopped && connected && !state.busy && captured == report,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text("Confirmar borrado") }
                }
            }
        }
    }
}

private fun kindColor(kind: DtcKind): Color = when (kind) {
    DtcKind.CONFIRMED -> DangerRed; DtcKind.PENDING -> WarningAmber; DtcKind.PERMANENT -> NeonCyan
}
@Composable
private fun DiagnosticCard(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable ColumnScope.() -> Unit) {
    val car = LocalCarMode.current
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (car) CockpitSurface else CardBackground),
        shape = RoundedCornerShape(if (car) 28.dp else 12.dp), border = if (car) null else BorderStroke(1.dp, BorderDark)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@Composable
private fun Notice(text: String, color: Color) { Text(text, color = color, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }

@Composable
private fun CarDiagnosticDialog(title: String, onDismiss: () -> Unit, footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    ResponsiveDialog(onDismiss) {
        val dialogHeight = maxHeight
        Surface(Modifier.fillMaxWidth().height(dialogHeight), color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = TextWhite, fontSize = 28.sp)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                footer()
            }
        }
    }
}
