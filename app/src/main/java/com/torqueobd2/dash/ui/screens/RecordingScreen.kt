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
                        ?: error(com.torqueobd2.dash.i18n.tr("No se pudo abrir el archivo", "Could not open file"))
                }
                Toast.makeText(context, com.torqueobd2.dash.i18n.tr("Sesión exportada", "Session exported"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(
                    context,
                    com.torqueobd2.dash.i18n.tr("No se pudo exportar: ${e.message}", "Could not export: ${e.message}"),
                    Toast.LENGTH_LONG
                ).show()
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
                    Text(com.torqueobd2.dash.i18n.tr("Lectura en segundo plano", "Background polling"), color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(
                        com.torqueobd2.dash.i18n.tr(
                            "Consultar los sensores habilitados al cambiar de app o apagar la pantalla.",
                            "Poll enabled sensors when switching apps or turning off the screen."
                        ),
                        color = TextMuted, fontSize = 13.sp
                    )
                }
                Switch(checked = backgroundEnabled, onCheckedChange = { onBackgroundEnabled(it); if (it) requestNotifications(false) })
            }
            Text(
                if (serviceRunning) com.torqueobd2.dash.i18n.tr("Servicio de lectura activo", "Background service active")
                else com.torqueobd2.dash.i18n.tr("Servicio de lectura inactivo", "Background service inactive"),
                color = if (serviceRunning) RacingOrange else TextMuted
            )
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Durante una grabación, la lectura continúa aunque este ajuste esté desactivado. Al detenerla se recupera el modo de lectura elegido en General.",
                    "During recording, polling continues even if this setting is disabled. Stopping restores the polling mode selected in General."
                ),
                color = TextMuted, fontSize = 12.sp
            )
            if (!notificationsEnabled) {
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Las notificaciones están desactivadas. Habilítalas para ver y detener la lectura desde la barra de notificaciones.",
                        "Notifications are disabled. Enable them to view and stop background polling from the notification bar."
                    ),
                    color = RacingOrange, fontSize = 13.sp
                )
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !promptPreferences.getBoolean("notification_prompted", false)) requestNotifications(false)
                    else context.startActivity(if (Build.VERSION.SDK_INT >= 26)
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
                }) { Text(com.torqueobd2.dash.i18n.tr("Habilitar notificaciones", "Enable notifications")) }
            }
        }

    }
    val sessionContent: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (active == null) com.torqueobd2.dash.i18n.tr("Nueva sesión", "New session")
                else com.torqueobd2.dash.i18n.tr("Grabación en curso", "Recording in progress"),
                color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp
            )
            if (active != null) {
                Text(active.context.vehicleName, color = TextWhite)
                Text(
                    "${source(active)} · ${active.sampleCount} ${com.torqueobd2.dash.i18n.tr("muestras", "samples")} · ${size(active.bytes)}",
                    color = RacingOrange
                )
                Text(
                    "${com.torqueobd2.dash.i18n.tr("Inicio", "Started")}: ${date(active.startedAtMillis)}",
                    color = TextMuted, fontSize = 13.sp
                )
                Button(onClick = onStop, enabled = !busy) { Text(com.torqueobd2.dash.i18n.tr("Detener grabación", "Stop recording")) }
            } else {
                if (!car || showDetails) Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Se guardará cada lectura recibida de todos los sensores habilitados, junto con su unidad, fórmula y origen. El CSV también registra los cortes y reconexiones.",
                        "Every reading received from all enabled sensors will be saved along with its unit, equation and source. The CSV also logs disconnects and reconnects."
                    ),
                    color = TextMuted, fontSize = 13.sp
                )
                Button(onClick = { requestNotifications(true) }, enabled = !busy && connection.status == ConnectionStatus.CONNECTED) {
                    Text(
                        if (busy) com.torqueobd2.dash.i18n.tr("Preparando…", "Preparing…")
                        else com.torqueobd2.dash.i18n.tr("Iniciar grabación", "Start recording")
                    )
                }
                if (connection.status != ConnectionStatus.CONNECTED) Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Conecta el adaptador o activa la simulación para grabar.",
                        "Connect the adapter or enable simulation to record."
                    ),
                    color = TextMuted, fontSize = 13.sp
                )
            }
            if (car) TextButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) com.torqueobd2.dash.i18n.tr("Ocultar detalles", "Hide details") else com.torqueobd2.dash.i18n.tr("Ver detalles", "Show details"))
            }
            if (connection.type == ConnectionType.SIMULATION) Text(
                com.torqueobd2.dash.i18n.tr(
                    "SIMULACIÓN · Las muestras se marcarán como datos de prueba.",
                    "SIMULATION · Samples will be marked as test data."
                ),
                color = RacingOrange, fontSize = 13.sp
            )
            (state.error ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            if (!car || showDetails) Text(
                com.torqueobd2.dash.i18n.tr(
                    "Las sesiones se guardan en este dispositivo. Cambiar de vehículo o adaptador cierra la sesión actual. Una grabación interrumpida no se reinicia automáticamente.",
                    "Sessions are saved on this device. Switching vehicle or adapter closes the current session. An interrupted recording does not restart automatically."
                ),
                color = TextMuted, fontSize = 12.sp
            )
        }

    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)) {
        if (!car) item {
            RacingHeader(
                com.torqueobd2.dash.i18n.tr("Grabación de sesiones", "Session recording"),
                com.torqueobd2.dash.i18n.tr("Sensores y lectura en segundo plano", "Sensors and background polling"),
                eyebrow = com.torqueobd2.dash.i18n.tr("CONFIGURACIÓN / GRABACIÓN", "SETTINGS / RECORDING")
            )
        }

        item { if (car) sessionContent() else backgroundContent() }
        item { if (car) backgroundContent() else sessionContent() }
        item {
            Text(
                com.torqueobd2.dash.i18n.tr("Sesiones guardadas (${state.sessions.size})", "Saved sessions (${state.sessions.size})"),
                color = TextWhite, fontWeight = FontWeight.Bold
            )
        }
        if (state.sessions.isEmpty()) item {
            Text(com.torqueobd2.dash.i18n.tr("Todavía no hay sesiones guardadas.", "No saved sessions yet."), color = TextMuted)
        }
        items(state.sessions, key = { it.id }) { session ->
            Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(session.context.vehicleName, color = TextWhite, fontWeight = FontWeight.SemiBold)
                Text("${date(session.startedAtMillis)} · ${session.end?.label.orEmpty()}", color = TextMuted, fontSize = 13.sp)
                Text(
                    "${source(session)} · ${session.sampleCount} ${com.torqueobd2.dash.i18n.tr("muestras", "samples")} · ${size(session.bytes)}",
                    color = RacingOrange, fontSize = 13.sp
                )
                if (session.reason.isNotBlank()) Text(session.reason, color = TextMuted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { exportId = session.id; export.launch("StradaOBD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(session.startedAtMillis))}_${session.id.take(8)}.csv") }) {
                        Text(com.torqueobd2.dash.i18n.tr("Exportar CSV", "Export CSV"))
                    }
                    TextButton(onClick = { deleteId = session.id }) { Text(com.torqueobd2.dash.i18n.tr("Eliminar", "Delete")) }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(com.torqueobd2.dash.i18n.tr("Eliminar sesión", "Delete session")) },
            text = {
                Text(
                    com.torqueobd2.dash.i18n.tr(
                        "Se eliminará la copia guardada en este dispositivo. Los CSV exportados se conservarán.",
                        "The copy saved on this device will be deleted. Exported CSV files will be kept."
                    )
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteId = null }) { Text(com.torqueobd2.dash.i18n.tr("Eliminar", "Delete")) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(com.torqueobd2.dash.i18n.tr("Cancelar", "Cancel")) } }
        )
    }
}

private fun date(millis: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(millis))
private fun source(session: RecordingSession) = if (session.isSimulated) com.torqueobd2.dash.i18n.tr("SIMULACIÓN", "SIMULATION") else "${session.context.connectionType.name} · ${session.context.adapterName}"
private fun size(bytes: Long) = when {
    bytes >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
