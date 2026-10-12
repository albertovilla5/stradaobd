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
    lowerLimit?.let { "${com.torqueobd2.dash.i18n.tr("Inferior", "Lower")}: ${it.limitText()} $units" },
    upperLimit?.let { "${com.torqueobd2.dash.i18n.tr("Superior", "Upper")}: ${it.limitText()} $units" }
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
        item {
            RacingHeader(
                com.torqueobd2.dash.i18n.tr("Avisos configurables", "Configurable alerts"),
                vehicle.name,
                eyebrow = com.torqueobd2.dash.i18n.tr("CONFIGURACIÓN / AVISOS", "SETTINGS / ALERTS")
            )
        }
        item {
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Son avisos de la aplicación con límites elegidos por ti, no testigos del coche. Se vigilan mientras la app está activa, también fuera de esta pantalla. Sin una lectura válida, la vigilancia se interrumpe.",
                    "These are app alerts with limits chosen by you, not vehicle warning lights. They are monitored while the app is active, even outside this screen. Without a valid reading, monitoring pauses."
                ),
                color = TextMuted, fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { creating = true }, enabled = pids.isNotEmpty()) { Text(com.torqueobd2.dash.i18n.tr("Añadir aviso", "Add alert")) }
        }
        if (alerts.isEmpty()) item {
            Text(com.torqueobd2.dash.i18n.tr("Este vehículo aún no tiene avisos. Añade uno y elige sus límites.", "This vehicle has no alerts yet. Add one and set its limits."), color = TextMuted)
        }
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
                Text("${com.torqueobd2.dash.i18n.tr("Persistencia", "Hold time")}: ${alert.rule.durationSeconds} s · ${alert.status.label}", color = color, fontSize = 13.sp)
                Text(alert.detail, color = TextMuted, fontSize = 12.sp)
                alert.reading?.let { reading ->
                    Text("${reading.formatted()} ${reading.units}${if (reading.isSimulated) " · ${com.torqueobd2.dash.i18n.tr("SIMULACIÓN", "SIMULATION")}" else ""}", color = color)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editingId = alert.rule.id }) { Text(com.torqueobd2.dash.i18n.tr("Editar", "Edit")) }
                    TextButton(onClick = { deletingId = alert.rule.id }) { Text(com.torqueobd2.dash.i18n.tr("Eliminar", "Delete"), color = DangerRed) }
                }
                if (pid != null && !pid.isEnabled && alert.rule.enabled) {
                    OutlinedButton(onClick = { onEnableSensor(pid.id) }) { Text(com.torqueobd2.dash.i18n.tr("Activar sensor", "Enable sensor")) }
                }
            }
        }
        item {
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Los avisos se guardan por vehículo. Los sensores vigilados añaden consultas OBD y pueden reducir la velocidad de actualización. La simulación permite probarlos con datos ficticios.",
                    "Alerts are saved per vehicle. Monitored sensors add OBD queries and may reduce the refresh rate. Simulation lets you test them with demo data."
                ),
                color = TextDim, fontSize = 12.sp
            )
        }
    }
    val editing = alerts.firstOrNull { it.rule.id == editingId }?.rule
    if (creating || editing != null) key(vehicle.id, editingId) {
        SensorAlertEditor(vehicle.id, pids, editing, onDismiss = { creating = false; editingId = null },
            onSave = { onSave(it); creating = false; editingId = null })
    }
    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(com.torqueobd2.dash.i18n.tr("Eliminar aviso", "Delete alert")) },
            text = { Text(com.torqueobd2.dash.i18n.tr("Se eliminarán sus límites guardados para este vehículo.", "Its saved limits for this vehicle will be deleted.")) },
            confirmButton = { TextButton(onClick = { onDelete(id); deletingId = null }) { Text(com.torqueobd2.dash.i18n.tr("Eliminar", "Delete")) } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(com.torqueobd2.dash.i18n.tr("Cancelar", "Cancel")) } }
        )
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
    ResponsiveFormDialog(
        title = if (original == null) com.torqueobd2.dash.i18n.tr("Añadir aviso", "Add alert") else com.torqueobd2.dash.i18n.tr("Editar aviso", "Edit alert"),
        confirmLabel = com.torqueobd2.dash.i18n.tr("Guardar aviso", "Save alert"),
        onDismiss = onDismiss,
        onConfirm = {
            val min = number(lower)
            val max = number(upper)
            val seconds = duration.trim().toIntOrNull()
            if (pid == null) error = com.torqueobd2.dash.i18n.tr("Selecciona un sensor disponible en la biblioteca.", "Select an available sensor from the library.")
            else if ((lower.isNotBlank() && min == null) || (upper.isNotBlank() && max == null)) error = com.torqueobd2.dash.i18n.tr("Introduce límites numéricos válidos.", "Enter valid numeric limits.")
            else if (seconds == null) error = com.torqueobd2.dash.i18n.tr("Introduce un número entero de segundos.", "Enter a whole number of seconds.")
            else {
                val rule = SensorAlertRule(id = original?.id ?: java.util.UUID.randomUUID().toString(),
                    vehicleId = vehicleId, pidId = pid.id, sensorName = pid.name, units = pid.units,
                    lowerLimit = min, upperLimit = max, durationSeconds = seconds, enabled = original?.enabled ?: true)
                error = rule.validationError()
                if (error == null) onSave(rule)
            }
        }, validationMessage = error) {
        OutlinedButton(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) {
            Text(pid?.name ?: original?.sensorName ?: com.torqueobd2.dash.i18n.tr("Seleccionar sensor", "Select sensor"))
        }
        pid?.let {
            Text(
                "${it.cleanObdCommand()} · ECU ${it.header.ifBlank { com.torqueobd2.dash.i18n.tr("del perfil", "from profile") }} · ${it.units.ifBlank { com.torqueobd2.dash.i18n.tr("sin unidad", "unitless") }}",
                color = TextMuted, fontSize = 12.sp
            )
            if (!it.isEnabled) Text(
                com.torqueobd2.dash.i18n.tr(
                    "Este sensor está desactivado. Después de guardar, pulsa Activar sensor para vigilarlo.",
                    "This sensor is disabled. After saving, tap Enable sensor to monitor it."
                ),
                color = WarningAmber, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            lower, { lower = it; error = null },
            label = { Text("${com.torqueobd2.dash.i18n.tr("Límite inferior", "Lower limit")}${pid?.units?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}") },
            supportingText = { Text(com.torqueobd2.dash.i18n.tr("Opcional: avisa si el valor es menor.", "Optional: alerts if the value is lower.")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            upper, { upper = it; error = null },
            label = { Text("${com.torqueobd2.dash.i18n.tr("Límite superior", "Upper limit")}${pid?.units?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}") },
            supportingText = { Text(com.torqueobd2.dash.i18n.tr("Opcional: avisa si el valor es mayor.", "Optional: alerts if the value is higher.")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            duration, { duration = it; error = null },
            label = { Text(com.torqueobd2.dash.i18n.tr("Persistencia (segundos)", "Hold time (seconds)")) },
            supportingText = { Text(com.torqueobd2.dash.i18n.tr("De 0 a 3600. Con 0, el aviso es inmediato.", "From 0 to 3600. At 0, the alert triggers immediately.")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            com.torqueobd2.dash.i18n.tr(
                "El aviso termina al volver dentro de los límites. Si cambia el sensor o su unidad, revisa los límites antes de guardar.",
                "The alert clears when the reading returns within limits. If the sensor or its unit changes, review the limits before saving."
            ),
            color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)
        )
    }
    if (choosing) {
        var search by rememberSaveable { mutableStateOf("") }
        ResponsiveDialog(onDismissRequest = { choosing = false }) {
            Surface(Modifier.widthIn(max = 650.dp).fillMaxWidth().heightIn(max = maxHeight), color = CardBackground) {
                Column(Modifier.padding(16.dp)) {
                    Text(com.torqueobd2.dash.i18n.tr("Seleccionar sensor", "Select sensor"), color = TextWhite, fontWeight = FontWeight.Bold)
                    OutlinedTextField(search, { search = it }, label = { Text(com.torqueobd2.dash.i18n.tr("Buscar sensor o PID", "Search sensor or PID")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val matching = pids.filter { "${it.name} ${it.shortName} ${it.cleanObdCommand()} ${it.header}".contains(search.trim(), true) }
                        if (matching.isEmpty()) item { Text(com.torqueobd2.dash.i18n.tr("No hay sensores que coincidan.", "No matching sensors found."), color = TextMuted) }
                        items(matching, key = { it.id }) { sensor ->
                            Column(Modifier.fillMaxWidth().clickable { pidId = sensor.id; choosing = false; error = null }.padding(vertical = 12.dp)) {
                                Text(sensor.name, color = if (sensor.id == pidId) RacingOrange else TextWhite)
                                Text(
                                    "${sensor.cleanObdCommand()} · ECU ${sensor.header.ifBlank { com.torqueobd2.dash.i18n.tr("del perfil", "from profile") }} · ${sensor.units} · ${if (!sensor.isEnabled) com.torqueobd2.dash.i18n.tr("Desactivado", "Disabled") else sensor.supportDescription}",
                                    color = TextMuted, fontSize = 12.sp
                                )
                            }
                        }
                    }
                    TextButton(onClick = { choosing = false }) { Text(com.torqueobd2.dash.i18n.tr("Cerrar", "Close")) }
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
        Icon(Icons.Default.Warning, contentDescription = com.torqueobd2.dash.i18n.tr("Avisos de la aplicación", "App alerts"), tint = WarningAmber)
        Column(Modifier.weight(1f)) {
            Text(
                "${com.torqueobd2.dash.i18n.tr("AVISO DE LA APP", "APP ALERT")}${if (first.reading?.isSimulated == true) " · ${com.torqueobd2.dash.i18n.tr("SIMULACIÓN", "SIMULATION")}" else ""}${if (active.size > 1) " · ${active.size} ${com.torqueobd2.dash.i18n.tr("activos", "active")}" else ""}",
                color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp
            )
            Text(
                "${first.rule.sensorName}: ${first.reading?.formatted()} ${first.rule.units} · ${com.torqueobd2.dash.i18n.tr("Ver detalles", "View details")}",
                color = TextWhite, fontSize = 13.sp
            )
        }
    }
    if (showDetails) ResponsiveDialog(onDismissRequest = { showDetails = false }) {
        Surface(Modifier.widthIn(max = 650.dp).fillMaxWidth().heightIn(max = maxHeight), color = CardBackground) {
            Column(Modifier.padding(16.dp)) {
                Text(com.torqueobd2.dash.i18n.tr("Avisos activos de la aplicación", "Active app alerts"), color = WarningAmber, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    active.forEach { alert ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(alert.rule.sensorName, color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("${alert.reading?.formatted()} ${alert.rule.units}${if (alert.reading?.isSimulated == true) " · ${com.torqueobd2.dash.i18n.tr("SIMULACIÓN", "SIMULATION")}" else ""}", color = WarningAmber)
                            Text(alert.detail, color = TextWhite)
                            Text("${alert.rule.limitsText()} · ${alert.rule.durationSeconds} s", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Text(
                        com.torqueobd2.dash.i18n.tr(
                            "Límites configurados por el usuario. Puedes modificarlos en Ajustes → Avisos.",
                            "User-configured limits. You can modify them in Settings → Alerts."
                        ),
                        color = TextMuted, fontSize = 12.sp
                    )
                }
                TextButton(onClick = { showDetails = false }) { Text(com.torqueobd2.dash.i18n.tr("Cerrar", "Close")) }
            }
        }
    }
}
