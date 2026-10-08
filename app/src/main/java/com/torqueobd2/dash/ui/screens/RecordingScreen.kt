package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.components.RacingHeader
import com.torqueobd2.dash.ui.components.racingPanel
import com.torqueobd2.dash.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingScreen(
    state: RecordingState, connection: ConnectionState, busy: Boolean,
    backgroundEnabled: Boolean, serviceRunning: Boolean, error: String?,
    onBackgroundEnabled: (Boolean) -> Unit, onStart: () -> Unit, onStop: () -> Unit,
    onDelete: (String) -> Unit, onExport: suspend (String, java.io.OutputStream) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var startAfterPermission by rememberSaveable { mutableStateOf(false) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val promptPreferences = remember { context.getSharedPreferences("recording_ui", android.content.Context.MODE_PRIVATE) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (startAfterPermission) { startAfterPermission = false; onStart() }
    }
    fun requestNotifications(start: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !promptPreferences.getBoolean("notification_prompted", false)) {
            startAfterPermission = start
            promptPreferences.edit().putBoolean("notification_prompted", true).apply()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (start) onStart()
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val id = exportId
        exportId = null
        if (uri != null && id != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { onExport(id, it) }
                        ?: error("No se pudo abrir el archivo")
                }
                Toast.makeText(context, "Sesión exportada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(context, "No se pudo exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val car = LocalCarMode.current
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val active = state.active
    val backgroundContent: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Lectura en segundo plano", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("Consultar los sensores habilitados al cambiar de app o apagar la pantalla.", color = TextMuted, fontSize = 13.sp)
                }
                Switch(checked = backgroundEnabled, onCheckedChange = { onBackgroundEnabled(it); if (it) requestNotifications(false) })
            }
            Text(if (serviceRunning) "Servicio de lectura activo" else "Servicio de lectura inactivo", color = if (serviceRunning) RacingOrange else TextMuted)
            Text("Durante una grabación, la lectura continúa aunque este ajuste esté desactivado. Al detenerla se recupera el modo de lectura elegido en General.", color = TextMuted, fontSize = 12.sp)
            if (!notificationsEnabled) {
                Text("Las notificaciones están desactivadas. Habilítalas para ver y detener la lectura desde la barra de notificaciones.", color = RacingOrange, fontSize = 13.sp)
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !promptPreferences.getBoolean("notification_prompted", false)) requestNotifications(false)
                    else context.startActivity(if (Build.VERSION.SDK_INT >= 26)
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
                }) { Text("Habilitar notificaciones") }
            }
        }

    }
    val sessionContent: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (active == null) "Nueva sesión" else "Grabación en curso", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (active != null) {
                Text(active.context.vehicleName, color = TextWhite)
                Text("${source(active)} · ${active.sampleCount} muestras · ${size(active.bytes)}", color = RacingOrange)
                Text("Inicio: ${date(active.startedAtMillis)}", color = TextMuted, fontSize = 13.sp)
                Button(onClick = onStop, enabled = !busy) { Text("Detener grabación") }
            } else {
                if (!car || showDetails) Text("Se guardará cada lectura recibida de todos los sensores habilitados, junto con su unidad, fórmula y origen. El CSV también registra los cortes y reconexiones.", color = TextMuted, fontSize = 13.sp)
                Button(onClick = { requestNotifications(true) }, enabled = !busy && connection.status == ConnectionStatus.CONNECTED) {
                    Text(if (busy) "Preparando…" else "Iniciar grabación")
                }
                if (connection.status != ConnectionStatus.CONNECTED) Text("Conecta el adaptador o activa la simulación para grabar.", color = TextMuted, fontSize = 13.sp)
            }
            if (car) TextButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "Ocultar detalles" else "Ver detalles") }
            if (connection.type == ConnectionType.SIMULATION) Text("SIMULACIÓN · Las muestras se marcarán como datos de prueba.", color = RacingOrange, fontSize = 13.sp)
            (state.error ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            if (!car || showDetails) Text("Las sesiones se guardan en este dispositivo. Cambiar de vehículo o adaptador cierra la sesión actual. Una grabación interrumpida no se reinicia automáticamente.", color = TextMuted, fontSize = 12.sp)
        }

    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)) {
        if (!car) item { RacingHeader("Grabación de sesiones", "Sensores y lectura en segundo plano", eyebrow = "CONFIGURACIÓN / GRABACIÓN") }

        item { if (car) sessionContent() else backgroundContent() }
        item { if (car) backgroundContent() else sessionContent() }
        item { Text("Sesiones guardadas (${state.sessions.size})", color = TextWhite, fontWeight = FontWeight.Bold) }
        if (state.sessions.isEmpty()) item { Text("Todavía no hay sesiones guardadas.", color = TextMuted) }
        items(state.sessions, key = { it.id }) { session ->
            Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(session.context.vehicleName, color = TextWhite, fontWeight = FontWeight.SemiBold)
                Text("${date(session.startedAtMillis)} · ${session.end?.label.orEmpty()}", color = TextMuted, fontSize = 13.sp)
                Text("${source(session)} · ${session.sampleCount} muestras · ${size(session.bytes)}", color = RacingOrange, fontSize = 13.sp)
                if (session.reason.isNotBlank()) Text(session.reason, color = TextMuted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { exportId = session.id; export.launch("Torque_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(session.startedAtMillis))}_${session.id.take(8)}.csv") }) { Text("Exportar CSV") }
                    TextButton(onClick = { deleteId = session.id }) { Text("Eliminar") }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Eliminar sesión") },
            text = { Text("Se eliminará la copia guardada en este dispositivo. Los CSV exportados se conservarán.") },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteId = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } })
    }
}

private fun date(millis: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(millis))
private fun source(session: RecordingSession) = if (session.isSimulated) "SIMULACIÓN" else "${session.context.connectionType.name} · ${session.context.adapterName}"
private fun size(bytes: Long) = when {
    bytes >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
