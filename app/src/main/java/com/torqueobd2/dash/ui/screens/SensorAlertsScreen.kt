package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.components.RacingHeader
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.components.ResponsiveFormDialog
import com.torqueobd2.dash.ui.components.racingPanel
import com.torqueobd2.dash.ui.theme.*

private fun Double.limitText() = toString().removeSuffix(".0")

private fun SensorAlertRule.limitsText(): String = listOfNotNull(
    lowerLimit?.let { "Inferior: ${it.limitText()} $units" },
    upperLimit?.let { "Superior: ${it.limitText()} $units" }
).joinToString(" · ")

@Composable
fun SensorAlertsScreen(vehicle: VehicleProfile, pids: List<ObdPid>, alerts: List<SensorAlertState>,
    onSave: (SensorAlertRule) -> Unit, onEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit, onEnableSensor: (String) -> Unit) {
    var editingId by rememberSaveable(vehicle.id) { mutableStateOf<String?>(null) }
    var creating by rememberSaveable(vehicle.id) { mutableStateOf(false) }
    var deletingId by rememberSaveable(vehicle.id) { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(CarbonBlack), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { RacingHeader("Avisos configurables", vehicle.name, eyebrow = "CONFIGURACIÓN / AVISOS") }
        item {
            Text("Son avisos de la aplicación con límites elegidos por ti, no testigos del coche. Se vigilan mientras la app está activa, también fuera de esta pantalla. Sin una lectura válida, la vigilancia se interrumpe.",
                color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { creating = true }, enabled = pids.isNotEmpty()) { Text("Añadir aviso") }
        }
        if (alerts.isEmpty()) item { Text("Este vehículo aún no tiene avisos. Añade uno y elige sus límites.", color = TextMuted) }
        items(alerts, key = { it.rule.id }) { alert ->
            val pid = pids.firstOrNull { it.id == alert.rule.pidId }
            val color = when (alert.status) {
                SensorAlertStatus.ACTIVE -> WarningAmber
                SensorAlertStatus.NORMAL -> NeonGreen
                else -> TextMuted
            }
            Column(Modifier.fillMaxWidth().racingPanel().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pid?.name ?: alert.rule.sensorName, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text(alert.rule.limitsText(), color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(checked = alert.rule.enabled, onCheckedChange = { onEnabled(alert.rule.id, it) })
                }
                Text("Persistencia: ${alert.rule.durationSeconds} s · ${alert.status.label}", color = color, fontSize = 13.sp)
                Text(alert.detail, color = TextMuted, fontSize = 12.sp)
                alert.reading?.let { reading ->
                    Text("${reading.formatted()} ${reading.units}${if (reading.isSimulated) " · SIMULACIÓN" else ""}", color = color)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editingId = alert.rule.id }) { Text("Editar") }
                    TextButton(onClick = { deletingId = alert.rule.id }) { Text("Eliminar", color = DangerRed) }
                }
                if (pid != null && !pid.isEnabled && alert.rule.enabled) {
                    OutlinedButton(onClick = { onEnableSensor(pid.id) }) { Text("Activar sensor") }
                }
            }
        }
        item { Text("Los avisos se guardan por vehículo. Los sensores vigilados añaden consultas OBD y pueden reducir la velocidad de actualización. La simulación permite probarlos con datos ficticios.", color = TextDim, fontSize = 12.sp) }
    }
    val editing = alerts.firstOrNull { it.rule.id == editingId }?.rule
    if (creating || editing != null) key(vehicle.id, editingId) {
        SensorAlertEditor(vehicle.id, pids, editing, onDismiss = { creating = false; editingId = null },
            onSave = { onSave(it); creating = false; editingId = null })
    }
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text("Eliminar aviso") },
            text = { Text("Se eliminarán sus límites guardados para este vehículo.") },
            confirmButton = { TextButton(onClick = { onDelete(id); deletingId = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun SensorAlertEditor(vehicleId: String, pids: List<ObdPid>, original: SensorAlertRule?,
    onDismiss: () -> Unit, onSave: (SensorAlertRule) -> Unit) {
    var pidId by rememberSaveable { mutableStateOf(original?.pidId ?: "") }
    var lower by rememberSaveable { mutableStateOf(original?.lowerLimit?.limitText() ?: "") }
    var upper by rememberSaveable { mutableStateOf(original?.upperLimit?.limitText() ?: "") }
    var duration by rememberSaveable { mutableStateOf((original?.durationSeconds ?: 3).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val pid = pids.firstOrNull { it.id == pidId }
    fun number(text: String) = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    ResponsiveFormDialog(title = if (original == null) "Añadir aviso" else "Editar aviso",
        confirmLabel = "Guardar aviso", onDismiss = onDismiss, onConfirm = {
            val min = number(lower)
            val max = number(upper)
            val seconds = duration.trim().toIntOrNull()
            if (pid == null) error = "Selecciona un sensor disponible en la biblioteca."
            else if ((lower.isNotBlank() && min == null) || (upper.isNotBlank() && max == null)) error = "Introduce límites numéricos válidos."
            else if (seconds == null) error = "Introduce un número entero de segundos."
            else {
                val rule = SensorAlertRule(id = original?.id ?: java.util.UUID.randomUUID().toString(),
                    vehicleId = vehicleId, pidId = pid.id, sensorName = pid.name, units = pid.units,
                    lowerLimit = min, upperLimit = max, durationSeconds = seconds, enabled = original?.enabled ?: true)
                error = rule.validationError()
                if (error == null) onSave(rule)
            }
        }, validationMessage = error) {
        OutlinedButton(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) {
            Text(pid?.name ?: original?.sensorName ?: "Seleccionar sensor")
        }
        pid?.let {
            Text("${it.cleanObdCommand()} · ECU ${it.header.ifBlank { "del perfil" }} · ${it.units.ifBlank { "sin unidad" }}", color = TextMuted, fontSize = 12.sp)
            if (!it.isEnabled) Text("Este sensor está desactivado. Después de guardar, pulsa Activar sensor para vigilarlo.", color = WarningAmber, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(lower, { lower = it; error = null }, label = { Text("Límite inferior${pid?.units?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}") },
            supportingText = { Text("Opcional: avisa si el valor es menor.") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(upper, { upper = it; error = null }, label = { Text("Límite superior${pid?.units?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}") },
            supportingText = { Text("Opcional: avisa si el valor es mayor.") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(duration, { duration = it; error = null }, label = { Text("Persistencia (segundos)") },
            supportingText = { Text("De 0 a 3600. Con 0, el aviso es inmediato.") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Text("El aviso termina al volver dentro de los límites. Si cambia el sensor o su unidad, revisa los límites antes de guardar.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
    if (choosing) {
        var search by rememberSaveable { mutableStateOf("") }
        ResponsiveDialog(onDismissRequest = { choosing = false }) {
            Surface(Modifier.widthIn(max = 650.dp).fillMaxWidth().heightIn(max = maxHeight), color = CardBackground) {
                Column(Modifier.padding(16.dp)) {
                    Text("Seleccionar sensor", color = TextWhite, fontWeight = FontWeight.Bold)
                    OutlinedTextField(search, { search = it }, label = { Text("Buscar sensor o PID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val matching = pids.filter { "${it.name} ${it.shortName} ${it.cleanObdCommand()} ${it.header}".contains(search.trim(), true) }
                        if (matching.isEmpty()) item { Text("No hay sensores que coincidan.", color = TextMuted) }
                        items(matching, key = { it.id }) { sensor ->
                            Column(Modifier.fillMaxWidth().clickable { pidId = sensor.id; choosing = false; error = null }.padding(vertical = 12.dp)) {
                                Text(sensor.name, color = if (sensor.id == pidId) RacingOrange else TextWhite)
                                Text("${sensor.cleanObdCommand()} · ECU ${sensor.header.ifBlank { "del perfil" }} · ${sensor.units} · ${if (!sensor.isEnabled) "Desactivado" else sensor.supportDescription}", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(onClick = { choosing = false }) { Text("Cerrar") }
                }
            }
        }
    }
}

/** Visible también en el cuadro a pantalla completa; no abre diálogos automáticamente. */
@Composable
fun SensorAlertBanner(alerts: List<SensorAlertState>) {
    val active = alerts.filter { it.status == SensorAlertStatus.ACTIVE }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(active.isEmpty()) { if (active.isEmpty()) showDetails = false }
    if (active.isEmpty()) return
    val first = active.first()
    Row(Modifier.fillMaxWidth().background(WarningAmber.copy(alpha = .16f)).clickable { showDetails = true }
        .heightIn(min = if (LocalCarMode.current) 76.dp else 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Warning, contentDescription = "Avisos de la aplicación", tint = WarningAmber)
        Column(Modifier.weight(1f)) {
            Text("AVISO DE LA APP${if (first.reading?.isSimulated == true) " · SIMULACIÓN" else ""}${if (active.size > 1) " · ${active.size} activos" else ""}", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${first.rule.sensorName}: ${first.reading?.formatted()} ${first.rule.units} · Ver detalles", color = TextWhite, fontSize = 13.sp)
        }
    }
    if (showDetails) ResponsiveDialog(onDismissRequest = { showDetails = false }) {
        Surface(Modifier.widthIn(max = 650.dp).fillMaxWidth().heightIn(max = maxHeight), color = CardBackground) {
            Column(Modifier.padding(16.dp)) {
                Text("Avisos activos de la aplicación", color = WarningAmber, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    active.forEach { alert ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(alert.rule.sensorName, color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("${alert.reading?.formatted()} ${alert.rule.units}${if (alert.reading?.isSimulated == true) " · SIMULACIÓN" else ""}", color = WarningAmber)
                            Text(alert.detail, color = TextWhite)
                            Text("${alert.rule.limitsText()} · ${alert.rule.durationSeconds} s", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Text("Límites configurados por el usuario. Puedes modificarlos en Ajustes → Avisos.", color = TextMuted, fontSize = 12.sp)
                }
                TextButton(onClick = { showDetails = false }) { Text("Cerrar") }
            }
        }
    }
}
