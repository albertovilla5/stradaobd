package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Terminal
import com.torqueobd2.dash.ui.components.RacingHeader

import com.torqueobd2.dash.ui.components.racingPanel

import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.PollingMode
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.data.model.DeviceItem
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber
import com.torqueobd2.dash.util.BluetoothPermissionHelper
import com.torqueobd2.dash.util.DeviceDiagnostics

/**
 * Pantalla de configuración y gestión de conexión para ELM327 Bluetooth, WiFi y Modo Simulación.
 * Diseñada para diagnóstico profundo con información detallada de errores y soporte en Android 10 a 15.
 */
@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    pairedDevices: List<DeviceItem>,
    diagnosticLogs: List<String>,
    bluetoothErrorHint: String?,
    onRefreshDevices: () -> Unit,
    isScanningBluetooth: Boolean,
    onScanBluetooth: () -> Unit,
    onStopBluetoothScan: () -> Unit,
    onConnectBluetooth: (DeviceItem) -> Unit,
    onConnectWifi: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onClearDiagnosticLogs: () -> Unit,
    pollingMode: PollingMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val deviceInfo = remember(context) { DeviceDiagnostics.describe(context) }

    var wifiHost by rememberSaveable { mutableStateOf(connectionState.wifiHost) }
    var wifiPortStr by rememberSaveable { mutableStateOf(connectionState.wifiPort.toString()) }
    var scanAfterPermission by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(BluetoothPermissionHelper.hasAllPermissions(context)) }
    var showLogsCopiedToast by remember { mutableStateOf(false) }

    // Launcher para pedir permisos de Bluetooth en tiempo de ejecución (Android 10 - 15)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = BluetoothPermissionHelper.hasAllPermissions(context)
        if (hasPermissions) onRefreshDevices()
        if (scanAfterPermission && BluetoothPermissionHelper.hasScanPermissions(context)) onScanBluetooth()
        scanAfterPermission = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermissions = BluetoothPermissionHelper.hasAllPermissions(context)
                onRefreshDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        hasPermissions = BluetoothPermissionHelper.hasAllPermissions(context)
        if (hasPermissions) {
            onRefreshDevices()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopBluetoothScan()
        }
    }

    val car = LocalCarMode.current
    var carSection by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = car && carSection != null) { carSection = null }
    if (car && carSection == null) {
        CarMenu(listOf(
            CarMenuEntry("Bluetooth", com.torqueobd2.dash.i18n.tr("Elegir adaptador ELM327", "Select ELM327 adapter"), Icons.Default.Bluetooth) { carSection = 0 },
            CarMenuEntry("WiFi", com.torqueobd2.dash.i18n.tr("Conexión por IP y puerto", "Connect via IP and port"), Icons.Default.Wifi) { carSection = 1 },
            CarMenuEntry(com.torqueobd2.dash.i18n.tr("Estado del enlace", "Link status"), com.torqueobd2.dash.i18n.tr("Conexión y reconexión", "Connection & reconnection"), Icons.Default.Tune) { carSection = 2 },
            CarMenuEntry(com.torqueobd2.dash.i18n.tr("Registro del enlace", "Link log"), com.torqueobd2.dash.i18n.tr("Detalles del adaptador", "Adapter details"), Icons.Default.Terminal) { carSection = 3 }
        ))
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (car) OutlinedButton(onClick = { carSection = null }) { Text(com.torqueobd2.dash.i18n.tr("Volver a conexión", "Back to connection")) }
        else RacingHeader(
            com.torqueobd2.dash.i18n.tr("Centro de conexión", "Connection Center"),
            com.torqueobd2.dash.i18n.tr("Reconexión automática al último OBD. Puedes seleccionar otro adaptador.", "Auto-reconnects to the last OBD adapter. You can select another adapter."),
            eyebrow = com.torqueobd2.dash.i18n.tr("ENLACE / ELM327", "LINK / ELM327")
        )

        Spacer(modifier = Modifier.height(14.dp))
        if (!car || carSection == 3) Text(text = deviceInfo, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // AVISO DE PERMISOS BLUETOOTH SI NO ESTÁN CONCEDIDOS
        if (!hasPermissions && (!car || carSection == 0)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarningAmber.copy(alpha = 0.15f))
                    .border(1.dp, WarningAmber, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = com.torqueobd2.dash.i18n.tr("Permisos de Bluetooth Necesarios", "Bluetooth Permissions Required"),
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = com.torqueobd2.dash.i18n.tr(
                            "Para poder buscar y conectar tu adaptador ELM327 mediante Bluetooth, Android requiere conceder permisos de dispositivos cercanos / Bluetooth.",
                            "To find and connect your ELM327 adapter via Bluetooth, Android requires granting nearby devices / Bluetooth permissions."
                        ),
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val perms = BluetoothPermissionHelper.getRequiredPermissions().toTypedArray()
                            permissionLauncher.launch(perms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(com.torqueobd2.dash.i18n.tr("Conceder Permisos de Bluetooth", "Grant Bluetooth Permissions"), color = CarbonBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!car || carSection == 2) {
        // TARJETA DE ESTADO ACTUAL DE LA CONEXIÓN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .racingPanel()
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = com.torqueobd2.dash.i18n.tr("ESTADO DE CONEXIÓN", "CONNECTION STATUS"),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (connectionState.type) {
                                ConnectionType.SIMULATION -> com.torqueobd2.dash.i18n.tr("Modo: Simulación Local", "Mode: Local Simulation")
                                ConnectionType.BLUETOOTH -> com.torqueobd2.dash.i18n.tr("Modo: Bluetooth SPP", "Mode: Bluetooth SPP")
                                ConnectionType.WIFI -> com.torqueobd2.dash.i18n.tr("Modo: WiFi TCP", "Mode: WiFi TCP")
                            },
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val statusColor = when (connectionState.status) {
                        ConnectionStatus.CONNECTED -> NeonGreen
                        ConnectionStatus.CONNECTING, ConnectionStatus.INITIALIZING_ELM -> WarningAmber
                        ConnectionStatus.RECONNECTING -> RacingOrange
                        ConnectionStatus.ERROR -> DangerRed
                        ConnectionStatus.DISCONNECTED -> TextMuted
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (connectionState.status == ConnectionStatus.CONNECTING ||
                                connectionState.status == ConnectionStatus.INITIALIZING_ELM ||
                                connectionState.status == ConnectionStatus.RECONNECTING
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = statusColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = when (connectionState.status) {
                                    ConnectionStatus.CONNECTED -> com.torqueobd2.dash.i18n.tr("CONECTADO", "CONNECTED")
                                    ConnectionStatus.CONNECTING -> com.torqueobd2.dash.i18n.tr("CONECTANDO...", "CONNECTING...")
                                    ConnectionStatus.RECONNECTING -> com.torqueobd2.dash.i18n.tr("RECONECTANDO...", "RECONNECTING...")
                                    ConnectionStatus.INITIALIZING_ELM -> com.torqueobd2.dash.i18n.tr("INICIALIZANDO...", "INITIALIZING...")
                                    ConnectionStatus.ERROR -> com.torqueobd2.dash.i18n.tr("ERROR", "ERROR")
                                    ConnectionStatus.DISCONNECTED -> com.torqueobd2.dash.i18n.tr("DESCONECTADO", "DISCONNECTED")
                                },
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = connectionState.statusMessage,
                    color = if (connectionState.status == ConnectionStatus.ERROR) DangerRed else TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // TARJETA DE RECOMENDACIÓN SI HAY ERROR
                if (connectionState.status == ConnectionStatus.ERROR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A1517))
                            .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = com.torqueobd2.dash.i18n.tr("Diagnóstico / Solución recomendada:", "Troubleshooting / Recommended fix:"),
                                    color = DangerRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = com.torqueobd2.dash.i18n.tr(
                                        "1. Comprueba que el contacto del coche esté dado (llave en posición ON o motor en marcha).\n2. Verifica que el adaptador tenga el LED de encendido activo.\n3. Asegúrate de haber emparejado el ELM327 previamente en Ajustes Bluetooth de Android (código 1234 o 0000).\n4. Consulta el 'Registro de Diagnóstico' más abajo para ver el comando exacto que falló.",
                                        "1. Make sure the vehicle ignition is ON (key in ON position or engine running).\n2. Verify the adapter's power LED is lit.\n3. Ensure you paired the ELM327 in Android Bluetooth Settings (PIN 1234 or 0000).\n4. Check the 'Live Diagnostic Log' below to see the exact command that failed."
                                    ),
                                    color = TextWhite.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = com.torqueobd2.dash.i18n.tr("Consultas al adaptador:", "Adapter queries:"),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", connectionState.commandsPerSecond)} ${com.torqueobd2.dash.i18n.tr("consultas/s", "queries/s")}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (connectionState.status == ConnectionStatus.CONNECTED && pollingMode == PollingMode.VISIBLE) {
                    Text(
                        com.torqueobd2.dash.i18n.tr(
                            "Solo pantalla: aquí no se consultan sensores. Abre un cuadro para ver la frecuencia de lectura.",
                            "Screen only: no sensors are polled here. Open a dashboard to see the polling rate."
                        ),
                        color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (connectionState.status != ConnectionStatus.DISCONNECTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (connectionState.status == ConnectionStatus.CONNECTED) com.torqueobd2.dash.i18n.tr("Desconectar Adaptador", "Disconnect Adapter")
                            else com.torqueobd2.dash.i18n.tr("Detener reconexión", "Stop reconnecting"),
                            color = DangerRed, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        }
        if (!car || carSection == 0) {
        // SECCIÓN 1: BLUETOOTH REAL ELM327
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (car) "Bluetooth" else com.torqueobd2.dash.i18n.tr("ADAPTADORES BLUETOOTH (ELM327)", "BLUETOOTH ADAPTERS (ELM327)"),
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onRefreshDevices) {
                Icon(Icons.Default.Refresh, contentDescription = com.torqueobd2.dash.i18n.tr("Actualizar dispositivos", "Refresh devices"), tint = NeonCyan)
            }
        }

        if (bluetoothErrorHint != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF261D13))
                    .border(1.dp, WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = bluetoothErrorHint, color = WarningAmber, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                if (isScanningBluetooth) onStopBluetoothScan()
                else if (BluetoothPermissionHelper.hasScanPermissions(context)) onScanBluetooth()
                else {
                    scanAfterPermission = true
                    permissionLauncher.launch(BluetoothPermissionHelper.getScanPermissions().toTypedArray())
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isScanningBluetooth) com.torqueobd2.dash.i18n.tr("Detener búsqueda", "Stop scan")
                else com.torqueobd2.dash.i18n.tr("Buscar adaptadores cercanos", "Scan for nearby adapters"),
                color = NeonCyan
            )
        }
        Text(
            com.torqueobd2.dash.i18n.tr(
                "Emparejados y encontrados en la búsqueda. Al elegir uno nuevo, Android puede pedir su PIN. La simulación se activa desde la pestaña General.",
                "Paired and discovered devices. When selecting a new one, Android may ask for its PIN. Simulation can be enabled in the General tab."
            ),
            color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp)
        )
        OutlinedButton(onClick = { openBluetoothSettings(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(com.torqueobd2.dash.i18n.tr("Ajustes de Bluetooth de Android", "Android Bluetooth Settings"), color = NeonCyan)
        }
        Spacer(Modifier.height(8.dp))

        // LISTA DE DISPOSITIVOS EMPAREJADOS
        if (pairedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .racingPanel()
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = com.torqueobd2.dash.i18n.tr(
                            "No hay adaptadores emparejados ni encontrados.",
                            "No paired or discovered adapters found."
                        ),
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = com.torqueobd2.dash.i18n.tr(
                            "Para conectar un ELM327 físico:\n1. Conéctalo a la toma OBD-II del vehículo.\n2. Pon la llave del coche en contacto (ON).\n3. Empareja el adaptador en Ajustes de Bluetooth de la radio (PIN habitual: 1234 o 0000).\n4. Vuelve a esta app.\nSi el ELM327 aparece solo en la app de fábrica y no aquí, el Bluetooth de la radio puede no estar disponible para otras apps; prueba un ELM327 WiFi.",
                            "To connect a physical ELM327:\n1. Plug it into the vehicle's OBD-II port.\n2. Turn the vehicle ignition to ON.\n3. Pair the adapter in your device's Bluetooth Settings (common PIN: 1234 or 0000).\n4. Return to this app.\nIf the ELM327 only appears in the head unit's factory app and not here, its Bluetooth stack may be restricted; try a WiFi ELM327."
                        ),
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            openBluetoothSettings(context)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(com.torqueobd2.dash.i18n.tr("Abrir Ajustes de Bluetooth de Android", "Open Android Bluetooth Settings"), color = NeonCyan)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pairedDevices.forEach { dev ->
                    val isObd = listOf("OBD", "ELM", "VGATE", "VLINKER", "VIECA", "CARISTA", "KONNWEI")
                        .any { dev.name.uppercase().contains(it) }

                    val isCurrentDevice = connectionState.activeDevice?.address == dev.address &&
                            connectionState.status == ConnectionStatus.CONNECTED

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrentDevice) Color(0xFF142C24) else CardBackground)
                            .border(
                                1.dp,
                                if (isCurrentDevice) NeonGreen else if (isObd) NeonCyan.copy(alpha = 0.5f) else Color(0xFF293341),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.name,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isObd) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("OBD2", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${dev.address} · ${if (dev.isPaired) com.torqueobd2.dash.i18n.tr("Emparejado", "Paired") else com.torqueobd2.dash.i18n.tr("Encontrado", "Discovered")}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            if (isCurrentDevice) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(com.torqueobd2.dash.i18n.tr("ACTIVO", "ACTIVE"), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onConnectBluetooth(dev) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isObd) RacingOrange else Color(0xFF242E3D)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(com.torqueobd2.dash.i18n.tr("Conectar", "Connect"), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        }
        if (!car || carSection == 1) {
        // SECCIÓN 3: WIFI / TCP
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (car) "WiFi / TCP" else com.torqueobd2.dash.i18n.tr("ADAPTADOR WIFI ELM327 (TCP)", "WIFI ELM327 ADAPTER (TCP)"),
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .racingPanel()
                .padding(12.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wifiHost,
                        onValueChange = { wifiHost = it },
                        label = { Text(if (car) com.torqueobd2.dash.i18n.tr("Dirección IP", "IP Address") else com.torqueobd2.dash.i18n.tr("IP Host (ej: 192.168.0.10)", "IP Host (e.g., 192.168.0.10)")) },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = wifiPortStr,
                        onValueChange = { wifiPortStr = it },
                        label = { Text(if (car) com.torqueobd2.dash.i18n.tr("Puerto", "Port") else com.torqueobd2.dash.i18n.tr("Puerto (ej: 35000)", "Port (e.g., 35000)")) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val port = wifiPortStr.toIntOrNull() ?: 35000
                        onConnectWifi(wifiHost, port)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(com.torqueobd2.dash.i18n.tr("Conectar vía WiFi TCP", "Connect via WiFi TCP"), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        }
        if (!car || carSection == 3) {
        // SECCIÓN 4: REGISTRO DE DIAGNÓSTICO EN TIEMPO REAL (MÁXIMA PROFUNDIDAD DE DETALLE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (car) com.torqueobd2.dash.i18n.tr("Registro del enlace", "Link log") else com.torqueobd2.dash.i18n.tr("REGISTRO DE DIAGNÓSTICO EN VIVO", "LIVE DIAGNOSTIC LOG"),
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = {
                    val fullText = deviceInfo + "\n\n" + diagnosticLogs.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(fullText))
                    showLogsCopiedToast = true
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = com.torqueobd2.dash.i18n.tr("Copiar logs", "Copy logs"), tint = NeonCyan)
                }
                IconButton(onClick = onClearDiagnosticLogs) {
                    Icon(Icons.Default.Delete, contentDescription = com.torqueobd2.dash.i18n.tr("Limpiar logs", "Clear logs"), tint = DangerRed)
                }
            }
        }

        AnimatedVisibility(visible = showLogsCopiedToast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonGreen.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                Text(
                    text = com.torqueobd2.dash.i18n.tr("✓ Registro de diagnóstico copiado al portapapeles", "✓ Diagnostic log copied to clipboard"),
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F1218))
                .border(1.dp, Color(0xFF232B38), RoundedCornerShape(10.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState(initial = Int.MAX_VALUE))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (diagnosticLogs.isEmpty()) {
                    Text(
                        text = com.torqueobd2.dash.i18n.tr(
                            "No hay eventos registrados aún. Al conectar el ELM327 verás aquí el handshake detallado paso a paso.",
                            "No events logged yet. When connecting the ELM327 you will see the step-by-step handshake here."
                        ),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                } else {
                    diagnosticLogs.forEach { logLine ->
                        val color = when {
                            logLine.contains("❌") || logLine.contains("ERROR") -> DangerRed
                            logLine.contains("⚠️") || logLine.contains("Aviso") || logLine.contains("Warning") -> WarningAmber
                            logLine.contains("✅") || logLine.contains("🚀") -> NeonGreen
                            logLine.contains("🔌") || logLine.contains("🔍") -> NeonCyan
                            else -> TextWhite.copy(alpha = 0.85f)
                        }

                        Text(
                            text = logLine,
                            color = color,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        }
    }
}

private fun openBluetoothSettings(context: android.content.Context) {
    for (action in listOf(Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_SETTINGS)) {
        try {
            context.startActivity(Intent(action))
            return
        } catch (_: ActivityNotFoundException) {
            // Some head units only provide their own settings activity.
        } catch (_: SecurityException) {
            // Try the general settings screen if the vendor restricts Bluetooth settings.
        }
    }
    Toast.makeText(
        context,
        com.torqueobd2.dash.i18n.tr(
            "Abre los ajustes de Bluetooth desde el menú de la radio.",
            "Open Bluetooth settings from your head unit menu."
        ),
        Toast.LENGTH_LONG
    ).show()
}
