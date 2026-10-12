package com.torqueobd2.dash.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import com.torqueobd2.dash.ui.automotive.*
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.torqueobd2.dash.util.BluetoothPermissionHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.obd.ObdPollingPolicy
import com.torqueobd2.dash.ui.components.FittedValue
import com.torqueobd2.dash.ui.components.RacingMark
import com.torqueobd2.dash.ui.components.StatusPill
import com.torqueobd2.dash.ui.screens.*
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.viewmodel.ObdDashboardViewModel
import java.util.Locale

data class NavTab(val title: String, val label: String, val icon: ImageVector)

@Composable
fun MainScreen(viewModel: ObdDashboardViewModel, modifier: Modifier = Modifier) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activePids by viewModel.activePids.collectAsState()
    val vehicleProfile by viewModel.vehicleProfile.collectAsState()
    val savedVehicles by viewModel.savedVehicles.collectAsState()
    val customDashboards by viewModel.customDashboards.collectAsState()
    val customDashboard = customDashboards.selectedDashboard.let { if (LocalCarMode.current) it.forCar() else it }
    val isScanningSensors by viewModel.isScanningSensors.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val notificationMessage by viewModel.csvImportNotification.collectAsState()
    val pairedBtDevices by viewModel.pairedBtDevices.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    val bluetoothErrorHint by viewModel.bluetoothErrorHint.collectAsState()
    val sensorAlerts by viewModel.sensorAlerts.collectAsState()
    val dtcState by viewModel.dtcState.collectAsState()


    if (savedVehicles.isEmpty()) {
        Box(modifier.fillMaxSize().background(CockpitBackground).safeDrawingPadding().imePadding()) {
            VehicleProfileWizard(
                initialProfile = null,
                initialSetup = true,
                onCancel = null,
                onSave = viewModel::createOrUpdateVehicle,
                onImportBackup = viewModel::importAppBackup
            )
        }
        return
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current
    var bluetoothStartupHandled by rememberSaveable { mutableStateOf(false) }
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshPairedBtDevices()
    }
    fun enableBluetoothOnStartup() {
        if (!BluetoothPermissionHelper.hasAllPermissions(context) ||
            !BluetoothPermissionHelper.isBluetoothSupported()) return
        if (BluetoothPermissionHelper.enableBluetoothIfAllowed(context)) return
        try {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                com.torqueobd2.dash.i18n.tr(
                    "Activa Bluetooth desde los ajustes del dispositivo.",
                    "Enable Bluetooth in your device settings."
                ),
                Toast.LENGTH_LONG
            ).show()
        } catch (_: SecurityException) {
            Toast.makeText(
                context,
                com.torqueobd2.dash.i18n.tr(
                    "Concede el permiso de Bluetooth para activarlo.",
                    "Grant Bluetooth permission to enable it."
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        enableBluetoothOnStartup()
        viewModel.refreshPairedBtDevices()
    }
    LaunchedEffect(Unit) {
        // No repetir la solicitud al recomponer, recrear la actividad o cerrar el diálogo.
        if (bluetoothStartupHandled) return@LaunchedEffect
        bluetoothStartupHandled = true
        if (!viewModel.shouldPromptBluetoothOnStartup()) return@LaunchedEffect
        if (!BluetoothPermissionHelper.isBluetoothSupported()) return@LaunchedEffect
        if (!BluetoothPermissionHelper.hasAllPermissions(context)) {
            permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions().toTypedArray())
        } else {
            enableBluetoothOnStartup()
        }
    }
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        NavTab(com.torqueobd2.dash.i18n.tr("Deportivo", "Sport"), com.torqueobd2.dash.i18n.tr("Deportivo", "Sport"), Icons.Default.Speed),
        NavTab(com.torqueobd2.dash.i18n.tr("A Medida", "Custom"), com.torqueobd2.dash.i18n.tr("A medida", "Custom"), Icons.Default.DashboardCustomize),
        NavTab(com.torqueobd2.dash.i18n.tr("Vehículo", "Vehicle"), com.torqueobd2.dash.i18n.tr("Vehículo", "Vehicle"), Icons.Default.DirectionsCar),
        NavTab(com.torqueobd2.dash.i18n.tr("Sensores", "Sensors"), com.torqueobd2.dash.i18n.tr("Sensores", "Sensors"), Icons.Default.Dashboard),
        NavTab(com.torqueobd2.dash.i18n.tr("Ajustes", "Settings"), com.torqueobd2.dash.i18n.tr("Ajustes", "Settings"), Icons.Default.Settings),
        NavTab(com.torqueobd2.dash.i18n.tr("Averías", "Faults"), com.torqueobd2.dash.i18n.tr("Averías", "Faults"), Icons.Default.CarRepair)
    )
    var savedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var dashboardFullScreen by rememberSaveable { mutableStateOf(false) }
    var isDashboardEditing by rememberSaveable { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isConsumingWakeGesture by remember { mutableStateOf(false) }

    var selectedSettingsTab by rememberSaveable { mutableIntStateOf(-1) }
    var carMenuVisible by rememberSaveable { mutableStateOf(false) }
    val car = LocalCarMode.current
    val selectedTabIndex = savedTabIndex.coerceIn(tabs.indices)

    val isPanelsTab = (selectedTabIndex == 1) && !carMenuVisible
    val currentIsPanelsTab by rememberUpdatedState(isPanelsTab)
    val currentDashboardFullScreen by rememberUpdatedState(dashboardFullScreen)

    LaunchedEffect(isPanelsTab) {
        if (isPanelsTab) {
            lastInteractionTime = System.currentTimeMillis()
        } else {
            dashboardFullScreen = false
        }
    }

    LaunchedEffect(isPanelsTab, dashboardFullScreen, isDashboardEditing, lastInteractionTime) {
        if (isPanelsTab && !dashboardFullScreen && !isDashboardEditing) {
            val remaining = PanelsFullScreenPolicy.remainingDelayMs(lastInteractionTime, System.currentTimeMillis())
            delay(remaining)
            dashboardFullScreen = true
        }
    }

    BackHandler(enabled = dashboardFullScreen) {
        dashboardFullScreen = false
        lastInteractionTime = System.currentTimeMillis()
    }
    var gridVisibleSensors by remember { mutableStateOf(emptySet<String>()) }
    var vehicleVisibleSensors by remember { mutableStateOf(emptySet<String>()) }
    val visibleSensors = when (if (car && carMenuVisible) -1 else selectedTabIndex) {
        2 -> vehicleVisibleSensors
        3 -> gridVisibleSensors
        else -> ObdPollingPolicy.screenSensors(selectedTabIndex, customDashboard, activePids)
    }
    LaunchedEffect(visibleSensors) { viewModel.setVisibleSensors(visibleSensors) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val verticalNavigationScroll = rememberScrollState()
    val horizontalNavigationScroll = rememberScrollState()
    val density = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val navigationHeight = 38.dp + 14.dp * fontScale
    val navigationWidth = 77.dp * fontScale
    val activeScroll = if (landscape) verticalNavigationScroll else horizontalNavigationScroll
    LaunchedEffect(selectedTabIndex, landscape, fontScale, activeScroll.viewportSize) {
        val step = if (landscape) navigationHeight + 4.dp else navigationWidth + 5.dp
        val itemSize = if (landscape) navigationHeight else navigationWidth
        val start = (selectedTabIndex * step.value * density).toInt()
        val end = start + (itemSize.value * density).toInt()
        if (activeScroll.viewportSize > 0) {
            val target = when {
                start < activeScroll.value -> start
                end > activeScroll.value + activeScroll.viewportSize -> end - activeScroll.viewportSize
                else -> activeScroll.value
            }
            activeScroll.animateScrollTo(target.coerceAtLeast(0))
        }
    }
    val warningLabel: String? = null // No inferir testigos del coche a partir de umbrales genéricos.
    val selectTab: (Int) -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        carMenuVisible = false
        savedTabIndex = it
        dashboardFullScreen = false
        lastInteractionTime = System.currentTimeMillis()
    }
    val screen: @Composable () -> Unit = {
        ScreenContent(
            tabIndex = selectedTabIndex, viewModel = viewModel, vehicleState = vehicleState,
            connectionState = connectionState, activePids = activePids, vehicleProfile = vehicleProfile,
            savedVehicles = savedVehicles, customDashboards = customDashboards,
            isScanningSensors = isScanningSensors, terminalLogs = terminalLogs,
            notificationMessage = notificationMessage, pairedBtDevices = pairedBtDevices,
            diagnosticLogs = diagnosticLogs, bluetoothErrorHint = bluetoothErrorHint,
            selectedSettingsTab = selectedSettingsTab,
            onSettingsTabSelected = { selectedSettingsTab = it },
            onGridSensorsChanged = { gridVisibleSensors = it },
            onVehicleSensorsChanged = { vehicleVisibleSensors = it },
            dashboardFullScreen = dashboardFullScreen,
            onDashboardFullScreenChanged = {
                dashboardFullScreen = it
                lastInteractionTime = System.currentTimeMillis()
            },
            onDashboardEditingChanged = { isDashboardEditing = it }
        )
    }
    val carBack: () -> Unit = {
        if (selectedTabIndex == 4 && selectedSettingsTab >= 0 && !carMenuVisible) selectedSettingsTab = -1
        else if (!carMenuVisible && selectedTabIndex > 1) carMenuVisible = true
        else { carMenuVisible = false; savedTabIndex = 0 }
    }
    BackHandler(enabled = car && !dashboardFullScreen && (carMenuVisible || selectedTabIndex > 1)) { carBack() }
    val openSettings: (Int) -> Unit = { section -> selectedSettingsTab = section; selectTab(4) }

    val isFullScreenActive = dashboardFullScreen && selectedTabIndex == 1
    val view = LocalView.current
    val activity = view.context as? Activity ?: (view.context as? ContextWrapper)?.baseContext as? Activity
    val window = activity?.window

    DisposableEffect(isFullScreenActive, window) {
        if (window != null && isFullScreenActive) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            val prevBehavior = insetsController.systemBarsBehavior
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = prevBehavior
            }
        } else {
            onDispose { }
        }
    }

    val panelsTouchModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (currentIsPanelsTab) {
                    val anyPressed = event.changes.any { it.pressed }
                    val hasDown = event.changes.any { it.changedToDown() }

                    if (currentDashboardFullScreen || isConsumingWakeGesture) {
                        if (hasDown || event.type == PointerEventType.Press) {
                            isConsumingWakeGesture = true
                            dashboardFullScreen = false
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        if (isConsumingWakeGesture) {
                            event.changes.forEach { it.consume() }
                            if (!anyPressed) {
                                isConsumingWakeGesture = false
                            }
                        }
                    } else {
                        if (hasDown || anyPressed) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                } else {
                    isConsumingWakeGesture = false
                }
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(if (car) com.torqueobd2.dash.ui.theme.CockpitBackground else CarbonBlack)
            .then(if (isFullScreenActive) Modifier else Modifier.safeDrawingPadding())
            .imePadding()
            .then(panelsTouchModifier)
    ) {
        SensorAlertBanner(sensorAlerts)
        if (dashboardFullScreen && selectedTabIndex == 1) { Box(Modifier.weight(1f)) { screen() } }
        else if (car) {
            CarShell(title = if (carMenuVisible) com.torqueobd2.dash.i18n.tr("Menú", "Menu") else when (selectedTabIndex) {
                0 -> com.torqueobd2.dash.i18n.tr("Conducción", "Driving")
                1 -> com.torqueobd2.dash.i18n.tr("Mis paneles", "My panels")
                4 -> when (selectedSettingsTab) {
                    0 -> com.torqueobd2.dash.i18n.tr("Preferencias", "Preferences")
                    1 -> com.torqueobd2.dash.i18n.tr("Conexión", "Connection")
                    2 -> com.torqueobd2.dash.i18n.tr("Terminal", "Terminal")
                    3 -> com.torqueobd2.dash.i18n.tr("Avisos", "Alerts")
                    4 -> com.torqueobd2.dash.i18n.tr("Grabación", "Recording")
                    else -> com.torqueobd2.dash.i18n.tr("Ajustes", "Settings")
                }
                else -> tabs[selectedTabIndex].title
            }, vehicle = vehicleProfile.name, connection = connectionState,
                selected = selectedTabIndex, menuVisible = carMenuVisible,
                diagnosticCount = dtcState.report?.codes?.size?.takeIf { it > 0 },
                onDriving = { selectTab(0) }, onPanels = { selectTab(1) },
                onMenu = { carMenuVisible = true; dashboardFullScreen = false }, onDiagnostics = { selectTab(5) }, onBack = { backDispatcher?.onBackPressed() ?: carBack() }, onConnection = { openSettings(1) }) {
                if (carMenuVisible) CarMenu(listOf(
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Vehículo", "Vehicle"), com.torqueobd2.dash.i18n.tr("Garaje y calibración", "Garage & calibration"), Icons.Default.DirectionsCar) { selectTab(2) },
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Sensores", "Sensors"), com.torqueobd2.dash.i18n.tr("Lecturas y catálogo", "Readings & catalog"), Icons.Default.Dashboard) { selectTab(3) },
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Averías OBD", "OBD Faults"), com.torqueobd2.dash.i18n.tr("Leer códigos de error y testigo MIL", "Read fault codes & MIL status"), Icons.Default.CarRepair) { selectTab(5) },
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Conexión", "Connection"), com.torqueobd2.dash.i18n.tr("Adaptador Bluetooth / WiFi", "Bluetooth / WiFi adapter"), Icons.Default.Bluetooth) { openSettings(1) },
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Grabación", "Recording"), com.torqueobd2.dash.i18n.tr("Guardar y exportar viajes", "Save & export trips"), Icons.Default.FiberManualRecord) { openSettings(4) },
                    CarMenuEntry(com.torqueobd2.dash.i18n.tr("Ajustes", "Settings"), com.torqueobd2.dash.i18n.tr("Pantalla, avisos y herramientas", "Display, alerts & tools"), Icons.Default.Settings) { openSettings(-1) }
                )) else screen()
            }
        } else if (landscape) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.width(86.dp * fontScale.coerceAtMost(1.4f)).fillMaxHeight().background(DarkSurface).padding(horizontal = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RacingMark(Modifier.padding(vertical = 10.dp).size(28.dp))
                    Column(Modifier.weight(1f).verticalScroll(verticalNavigationScroll), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            RaceNavigationItem(tab, index == selectedTabIndex, { selectTab(index) }, Modifier.fillMaxWidth().height(navigationHeight))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDark.copy(alpha = 0.5f)))
                Column(Modifier.weight(1f)) {
                    CockpitHeader(tabs[selectedTabIndex].title, vehicleProfile.name, connectionState.type,
                        connectionState.status, connectionState.commandsPerSecond, warningLabel)
                    Box(Modifier.weight(1f)) { screen() }
                }
            }
        } else {
            CockpitHeader(tabs[selectedTabIndex].title, vehicleProfile.name, connectionState.type,
                connectionState.status, connectionState.commandsPerSecond, warningLabel)
            Box(Modifier.weight(1f)) { screen() }
            Row(Modifier.fillMaxWidth().background(DarkSurface).horizontalScroll(horizontalNavigationScroll)
                .padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                tabs.forEachIndexed { index, tab ->
                    RaceNavigationItem(tab, index == selectedTabIndex, { selectTab(index) }, Modifier.width(navigationWidth).height(navigationHeight))
                }
            }
        }
    }
}

