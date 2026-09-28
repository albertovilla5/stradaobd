package com.torqueobd2.dash.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.ui.screens.ConnectionScreen
import com.torqueobd2.dash.ui.screens.CustomDashboardScreen
import com.torqueobd2.dash.ui.screens.DigitalHudScreen
import com.torqueobd2.dash.ui.screens.DpfDiagnosticsScreen
import com.torqueobd2.dash.ui.screens.GridMonitorScreen
import com.torqueobd2.dash.ui.screens.PidManagerScreen
import com.torqueobd2.dash.ui.screens.SportDashboardScreen
import com.torqueobd2.dash.ui.screens.TerminalScreen
import com.torqueobd2.dash.ui.screens.VehicleConfigScreen
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.DarkSurface
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber
import com.torqueobd2.dash.viewmodel.ObdDashboardViewModel

data class NavTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(
    viewModel: ObdDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activePids by viewModel.activePids.collectAsState()
    val vehicleProfile by viewModel.vehicleProfile.collectAsState()
    val customDashboard by viewModel.customDashboard.collectAsState()
    val isScanningSensors by viewModel.isScanningSensors.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val notificationMessage by viewModel.csvImportNotification.collectAsState()
    val pairedBtDevices by viewModel.pairedBtDevices.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    val bluetoothErrorHint by viewModel.bluetoothErrorHint.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val tabs = listOf(
        NavTab("Deportivo", Icons.Default.Speed),
        NavTab("A Medida", Icons.Default.DashboardCustomize),
        NavTab("Digital HUD", Icons.Default.DirectionsCar),
        NavTab("DPF / Diésel", Icons.Default.FilterDrama),
        NavTab("Perfil Coche", Icons.Default.Settings),
        NavTab("Monitores", Icons.Default.Dashboard),
        NavTab("PIDs CSV", Icons.Default.CloudDownload),
        NavTab("Conexión", Icons.Default.Build),
        NavTab("Terminal", Icons.Default.Terminal)
    )

    if (isLandscape) {
        // MODO LANDSCAPE (PANTALLA DE COCHE / TABLET APAYSADA): NavigationRail lateral para maximizar altura
        Row(modifier = Modifier.fillMaxSize().background(CarbonBlack)) {
            NavigationRail(
                containerColor = DarkSurface,
                contentColor = RacingOrange,
                modifier = Modifier.fillMaxHeight().width(68.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OBD",
                        color = RacingOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = RacingOrange,
                                unselectedIconColor = TextMuted,
                                indicatorColor = Color(0x33FF6D00)
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                TopStatusBar(
                    connectionType = connectionState.type,
                    connectionStatus = connectionState.status,
                    commandsPerSecond = connectionState.commandsPerSecond,
                    isDpfAlert = vehicleState.dpfWarningLight || vehicleState.dpfActiveRegen,
                    estimatedHp = vehicleState.estimatedHorsepowerCv,
                    rangeKm = vehicleState.estimatedRangeKm
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenContent(
                        tabIndex = selectedTabIndex,
                        viewModel = viewModel,
                        vehicleState = vehicleState,
                        connectionState = connectionState,
                        activePids = activePids,
                        vehicleProfile = vehicleProfile,
                        customDashboard = customDashboard,
                        isScanningSensors = isScanningSensors,
                        terminalLogs = terminalLogs,
                        notificationMessage = notificationMessage,
                        pairedBtDevices = pairedBtDevices,
                        diagnosticLogs = diagnosticLogs,
                        bluetoothErrorHint = bluetoothErrorHint
                    )
                }
            }
        }
    } else {
        // MODO VERTICAL (PORTRAIT): Scaffold estándar con barra inferior desplazable
        Scaffold(
            topBar = {
                TopStatusBar(
                    connectionType = connectionState.type,
                    connectionStatus = connectionState.status,
                    commandsPerSecond = connectionState.commandsPerSecond,
                    isDpfAlert = vehicleState.dpfWarningLight || vehicleState.dpfActiveRegen,
                    estimatedHp = vehicleState.estimatedHorsepowerCv,
                    rangeKm = vehicleState.estimatedRangeKm
                )
            },
            bottomBar = {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DarkSurface,
                    contentColor = RacingOrange,
                    edgePadding = 8.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) RacingOrange else TextMuted
                                )
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (selectedTabIndex == index) RacingOrange else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            },
            containerColor = CarbonBlack
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ScreenContent(
                    tabIndex = selectedTabIndex,
                    viewModel = viewModel,
                    vehicleState = vehicleState,
                    connectionState = connectionState,
                    activePids = activePids,
                    vehicleProfile = vehicleProfile,
                    customDashboard = customDashboard,
                    isScanningSensors = isScanningSensors,
                    terminalLogs = terminalLogs,
                    notificationMessage = notificationMessage,
                    pairedBtDevices = pairedBtDevices,
                    diagnosticLogs = diagnosticLogs,
                    bluetoothErrorHint = bluetoothErrorHint
                )
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
    customDashboard: com.torqueobd2.dash.data.model.CustomDashboardConfig,
    isScanningSensors: Boolean,
    terminalLogs: List<String>,
    notificationMessage: String?,
    pairedBtDevices: List<com.torqueobd2.dash.data.model.DeviceItem>,
    diagnosticLogs: List<String>,
    bluetoothErrorHint: String?
) {
    when (tabIndex) {
        0 -> SportDashboardScreen(
            vehicleState = vehicleState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onThrottleChanged = { viewModel.setManualThrottle(it) }
        )
        1 -> CustomDashboardScreen(
            customDashboard = customDashboard,
            availablePids = activePids,
            vehicleState = vehicleState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onAddWidget = { viewModel.addCustomWidget(it) },
            onRemoveWidget = { viewModel.removeCustomWidget(it) },
            onToggleSize = { viewModel.toggleWidgetSize(it) },
            onResetDefault = { viewModel.resetCustomDashboardToDefault() },
            onThrottleChanged = { viewModel.setManualThrottle(it) }
        )
        2 -> DigitalHudScreen(
            vehicleState = vehicleState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onThrottleChanged = { viewModel.setManualThrottle(it) }
        )
        3 -> DpfDiagnosticsScreen(
            vehicleState = vehicleState,
            isSimulationMode = connectionState.type == ConnectionType.SIMULATION,
            onThrottleChanged = { viewModel.setManualThrottle(it) }
        )
        4 -> VehicleConfigScreen(
            currentProfile = vehicleProfile,
            allPids = activePids,
            isScanningSensors = isScanningSensors,
            onSaveProfile = { viewModel.updateVehicleProfile(it) },
            onScanSensors = { viewModel.scanSupportedSensors() }
        )
        5 -> GridMonitorScreen(vehicleState = vehicleState)
        6 -> PidManagerScreen(
            pids = activePids,
            onTogglePid = { id, en -> viewModel.togglePid(id, en) },
            onImportCsvFromStream = { viewModel.importCsvFromStream(it) },
            onImportCsvFromString = { viewModel.importCsvFromString(it) },
            onLoadSampleHonda = { viewModel.loadSampleHondaIdtecPids() },
            onLoadStandard = { viewModel.loadStandardPids() },
            notificationMessage = notificationMessage,
            onClearNotification = { viewModel.clearNotification() }
        )
        7 -> ConnectionScreen(
            connectionState = connectionState,
            pairedDevices = pairedBtDevices,
            diagnosticLogs = diagnosticLogs,
            bluetoothErrorHint = bluetoothErrorHint,
            onRefreshDevices = { viewModel.refreshPairedBtDevices() },
            onSwitchToSimulation = { viewModel.switchToSimulation() },
            onConnectBluetooth = { viewModel.connectBluetooth(it) },
            onConnectWifi = { host, port -> viewModel.connectWifi(host, port) },
            onDisconnect = { viewModel.disconnect() },
            onClearDiagnosticLogs = { viewModel.clearDiagnosticLogs() }
        )
        8 -> TerminalScreen(
            logs = terminalLogs,
            onSendCommand = { viewModel.sendTerminalCommand(it) }
        )
    }
}

@Composable
private fun TopStatusBar(
    connectionType: ConnectionType,
    connectionStatus: ConnectionStatus,
    commandsPerSecond: Float,
    isDpfAlert: Boolean,
    estimatedHp: Float,
    rangeKm: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TORQUE OBD2",
            color = RacingOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(8.dp))

        val (modeText, modeBg) = when (connectionType) {
            ConnectionType.SIMULATION -> "SIMULADOR" to NeonCyan.copy(alpha = 0.2f)
            ConnectionType.BLUETOOTH -> "BT SPP" to NeonGreen.copy(alpha = 0.2f)
            ConnectionType.WIFI -> "WIFI TCP" to WarningAmber.copy(alpha = 0.2f)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(modeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = modeText,
                color = when (connectionType) {
                    ConnectionType.SIMULATION -> NeonCyan
                    ConnectionType.BLUETOOTH -> NeonGreen
                    ConnectionType.WIFI -> WarningAmber
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (estimatedHp > 5f) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${estimatedHp.toInt()} CV",
                color = TextWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (isDpfAlert) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF8B1A1A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "DPF REGEN",
                    color = TextWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${String.format("%.1f", commandsPerSecond)} Hz",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
