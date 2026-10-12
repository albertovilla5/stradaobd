package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.CarDropdownMenuItem as DropdownMenuItem
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.tr
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
    modifier: Modifier = Modifier,
    onLoadManufacturerPack: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showCatalog by remember { mutableStateOf(false) }
    var showPasteDialog by rememberSaveable { mutableStateOf(false) }
    var showManufacturerDialog by rememberSaveable { mutableStateOf(false) }
    var pastedCsvText by rememberSaveable { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error(tr("No se pudo abrir el archivo seleccionado", "Could not open the selected file"))
                stream.use(onImportCsvFromStream)
            } catch (e: Exception) {
                fileError = tr("No se pudo leer el CSV: ${e.message}", "Could not read CSV: ${e.message}")
            }
        }
    }
    GridMonitorScreen(vehicleState, pids, connectionState, isScanningSensors, onScanSensors,
        onTogglePid, onVisibleSensorsChanged, modifier, catalogAction = {
            Box {
                IconButton(onClick = { showCatalog = true }) {
                    Icon(Icons.Default.LibraryAdd, tr("Importar / catálogo", "Import / catalog"), Modifier.size(18.dp), tint = NeonCyan)
                }
                DropdownMenu(showCatalog, { showCatalog = false }) {
                    DropdownMenuItem(text = { Text(tr("Abrir CSV", "Open CSV")) }, onClick = { showCatalog = false; filePicker.launch("*/*") })
                    DropdownMenuItem(text = { Text(tr("Pegar CSV", "Paste CSV")) }, onClick = { showCatalog = false; showPasteDialog = true })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(tr("Conjuntos de fabricante…", "Manufacturer preset packs…")) }, onClick = { showCatalog = false; showManufacturerDialog = true })
                    DropdownMenuItem(text = { Text(tr("Cargar Honda i-DTEC", "Load Honda i-DTEC")) }, onClick = { showCatalog = false; onLoadSampleHonda() })
                    DropdownMenuItem(text = { Text(tr("Cargar OBD estándar", "Load standard OBD")) }, onClick = { showCatalog = false; onLoadStandard() })
                }
            }
        })
    if (showManufacturerDialog) {
        val packs = remember(AppI18n.currentLanguage) { DefaultPids.getManufacturerPacks() }
        ResponsiveFormDialog(
            title = tr("Conjuntos de PIDs por fabricante", "Manufacturer PID preset packs"),
            confirmLabel = tr("Cerrar", "Close"),
            onDismiss = { showManufacturerDialog = false },
            onConfirm = { showManufacturerDialog = false }
        ) {
            Text(
                tr(
                    "Selecciona un paquete de PIDs extendidos de fabricante (Modo 21/22) para añadirlo a tu catálogo de sensores.",
                    "Select a manufacturer extended PID pack (Mode 21/22) to add to your sensor catalog."
                ),
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                packs.forEach { pack ->
                    val shape = RoundedCornerShape(10.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(DarkSurface)
                            .border(1.dp, BorderDark, shape)
                            .clickable {
                                if (pack.id == "honda_idtec") onLoadSampleHonda() else onLoadManufacturerPack(pack.id)
                                showManufacturerDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pack.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(pack.subtitle, color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${pack.pids.size} PIDs", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (showPasteDialog) {
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        ResponsiveFormDialog(title = tr("Pegar CSV de PIDs extendidos", "Paste extended PID CSV"), confirmLabel = tr("Importar PIDs", "Import PIDs"),
            onDismiss = { showPasteDialog = false }, onConfirm = {
                if (pastedCsvText.isNotBlank()) {
                    onImportCsvFromString(pastedCsvText)
                    pastedCsvText = ""
                }
                showPasteDialog = false
            }) {
            if (!keyboardVisible) {
                Text(tr("Pega las líneas del CSV con nombres, ModeAndPID, ecuación, límites y unidades.", "Paste CSV lines with names, ModeAndPID, equation, limits and units."), color = TextMuted, fontSize = 12.sp)
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
        AlertDialog(onDismissRequest = dismiss, title = { Text(tr("Importación de PIDs", "PID Import")) },
            text = { Text(message) }, confirmButton = { TextButton(onClick = dismiss) { Text(tr("Aceptar", "OK")) } },
            containerColor = CardBackground)
    }
}