@Composable
private fun RaceNavigationItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.heightIn(min = 52.dp).clip(shape)
            .background(if (selected) RacingOrange.copy(alpha = 0.13f) else Color.Transparent)
            .border(1.dp, if (selected) RacingOrange.copy(alpha = 0.35f) else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(tab.icon, contentDescription = tab.title, tint = if (selected) RacingOrange else TextMuted, modifier = Modifier.size(19.dp))
        Text(tab.label, color = if (selected) TextWhite else TextMuted, fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun CockpitHeader(title: String, vehicle: String, type: ConnectionType, status: ConnectionStatus, rate: Float, alert: String?) {
    BoxWithConstraints(Modifier.fillMaxWidth().background(CarbonBlack)) {
        val compact = maxWidth < 460.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FittedValue("STRADA", 13.sp, Modifier.weight(1f, fill = false), color = TextWhite,
                        fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fillWidth = false)
                    Text("OBD", color = RacingOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                    if (!compact) Text("/  $title", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (type == ConnectionType.SIMULATION) com.torqueobd2.dash.i18n.tr("SIMULACIÓN · DATOS FICTICIOS", "SIMULATION · DEMO DATA") else if (compact) title else vehicle, color = TextMuted, fontSize = 10.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
            val label = when (status) {
                ConnectionStatus.CONNECTED -> when (type) {
                    ConnectionType.SIMULATION -> com.torqueobd2.dash.i18n.tr("Simulación", "Simulation")
                    ConnectionType.BLUETOOTH -> "Bluetooth"
                    ConnectionType.WIFI -> "WiFi"
                }
                ConnectionStatus.CONNECTING -> com.torqueobd2.dash.i18n.tr("Conectando", "Connecting")
                ConnectionStatus.INITIALIZING_ELM -> com.torqueobd2.dash.i18n.tr("Inicializando", "Initializing")
                ConnectionStatus.RECONNECTING -> com.torqueobd2.dash.i18n.tr("Reconectando", "Reconnecting")
                ConnectionStatus.DISCONNECTED -> com.torqueobd2.dash.i18n.tr("Sin conexión", "Disconnected")
                ConnectionStatus.ERROR -> com.torqueobd2.dash.i18n.tr("Error de enlace", "Link error")
            }
            val color = when (status) { ConnectionStatus.CONNECTED -> if (type == ConnectionType.SIMULATION) NeonCyan else NeonGreen
                ConnectionStatus.ERROR -> DangerRed; ConnectionStatus.DISCONNECTED -> TextMuted; else -> WarningAmber }
            if (alert != null) {
                if (compact) Icon(Icons.Default.Warning, contentDescription = alert, tint = WarningAmber, modifier = Modifier.size(18.dp))
                else StatusPill(alert, WarningAmber)
            }
            StatusPill(label, color)
            if (!compact) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format(Locale.US, "%.1f/s", rate), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(com.torqueobd2.dash.i18n.tr("CONSULTAS OBD", "OBD QUERIES"), color = TextDim, fontSize = 8.sp, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    tabIndex: Int,
    viewModel: ObdDashboardViewModel,
    vehicleState: com.torqueobd2.dash.data.model.VehicleState,
    connectionState: com.torqueobd2.dash.data.model.ConnectionState,
    activePids: List<com.torqueobd2.dash.data.model.ObdPid>,
    vehicleProfile: com.torqueobd2.dash.data.model.VehicleProfile,
    savedVehicles: List<com.torqueobd2.dash.data.model.VehicleProfile>,
    customDashboards: com.torqueobd2.dash.data.model.CustomDashboardTabs,
    isScanningSensors: Boolean,
    terminalLogs: List<String>,
    notificationMessage: String?,
    pairedBtDevices: List<com.torqueobd2.dash.data.model.DeviceItem>,
    diagnosticLogs: List<String>,
    bluetoothErrorHint: String?,
    selectedSettingsTab: Int,
    onSettingsTabSelected: (Int) -> Unit,
    onGridSensorsChanged: (Set<String>) -> Unit,
    onVehicleSensorsChanged: (Set<String>) -> Unit,
    dashboardFullScreen: Boolean, onDashboardFullScreenChanged: (Boolean) -> Unit,
    onDashboardEditingChanged: (Boolean) -> Unit = {}
) {
    when (tabIndex) {
        5 -> DtcDiagnosticsScreen(
            state = viewModel.dtcState.collectAsState().value,
            connection = connectionState,
            catalog = viewModel.dtcCatalog.collectAsState().value,
            onRead = viewModel::readTroubleCodes,
            onCancel = viewModel::cancelDiagnostics,
            onClear = viewModel::clearTroubleCodes
        )
        0 -> SportDashboardScreen(
            vehicleState = vehicleState,
            availablePids = activePids, connectionState = connectionState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onThrottleChanged = { viewModel.setManualThrottle(it) },
            maxRpm = vehicleProfile.maxRpm
        )
        1 -> CustomDashboardScreen(
            connectionState = connectionState,
            storedDashboards = customDashboards,
            onSelectTab = viewModel::selectDashboardTab,
            onCreateTab = viewModel::createDashboardTab,
            onRenameTab = viewModel::renameDashboardTab,
            onMoveTab = viewModel::moveDashboardTab,
            availablePids = activePids,
            vehicleState = vehicleState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onResetDefault = { viewModel.resetCustomDashboardToDefault() },
            onApplyPreset = viewModel::applyDashboardPreset,
            onClearDashboard = { viewModel.clearCustomDashboard() },
            onSelectGridLayout = { viewModel.setCustomGridLayout(it) },
            onSetWidgetInSlot = { slot, widget -> viewModel.setWidgetInSlot(slot, widget) },
            onClearSlot = { viewModel.clearSlot(it) },
            onThrottleChanged = { viewModel.setManualThrottle(it) },
            onDuplicateTab = viewModel::duplicateDashboardTab,
            onDeleteTab = viewModel::deleteDashboardTab,
            onTransferWidget = viewModel::transferDashboardWidget,
            onDensityChanged = viewModel::setDashboardDensity,
            onImportDesigns = viewModel::importDashboardDesigns,
            onAddSensors = viewModel::addDashboardSensors,
            fullScreen = dashboardFullScreen,
            onFullScreenChanged = onDashboardFullScreenChanged,
            onEditModeChanged = onDashboardEditingChanged
        )
        2 -> VehicleConfigScreen(
            activeProfile = vehicleProfile,
            savedVehicles = savedVehicles,
            allPids = activePids,
            vehicleState = vehicleState,
            connectionState = connectionState,
            isScanningSensors = isScanningSensors,
            onSaveVehicle = { viewModel.createOrUpdateVehicle(it) },
            onSetActiveVehicle = { viewModel.setActiveVehicle(it) },
            onDeleteVehicle = { viewModel.deleteVehicle(it) },
            onScanSensors = { viewModel.scanSupportedSensors() },
            onTogglePid = { id, en -> viewModel.togglePid(id, en) },
            onVisibleSensorsChanged = onVehicleSensorsChanged
        )
        3 -> SensorsScreen(
            vehicleState = vehicleState,
            pids = activePids,
            connectionState = connectionState,
            onTogglePid = { id, enabled -> viewModel.togglePid(id, enabled) },
            onImportCsvFromStream = { viewModel.importCsvFromStream(it) },
            onImportCsvFromString = { viewModel.importCsvFromString(it) },
            onLoadSampleHonda = { viewModel.loadSampleHondaIdtecPids() },
            onLoadStandard = { viewModel.loadStandardPids() },
            notificationMessage = notificationMessage,
            onClearNotification = { viewModel.clearNotification() },
            onScanSensors = { viewModel.scanSupportedSensors() },
            isScanningSensors = isScanningSensors,
            onVisibleSensorsChanged = onGridSensorsChanged,
            onLoadManufacturerPack = viewModel::loadManufacturerPack
        )
        4 -> SettingsScreen(
            selectedTabIndex = selectedSettingsTab,
            onTabSelected = onSettingsTabSelected,
            simulationEnabled = viewModel.simulationEnabled.collectAsState().value,
            onSimulationChanged = viewModel::setSimulationEnabled,
            pollingMode = viewModel.pollingMode.collectAsState().value,
            onPollingModeChanged = viewModel::setPollingMode,
            connectionContent = {
                ConnectionScreen(
                    connectionState = connectionState,
                    pairedDevices = pairedBtDevices,
                    diagnosticLogs = diagnosticLogs,
                    bluetoothErrorHint = bluetoothErrorHint,
                    onRefreshDevices = { viewModel.refreshPairedBtDevices() },
                    isScanningBluetooth = viewModel.isScanningBluetooth.collectAsState().value,
                    onScanBluetooth = { viewModel.scanBluetoothDevices() },
                    onStopBluetoothScan = { viewModel.stopBluetoothDiscovery() },
                    onConnectBluetooth = { viewModel.connectBluetooth(it) },
                    onConnectWifi = { host, port -> viewModel.connectWifi(host, port) },
                    onDisconnect = { viewModel.disconnect() },
                    onClearDiagnosticLogs = { viewModel.clearDiagnosticLogs() },
                    pollingMode = viewModel.pollingMode.collectAsState().value
                )
            },
            terminalContent = {
                TerminalScreen(
                    logs = terminalLogs,
                    onSendCommand = { viewModel.sendTerminalCommand(it) }
                )
            },
            alertsContent = {
                SensorAlertsScreen(vehicleProfile, activePids, viewModel.sensorAlerts.collectAsState().value,
                    onSave = viewModel::saveSensorAlert, onEnabled = viewModel::setSensorAlertEnabled,
                    onDelete = viewModel::deleteSensorAlert,
                    onEnableSensor = { viewModel.togglePid(it, true) })
            },
            recordingContent = {
                com.torqueobd2.dash.ui.screens.RecordingScreen(
                    state = viewModel.recordingState.collectAsState().value,
                    connection = connectionState,
                    busy = viewModel.recordingBusy.collectAsState().value,
                    backgroundEnabled = viewModel.backgroundEnabled.collectAsState().value,
                    serviceRunning = viewModel.backgroundServiceRunning.collectAsState().value,
                    error = viewModel.recordingError.collectAsState().value,
                    onBackgroundEnabled = viewModel::setBackgroundEnabled,
                    onStart = viewModel::startRecording, onStop = viewModel::stopRecording,
                    onDelete = { viewModel.deleteRecording(it) }, onExport = viewModel::exportRecording)
            },
            onExportBackup = viewModel::exportAppBackup,
            onImportBackup = viewModel::importAppBackup
        )
    }
}
