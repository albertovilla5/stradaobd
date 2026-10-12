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
                    com.torqueobd2.dash.i18n.tr("Informe guardado", "Report saved")
                } catch (e: Exception) {
                    com.torqueobd2.dash.i18n.tr("No se pudo guardar: ${e.message}", "Could not save: ${e.message}")
                }
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
            Text(com.torqueobd2.dash.i18n.tr("Leer averías", "Read faults"), fontSize = 24.sp)
        }
        if (state.busy) CarIconButton(onCancel) { CarIcon(Icons.Default.Close, com.torqueobd2.dash.i18n.tr("Cancelar consulta", "Cancel query")) }
        CarIconButton({ actions = !actions }) { CarIcon(if (actions) Icons.Default.Close else Icons.Default.MoreHoriz,
            if (actions) com.torqueobd2.dash.i18n.tr("Cerrar opciones de averías", "Close fault options") else com.torqueobd2.dash.i18n.tr("Opciones de averías", "Fault options"), tint = CockpitAccent) }
    }
    if (car && actions) CarMenu(listOf(
        CarMenuEntry(
            com.torqueobd2.dash.i18n.tr("Códigos de error", "Fault codes"),
            if (report == null) com.torqueobd2.dash.i18n.tr("Confirmados, pendientes y permanentes", "Confirmed, pending and permanent")
            else com.torqueobd2.dash.i18n.tr("${report.codes.size} códigos en la última consulta", "${report.codes.size} codes in last query"),
            Icons.Default.CarRepair
        ) { section = "codes"; actions = false },
        CarMenuEntry(com.torqueobd2.dash.i18n.tr("Buscar códigos", "Search codes"), com.torqueobd2.dash.i18n.tr("Filtrar por tipo, descripción o ECU", "Filter by type, description or ECU"), Icons.Default.Search, enabled = report != null) { section = "filter"; actions = false },
        CarMenuEntry(com.torqueobd2.dash.i18n.tr("Informe", "Report"), com.torqueobd2.dash.i18n.tr("Captura, copiar y guardar TXT", "Snapshot, copy and save TXT"), Icons.Default.Description, enabled = report != null) { section = "report"; actions = false },
        CarMenuEntry(com.torqueobd2.dash.i18n.tr("Monitores y testigo", "Monitors & MIL"), com.torqueobd2.dash.i18n.tr("Estado MIL y comprobaciones ECU", "MIL status and ECU readiness"), Icons.Default.Speed, enabled = report != null) { section = "monitors"; showMonitors = true; actions = false },
        CarMenuEntry(com.torqueobd2.dash.i18n.tr("Datos congelados", "Freeze frame"), com.torqueobd2.dash.i18n.tr("Condiciones cuando apareció el fallo", "Conditions when fault occurred"), Icons.Default.AcUnit, enabled = report != null) { section = "freeze"; showFreeze = true; actions = false },
        CarMenuEntry(com.torqueobd2.dash.i18n.tr("Borrar averías", "Clear faults"), com.torqueobd2.dash.i18n.tr("Requiere confirmación; guarda antes el informe", "Requires confirmation; save report first"), Icons.Default.Delete, enabled = connected && !state.busy && report != null) {
            confirmation = report; engineStopped = false; actions = false
        }
    ), Modifier.weight(1f)) else
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!car || report == null) item {
            Text(if (car) com.torqueobd2.dash.i18n.tr("Códigos de error OBD", "OBD Fault Codes") else com.torqueobd2.dash.i18n.tr("Diagnóstico OBD de emisiones", "Emissions OBD Diagnostics"), color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (!car) Text(com.torqueobd2.dash.i18n.tr("Consulta las ECU que responden al OBD estándar. Guarda una captura con códigos, condiciones y respuestas originales.", "Queries ECUs responding to standard OBD. Saves a snapshot with codes, conditions and raw responses."), color = TextMuted)
        }
        if (connection.type == ConnectionType.SIMULATION) item {
            Notice(
                if (car) com.torqueobd2.dash.i18n.tr("SIMULACIÓN · Datos de prueba", "SIMULATION · Test data")
                else com.torqueobd2.dash.i18n.tr("SIMULACIÓN · Averías ficticias para probar el diagnóstico", "SIMULATION · Simulated faults for testing diagnostics"),
                WarningAmber
            )
        }
        item {
            if (!car) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onRead, enabled = connected && !state.busy) {
                    Text(if (report == null) com.torqueobd2.dash.i18n.tr("Leer averías", "Read faults") else com.torqueobd2.dash.i18n.tr("Volver a leer", "Read again"))
                }
                OutlinedButton(onClick = { confirmation = report; engineStopped = false }, enabled = connected && !state.busy && report != null) {
                    Text(com.torqueobd2.dash.i18n.tr("Borrar averías", "Clear faults"))
                }
                if (state.busy) OutlinedButton(onClick = onCancel) { Text(com.torqueobd2.dash.i18n.tr("Cancelar consulta", "Cancel query")) }
            }
            if (!connected) Text(com.torqueobd2.dash.i18n.tr("Conecta el adaptador en Conexión para consultar la ECU.", "Connect the adapter in Connection to query the ECU."), color = TextMuted)
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
                    Text(
                        "${com.torqueobd2.dash.i18n.tr("Última captura", "Last capture")} · ${if (report.complete) com.torqueobd2.dash.i18n.tr("Lectura completa", "Full scan") else com.torqueobd2.dash.i18n.tr("Revisar resultados parciales", "Review partial results")}",
                        color = TextWhite, fontWeight = FontWeight.Bold
                    )
                    Text("${report.vehicleName} · ${report.adapterName}", color = TextMuted)
                    Text(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(report.capturedAt)), color = TextMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val text = report.asText { catalog.describe(it).text }
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(com.torqueobd2.dash.i18n.tr("Informe DTC", "DTC Report"), text))
                            notice = com.torqueobd2.dash.i18n.tr("Informe copiado", "Report copied")
                        }) { Text(com.torqueobd2.dash.i18n.tr("Copiar informe", "Copy report")) }
                        TextButton(onClick = {
                            exportText = report.asText { catalog.describe(it).text }
                            saveReport.launch("${com.torqueobd2.dash.i18n.tr("averias", "faults")}-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(report.capturedAt))}.txt")
                        }) { Text(com.torqueobd2.dash.i18n.tr("Guardar TXT", "Save TXT")) }
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
                    FilterChip(filter == null, { filter = null }, label = { Text(com.torqueobd2.dash.i18n.tr("Todos", "All")) })
                    DtcKind.entries.forEach { kind -> FilterChip(filter == kind, { filter = kind }, label = { Text(kind.label) }) }
                }
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text(com.torqueobd2.dash.i18n.tr("Buscar código, descripción o ECU", "Search code, description or ECU")) }, singleLine = true)
                if (car) Button({ section = "codes" }, Modifier.fillMaxWidth()) { Text(com.torqueobd2.dash.i18n.tr("Ver códigos", "View codes"), fontSize = 24.sp) }
            }
            if (!car || section == "codes") report.batches.filter { filter == null || it.kind == filter }.forEach { batch ->
                item(key = "batch-${batch.kind}") {
                    Text(batch.kind.label, color = kindColor(batch.kind), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!car) Text(batch.kind.explanation, color = TextMuted, fontSize = 13.sp)
                    if (batch.codes.isEmpty()) Text(
                        if (batch.status == DiagnosticStatus.OK) com.torqueobd2.dash.i18n.tr(
                            "Sin códigos en las ECU que respondieron: ${batch.respondingEcus.joinToString()}",
                            "No codes in responding ECUs: ${batch.respondingEcus.joinToString()}"
                        ) else batch.status.label,
                        color = TextWhite
                    )
                    batch.issues.forEach { Text(it, color = WarningAmber, fontSize = 13.sp) }
                }
                val matching = batch.codes.filter { code ->
                    "${code.code} ${code.ecu} ${catalog.describe(code.code).text}".contains(search.trim(), ignoreCase = true)
                }
                if (matching.isEmpty() && batch.codes.isNotEmpty()) item { Text(com.torqueobd2.dash.i18n.tr("Sin coincidencias para esta búsqueda", "No matches for this search"), color = TextMuted) }
                items(matching, key = { "${it.kind}-${it.ecu}-${it.code}" }) { code ->
                    Card(onClick = { detail = code }, colors = CardDefaults.cardColors(containerColor = if (car) CockpitSurface else CardBackground),
                        shape = RoundedCornerShape(if (car) 28.dp else 12.dp), border = if (car) null else BorderStroke(1.dp, BorderDark)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${code.code} · ECU ${code.ecu}", color = kindColor(code.kind), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(catalog.describe(code.code).text, color = TextWhite)
                            Text("${catalog.describe(code.code).source} · ${com.torqueobd2.dash.i18n.tr("Ver detalle", "View details")}", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (!car || section == "monitors") item {
                DiagnosticCard {
                    Text("${com.torqueobd2.dash.i18n.tr("Testigo MIL", "MIL Indicator")} · ${report.monitorStatus.label}", color = TextWhite, fontWeight = FontWeight.Bold)
                    report.monitors.forEach { ecu ->
                        Text(
                            "ECU ${ecu.ecu}: ${if (ecu.milOn) com.torqueobd2.dash.i18n.tr("MIL encendido", "MIL ON") else com.torqueobd2.dash.i18n.tr("MIL apagado", "MIL OFF")} · ${ecu.confirmedCount} ${com.torqueobd2.dash.i18n.tr("DTC declarados", "reported DTCs")}",
                            color = if (ecu.milOn) WarningAmber else NeonGreen
                        )
                    }
                    TextButton(onClick = { showMonitors = !showMonitors }) {
                        Text(if (showMonitors) com.torqueobd2.dash.i18n.tr("Ocultar monitores", "Hide monitors") else com.torqueobd2.dash.i18n.tr("Ver monitores desde el último borrado", "View monitors since last clear"))
                    }
                    if (showMonitors) report.monitors.forEach { ecu ->
                        Text("ECU ${ecu.ecu}", color = TextWhite)
                        if (ecu.monitors.isEmpty()) Text(com.torqueobd2.dash.i18n.tr("La ECU no declara monitores compatibles", "ECU does not report supported monitors"), color = TextMuted)
                        ecu.monitors.forEach { monitor ->
                            Text(
                                "${monitor.name}: ${if (monitor.complete) com.torqueobd2.dash.i18n.tr("Completado", "Complete") else com.torqueobd2.dash.i18n.tr("Pendiente", "Incomplete")}",
                                color = if (monitor.complete) NeonGreen else WarningAmber
                            )
                        }
                    }
                }
            }
            if (!car || section == "freeze") item {
                val freeze = report.freezeFrame
                DiagnosticCard {
                    Text("${com.torqueobd2.dash.i18n.tr("Datos congelados", "Freeze frame")} · ${com.torqueobd2.dash.i18n.tr("Cuadro", "Frame")} ${freeze.frameNumber}", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(freeze.status.label, color = if (freeze.status == DiagnosticStatus.OK) TextMuted else WarningAmber)
                    if (freeze.status == DiagnosticStatus.OK && freeze.triggeringCodes.isEmpty()) Text(com.torqueobd2.dash.i18n.tr("La ECU comunica que no hay cuadro congelado.", "ECU reports no freeze frame stored."), color = TextMuted)
                    freeze.triggeringCodes.forEach { (ecu, code) -> Text("ECU $ecu · $code", color = NeonCyan) }
                    TextButton(onClick = { showFreeze = !showFreeze }) {
                        Text(if (showFreeze) com.torqueobd2.dash.i18n.tr("Ocultar datos congelados", "Hide freeze frame") else com.torqueobd2.dash.i18n.tr("Ver datos congelados", "View freeze frame"))
                    }
                    if (showFreeze) {
                        freeze.values.forEach { Text("ECU ${it.ecu} · ${it.name}: ${String.format(Locale.getDefault(), "%.1f", it.value)} ${it.unit}", color = TextWhite) }
                        freeze.issues.forEach { Text(it, color = WarningAmber, fontSize = 13.sp) }
                    }
                }
            }
            item {
                Notice(
                    com.torqueobd2.dash.i18n.tr(
                        "Los permanentes solo los elimina la ECU tras verificar la reparación. Un código describe una condición detectada; no confirma qué pieza sustituir.",
                        "Permanent codes are only cleared by the ECU after verifying the repair. A code describes a detected condition; it does not confirm which part to replace."
                    ),
                    TextMuted
                )
            }
        } else if (!state.busy) item {
            DiagnosticCard {
                Text(com.torqueobd2.dash.i18n.tr("Prepara la consulta", "Prepare diagnostic scan"), color = TextWhite, fontWeight = FontWeight.Bold)
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Conecta el ELM327 y enciende el contacto. Pulsa Leer averías. Los resultados se conservan durante esta conexión hasta la siguiente lectura.",
                        "Connect the ELM327 and turn on ignition. Tap Read faults. Results are kept during this connection until the next scan."
                    ),
                    color = TextMuted
                )
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "ABS, airbag y otros módulos con protocolos del fabricante requieren diagnóstico específico.",
                        "ABS, airbag and other modules using manufacturer-specific protocols require dedicated diagnostics."
                    ),
                    color = TextMuted
                )
            }
        }
        item {
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Catálogo sin conexión · Definiciones genéricas. Traducciones en español para códigos habituales; otras definiciones en inglés. Fuente: Wal33D/dtc-database · Licencia MIT.",
                    "Offline catalog · Generic definitions. Source: Wal33D/dtc-database · MIT License."
                ),
                color = TextDim, fontSize = 12.sp
            )
        }
    }
    }
    detail?.let { code ->
        if (car) CarDiagnosticDialog(code.code, { detail = null }, footer = {
            Button({ detail = null }, Modifier.fillMaxWidth()) { Text(com.torqueobd2.dash.i18n.tr("Cerrar detalle", "Close details"), fontSize = 24.sp) }
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
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Anota las condiciones, revisa los datos congelados y consulta el procedimiento del fabricante antes de reparar. La misma condición puede tener varias causas.",
                        "Record the conditions, review freeze frame data and consult the manufacturer service procedure before repairing. The same condition may have multiple causes."
                    ),
                    color = TextMuted
                )
                TextButton(onClick = { detail = null }) { Text(com.torqueobd2.dash.i18n.tr("Cerrar detalle", "Close details")) }
            }
        }
    }
    confirmation?.let { captured ->
        if (car) CarDiagnosticDialog(com.torqueobd2.dash.i18n.tr("Confirmar borrado", "Confirm clear"), { confirmation = null }, footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ confirmation = null }, Modifier.weight(1f)) { Text(com.torqueobd2.dash.i18n.tr("Cancelar", "Cancel"), fontSize = 24.sp) }
                Button({ confirmation = null; onClear(captured.session) }, Modifier.weight(1f),
                    enabled = engineStopped && connected && !state.busy && captured == report,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text(com.torqueobd2.dash.i18n.tr("Confirmar", "Confirm"), fontSize = 24.sp) }
            }
        }) {
            Text(com.torqueobd2.dash.i18n.tr("Confirmar borrado de averías", "Confirm clearing fault codes"), color = DangerRed, fontSize = 28.sp)
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Guarda el informe antes. Se borrarán los códigos almacenados y pendientes, los datos congelados y el estado de los monitores. Los permanentes los elimina la ECU tras verificar la reparación.",
                    "Save the report first. Stored and pending codes, freeze frame data and readiness monitors will be cleared. Permanent codes are cleared by the ECU after verifying the repair."
                ),
                color = TextWhite, fontSize = 24.sp
            )
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "El borrado no repara la avería. Contacto encendido y motor apagado.",
                    "Clearing codes does not fix the fault. Ignition ON and engine OFF."
                ),
                color = CockpitMuted, fontSize = 24.sp
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(engineStopped, { engineStopped = it })
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Confirmo que el motor está apagado y quiero borrar los datos de averías.",
                        "I confirm the engine is stopped and I want to clear fault data."
                    ),
                    Modifier.weight(1f), color = TextWhite, fontSize = 24.sp
                )
            }
        } else ResponsiveDialog(onDismissRequest = { confirmation = null }) {
            DiagnosticCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(com.torqueobd2.dash.i18n.tr("Confirmar borrado de averías", "Confirm clearing fault codes"), color = DangerRed, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Se enviará el modo OBD 04 a las ECU de emisiones. Borra códigos almacenados y pendientes, datos congelados y el estado de los monitores. Los permanentes permanecen hasta que la ECU verifica la reparación.",
                        "OBD Mode 04 will be sent to emissions ECUs. This clears stored and pending codes, freeze frame data and readiness monitors. Permanent codes remain until the ECU verifies the repair."
                    ),
                    color = TextWhite
                )
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Guarda el informe antes de borrar. El borrado no repara la avería. Contacto encendido y motor apagado.",
                        "Save the report before clearing. Clearing codes does not fix the fault. Ignition ON and engine OFF."
                    ),
                    color = TextMuted
                )
                Row {
                    Checkbox(engineStopped, { engineStopped = it })
                    Text(
                        com.torqueobd2.dash.i18n.tr(
                            "Confirmo que el motor está apagado y quiero borrar los datos de averías.",
                            "I confirm the engine is stopped and I want to clear fault data."
                        ),
                        color = TextWhite, modifier = Modifier.padding(top = 10.dp)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { confirmation = null }) { Text(com.torqueobd2.dash.i18n.tr("Cancelar borrado", "Cancel")) }
                    Button(onClick = { confirmation = null; onClear(captured.session) }, enabled = engineStopped && connected && !state.busy && captured == report,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text(com.torqueobd2.dash.i18n.tr("Confirmar borrado", "Confirm clear")) }
                }
            }
        }
    }
}

private fun kindColor(kind: DtcKind): Color = when (kind) {
    DtcKind.CONFIRMED -> DangerRed; DtcKind.PENDING -> WarningAmber; DtcKind.PERMANENT -> NeonCyan
}
@Composable
private fun DiagnosticCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val car = LocalCarMode.current
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (car) CockpitSurface else CardBackground),
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
