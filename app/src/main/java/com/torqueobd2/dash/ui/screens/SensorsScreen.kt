package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.CarDropdownMenuItem as DropdownMenuItem
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.ui.components.ResponsiveFormDialog
import com.torqueobd2.dash.ui.theme.*
import java.io.InputStream

/** Lecturas, selección e importación en una sola pantalla; el catálogo no ocupa la tabla. */
@Composable
fun SensorsScreen(
    vehicleState: VehicleState,
    pids: List<ObdPid>,
    connectionState: ConnectionState,
    onTogglePid: (String, Boolean) -> Unit,
    onImportCsvFromStream: (InputStream) -> Unit,
    onImportCsvFromString: (String) -> Unit,
    onLoadSampleHonda: () -> Unit,
    onLoadStandard: () -> Unit,
    notificationMessage: String?,
    onClearNotification: () -> Unit,
    onScanSensors: () -> Unit,
    isScanningSensors: Boolean,
    onVisibleSensorsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCatalog by remember { mutableStateOf(false) }
    var showPasteDialog by rememberSaveable { mutableStateOf(false) }
    var pastedCsvText by rememberSaveable { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("No se pudo abrir el archivo seleccionado")
                stream.use(onImportCsvFromStream)
            } catch (e: Exception) {
                fileError = "No se pudo leer el CSV: ${e.message}"
            }
        }
    }
    GridMonitorScreen(vehicleState, pids, connectionState, isScanningSensors, onScanSensors,
        onTogglePid, onVisibleSensorsChanged, modifier, catalogAction = {
            Box {
                IconButton(onClick = { showCatalog = true }) {
                    Icon(Icons.Default.LibraryAdd, "Importar / catálogo", Modifier.size(18.dp), tint = NeonCyan)
                }
                DropdownMenu(showCatalog, { showCatalog = false }) {
                    DropdownMenuItem(text = { Text("Abrir CSV") }, onClick = { showCatalog = false; filePicker.launch("*/*") })
                    DropdownMenuItem(text = { Text("Pegar CSV") }, onClick = { showCatalog = false; showPasteDialog = true })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Cargar Honda i-DTEC") }, onClick = { showCatalog = false; onLoadSampleHonda() })
                    DropdownMenuItem(text = { Text("Cargar OBD estándar") }, onClick = { showCatalog = false; onLoadStandard() })
                }
            }
        })
    if (showPasteDialog) {
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        ResponsiveFormDialog(title = "Pegar CSV de Torque Pro", confirmLabel = "Importar PIDs",
            onDismiss = { showPasteDialog = false }, onConfirm = {
                if (pastedCsvText.isNotBlank()) {
                    onImportCsvFromString(pastedCsvText)
                    pastedCsvText = ""
                }
                showPasteDialog = false
            }) {
            if (!keyboardVisible) {
                Text("Pega las líneas del CSV con nombres, ModeAndPID, ecuación, límites y unidades.", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(pastedCsvText, { pastedCsvText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp), minLines = 4, maxLines = 8,
                placeholder = { Text(DefaultPids.SAMPLE_HONDA_IDTEC_CSV.take(160) + "…", color = TextMuted, fontSize = 11.sp) })
        }
    }
    val message = fileError ?: notificationMessage
    if (message != null) {
        val dismiss = { fileError = null; onClearNotification() }
        AlertDialog(onDismissRequest = dismiss, title = { Text("Importación de PIDs") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = dismiss) { Text("Aceptar") } },
            containerColor = CardBackground)
    }
}
