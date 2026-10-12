package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarTab as Tab
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.*
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.PollingMode
import com.torqueobd2.dash.data.repository.AppBackupSnapshot
import com.torqueobd2.dash.ui.components.RacingHeader
import com.torqueobd2.dash.ui.components.racingPanel
import com.torqueobd2.dash.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    simulationEnabled: Boolean,
    onSimulationChanged: (Boolean) -> Unit,
    pollingMode: PollingMode,
    onPollingModeChanged: (PollingMode) -> Unit,
    connectionContent: @Composable () -> Unit,
    terminalContent: @Composable () -> Unit,
    alertsContent: @Composable () -> Unit,
    recordingContent: @Composable () -> Unit,
    onExportBackup: ((OutputStream) -> Unit)? = null,
    onImportBackup: ((InputStream) -> AppBackupSnapshot)? = null
) {
    if (LocalCarMode.current) {
        BackHandler(enabled = selectedTabIndex >= 0) { onTabSelected(-1) }
        when (selectedTabIndex) {
            0 -> GeneralSettingsScreen(
                simulationEnabled = simulationEnabled,
                onSimulationChanged = onSimulationChanged,
                pollingMode = pollingMode,
                onPollingModeChanged = onPollingModeChanged,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup
            )
            1 -> connectionContent()
            2 -> terminalContent()
            3 -> alertsContent()
            4 -> recordingContent()
            else -> CarMenu(listOf(
                CarMenuEntry(
                    com.torqueobd2.dash.i18n.tr("Pantalla y lectura", "Display & polling"),
                    com.torqueobd2.dash.i18n.tr("Modo coche / móvil, idioma y copia de seguridad", "Car / mobile mode, language & backup"),
                    Icons.Default.Tune
                ) { onTabSelected(0) },
                CarMenuEntry(com.torqueobd2.dash.i18n.tr("Conexión", "Connection"), com.torqueobd2.dash.i18n.tr("Bluetooth y WiFi", "Bluetooth & WiFi"), Icons.Default.Bluetooth) { onTabSelected(1) },
                CarMenuEntry(com.torqueobd2.dash.i18n.tr("Avisos", "Alerts"), com.torqueobd2.dash.i18n.tr("Alertas de sensores", "Sensor alerts"), Icons.Default.Notifications) { onTabSelected(3) },
                CarMenuEntry(com.torqueobd2.dash.i18n.tr("Grabación", "Recording"), com.torqueobd2.dash.i18n.tr("Sesiones y archivos CSV", "Sessions & CSV files"), Icons.Default.FiberManualRecord) { onTabSelected(4) },
                CarMenuEntry(com.torqueobd2.dash.i18n.tr("Terminal", "Terminal"), com.torqueobd2.dash.i18n.tr("Comandos del adaptador", "Adapter commands"), Icons.Default.Terminal) { onTabSelected(2) }
            ))
        }
        return
    }
    val tabs = listOf(
        com.torqueobd2.dash.i18n.tr("General", "General"),
        com.torqueobd2.dash.i18n.tr("Conexión", "Connection"),
        com.torqueobd2.dash.i18n.tr("Terminal", "Terminal"),
        com.torqueobd2.dash.i18n.tr("Avisos", "Alerts"),
        com.torqueobd2.dash.i18n.tr("Grabación", "Recording")
    )
    val selectedTab = selectedTabIndex.coerceIn(tabs.indices)
    val stateHolder = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize().background(CarbonBlack)) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            containerColor = DarkSurface,
            contentColor = RacingOrange,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = RacingOrange,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    selectedContentColor = RacingOrange,
                    unselectedContentColor = TextMuted,
                    text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            stateHolder.SaveableStateProvider(selectedTab) {
                when (selectedTab) {
                    0 -> GeneralSettingsScreen(
                        simulationEnabled = simulationEnabled,
                        onSimulationChanged = onSimulationChanged,
                        pollingMode = pollingMode,
                        onPollingModeChanged = onPollingModeChanged,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup
                    )
                    1 -> connectionContent()
                    2 -> terminalContent()
                    3 -> alertsContent()
                    4 -> recordingContent()
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsScreen(
    simulationEnabled: Boolean,
    onSimulationChanged: (Boolean) -> Unit,
    pollingMode: PollingMode,
    onPollingModeChanged: (PollingMode) -> Unit,
    onExportBackup: ((OutputStream) -> Unit)? = null,
    onImportBackup: ((InputStream) -> AppBackupSnapshot)? = null
) {
    Column(Modifier.fillMaxSize().background(CarbonBlack).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!LocalCarMode.current) RacingHeader(
            com.torqueobd2.dash.i18n.tr("Preferencias", "Preferences"),
            com.torqueobd2.dash.i18n.tr("Idioma, copia de seguridad, simulación y lectura de sensores", "Language, backup, simulation and sensor polling"),
            eyebrow = com.torqueobd2.dash.i18n.tr("CONFIGURACIÓN / GENERAL", "SETTINGS / GENERAL")
        )
        InterfaceModeSettings()
        LanguageSettings()
        if (onExportBackup != null && onImportBackup != null) {
            BackupSettingsPanel(onExportBackup = onExportBackup, onImportBackup = onImportBackup)
        }
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(com.torqueobd2.dash.i18n.tr("Lectura de sensores", "Sensor polling"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PollingMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().selectable(selected = pollingMode == mode,
                    role = Role.RadioButton, onClick = { onPollingModeChanged(mode) }).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(selected = pollingMode == mode, onClick = null)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(mode.title, color = if (pollingMode == mode) RacingOrange else TextWhite, fontWeight = FontWeight.SemiBold)
                        Text(mode.description, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Al cambiar de pantalla se actualiza la selección. Los sensores con avisos activados se consultan en todos los modos aunque no estén visibles, siempre que estén habilitados en Sensores. El escaneo completo de la ECU sigue disponible en Vehículo.",
                    "Selection updates when switching screens. Sensors with active alerts are polled in all modes even when not visible, as long as they are enabled in Sensors. Full ECU scanning remains available in Vehicle."
                ),
                color = TextMuted, fontSize = 12.sp
            )
        }
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(com.torqueobd2.dash.i18n.tr("Modo simulación", "Simulation mode"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(com.torqueobd2.dash.i18n.tr("Datos de prueba para usar los indicadores sin el coche.", "Demo data to test gauges without connecting to a vehicle."), color = TextMuted, fontSize = 13.sp)
                }
                Switch(checked = simulationEnabled, onCheckedChange = onSimulationChanged)
            }
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "Desactivado por defecto. Al activarlo se desconecta el OBD real. Al desactivarlo se vuelve a intentar conectar al último adaptador guardado.",
                    "Disabled by default. Enabling disconnects real OBD. Disabling retries connecting to the last saved adapter."
                ),
                color = TextMuted, fontSize = 12.sp
            )
        }
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(com.torqueobd2.dash.i18n.tr("Conexión automática", "Auto-reconnect"), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                com.torqueobd2.dash.i18n.tr(
                    "La app recuerda el último adaptador Bluetooth o WiFi conectado y vuelve a intentarlo mientras no esté disponible. Puedes elegir otro desde la pestaña Conexión. Desconectar detiene los intentos hasta que selecciones un adaptador o vuelvas a abrir la app.",
                    "The app remembers the last connected Bluetooth or WiFi adapter and retries while unavailable. You can pick another in the Connection tab. Disconnecting stops retries until you select an adapter or reopen the app."
                ),
                color = TextMuted, fontSize = 13.sp
            )
        }
        PrivacyAndContactSettingsPanel()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyAndContactSettingsPanel() {
    val context = LocalContext.current
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().racingPanel().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            com.torqueobd2.dash.i18n.tr("Privacidad y contacto del desarrollador", "Privacy & developer contact"),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            com.torqueobd2.dash.i18n.tr(
                "Desarrollador: ${AppPrivacyPolicy.DEVELOPER_NAME} · Contacto: ${AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL}",
                "Developer: ${AppPrivacyPolicy.DEVELOPER_NAME} · Contact: ${AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL}"
            ),
            color = TextMuted,
            fontSize = 13.sp
        )
        Text(
            com.torqueobd2.dash.i18n.tr(
                "URL de Política de Privacidad (HTTPS): ${AppPrivacyPolicy.PRIVACY_POLICY_URL}",
                "Privacy Policy URL (HTTPS): ${AppPrivacyPolicy.PRIVACY_POLICY_URL}"
            ),
            color = TextMuted,
            fontSize = 12.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = { showPrivacyDialog = true }) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
                Text(
                    com.torqueobd2.dash.i18n.tr("Ver Política de Privacidad", "View Privacy Policy"),
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = {
                    AppPrivacyPolicy.openExternalUrlOrCopy(
                        context,
                        AppPrivacyPolicy.PRIVACY_POLICY_URL,
                        com.torqueobd2.dash.i18n.tr("Política de Privacidad Web", "Web Privacy Policy")
                    )
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = RacingOrange)
                Spacer(Modifier.width(8.dp))
                Text(
                    com.torqueobd2.dash.i18n.tr("Abrir Política Web (HTTPS)", "Open Web Policy (HTTPS)"),
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupSettingsPanel(
    onExportBackup: (OutputStream) -> Unit,
    onImportBackup: (InputStream) -> AppBackupSnapshot
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onCarModeChanged = LocalOnCarModeChanged.current
    val onLanguagePreferenceChanged = com.torqueobd2.dash.i18n.LocalOnAppLanguagePreferenceChanged.current
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use(onExportBackup)
                            ?: error(com.torqueobd2.dash.i18n.tr("No se pudo abrir el archivo de destino.", "Could not open destination file."))
                    }
                    val msg = com.torqueobd2.dash.i18n.tr(
                        "Copia de seguridad exportada correctamente.",
                        "Backup exported successfully."
                    )
                    statusMessage = msg
                    isError = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val msg = com.torqueobd2.dash.i18n.tr(
                        "No se pudo exportar la copia: ${e.message}",
                        "Could not export backup: ${e.message}"
                    )
                    statusMessage = msg
                    isError = true
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val snapshot = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use(onImportBackup)
                            ?: error(com.torqueobd2.dash.i18n.tr("No se pudo leer el archivo seleccionado.", "Could not read selected file."))
                    }
                    onCarModeChanged(snapshot.carMode)
                    onLanguagePreferenceChanged(snapshot.languagePreference)
                    val msg = com.torqueobd2.dash.i18n.tr(
                        "Copia restaurada: ${snapshot.vehicles.size} vehículo(s), ${snapshot.dashboardTabs.dashboards.size} pantalla(s) y ${snapshot.alertRules.size} aviso(s).",
                        "Backup restored: ${snapshot.vehicles.size} vehicle(s), ${snapshot.dashboardTabs.dashboards.size} dashboard(s) and ${snapshot.alertRules.size} alert(s)."
                    )
                    statusMessage = msg
                    isError = false
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val msg = com.torqueobd2.dash.i18n.tr(
                        "Error al restaurar copia: ${e.message}",
                        "Failed to restore backup: ${e.message}"
                    )
                    statusMessage = msg
                    isError = true
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().racingPanel().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            com.torqueobd2.dash.i18n.tr("Copia de seguridad y restauración", "Backup & restore"),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            com.torqueobd2.dash.i18n.tr(
                "Android respalda automáticamente tus vehículos, pantallas a medida, sensores, avisos e idioma en Google Drive. También puedes exportar o importar un archivo JSON para transferir tu configuración a una radio Android de coche sin cuenta de Google.",
                "Android automatically backs up your vehicles, custom dashboards, sensors, alerts and language to Google Drive. You can also export or import a JSON file to transfer your setup to an Android car head unit without a Google account."
            ),
            color = TextMuted,
            fontSize = 13.sp
        )
        Text(
            com.torqueobd2.dash.i18n.tr(
                "Por privacidad y compatibilidad entre dispositivos, las direcciones MAC Bluetooth, IP WiFi, modo simulación y grabaciones CSV no se incluyen en la copia.",
                "For privacy and cross-device compatibility, Bluetooth MAC addresses, WiFi IPs, simulation mode and CSV recordings are excluded from backups."
            ),
            color = TextMuted,
            fontSize = 12.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    try {
                        exportLauncher.launch("stradaobd_backup_$timestamp.json")
                    } catch (_: ActivityNotFoundException) {
                        val msg = com.torqueobd2.dash.i18n.tr(
                            "Este dispositivo no dispone de selector de archivos para guardar documentos.",
                            "This device does not have a file picker to save documents."
                        )
                        statusMessage = msg
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = CarbonBlack)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    com.torqueobd2.dash.i18n.tr("Exportar copia (.json)", "Export backup (.json)"),
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = {
                    try {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                    } catch (_: ActivityNotFoundException) {
                        val msg = com.torqueobd2.dash.i18n.tr(
                            "Este dispositivo no dispone de selector de archivos para abrir documentos.",
                            "This device does not have a file picker to open documents."
                        )
                        statusMessage = msg
                        isError = true
                    }
                }
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
                Text(
                    com.torqueobd2.dash.i18n.tr("Importar copia (.json)", "Import backup (.json)"),
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        statusMessage?.let { msg ->
            Text(
                text = msg,
                color = if (isError) DangerRed else NeonGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InterfaceModeSettings() {
    val car = LocalCarMode.current
    val onChange = LocalOnCarModeChanged.current
    Column(Modifier.fillMaxWidth().racingPanel().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(com.torqueobd2.dash.i18n.tr("Presentación de la pantalla", "Display mode"), color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(com.torqueobd2.dash.i18n.tr("Elige el tamaño y la navegación para tu dispositivo.", "Choose the layout size and navigation style for your device."), color = TextMuted)
        listOf(
            true to com.torqueobd2.dash.i18n.tr("Modo coche", "Car mode"),
            false to com.torqueobd2.dash.i18n.tr("Modo móvil", "Mobile mode")
        ).forEach { (value, title) ->
            Row(Modifier.fillMaxWidth().heightIn(min = if (car) 76.dp else 48.dp)
                .selectable(selected = car == value, role = Role.RadioButton, onClick = { onChange(value) })
                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadioButton(selected = car == value, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(title, color = if (car == value) RacingOrange else TextWhite, fontWeight = FontWeight.Medium)
                    Text(
                        if (value) com.torqueobd2.dash.i18n.tr("Botones grandes y accesos directos", "Large touch targets and quick shortcuts")
                        else com.torqueobd2.dash.i18n.tr("Interfaz compacta para teléfono", "Compact interface for phones"),
                        color = TextMuted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSettings() {
    val car = LocalCarMode.current
    val currentLanguage = com.torqueobd2.dash.i18n.LocalAppLanguage.current
    val currentPreference = com.torqueobd2.dash.i18n.LocalAppLanguagePreference.current
    val onPreferenceChanged = com.torqueobd2.dash.i18n.LocalOnAppLanguagePreferenceChanged.current
    val systemLanguageName = com.torqueobd2.dash.i18n.AppLanguagePreference.SYSTEM
        .resolve(com.torqueobd2.dash.i18n.AppI18n.getSystemLocale()).nativeName
    Column(Modifier.fillMaxWidth().racingPanel().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(com.torqueobd2.dash.i18n.tr("Idioma", "Language"), color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(
            com.torqueobd2.dash.i18n.tr(
                "Sigue el idioma del sistema por defecto o selecciona un idioma para la aplicación.",
                "Follows your device language by default or lets you select a specific app language."
            ),
            color = TextMuted
        )
        com.torqueobd2.dash.i18n.AppLanguagePreference.entries.forEach { pref ->
            val selected = currentPreference == pref
            val subtitle = when (pref) {
                com.torqueobd2.dash.i18n.AppLanguagePreference.SYSTEM -> com.torqueobd2.dash.i18n.tr(
                    "Usa el idioma del dispositivo ($systemLanguageName)",
                    "Uses your device language ($systemLanguageName)"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.ES -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en español",
                    "Interface, sensors and diagnostics in Spanish"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.EN -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en inglés",
                    "Interface, sensors and diagnostics in English"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.FR -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en francés",
                    "Interface, sensors and diagnostics in French"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.DE -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en alemán",
                    "Interface, sensors and diagnostics in German"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.PT -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en portugués",
                    "Interface, sensors and diagnostics in Portuguese"
                )
                com.torqueobd2.dash.i18n.AppLanguagePreference.IT -> com.torqueobd2.dash.i18n.tr(
                    "Interfaz, sensores y diagnósticos en italiano",
                    "Interface, sensors and diagnostics in Italian"
                )
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (car) 76.dp else 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPreferenceChanged(pref) })
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioButton(selected = selected, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(pref.displayName(currentLanguage), color = if (selected) RacingOrange else TextWhite, fontWeight = FontWeight.Medium)
                    Text(subtitle, color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
