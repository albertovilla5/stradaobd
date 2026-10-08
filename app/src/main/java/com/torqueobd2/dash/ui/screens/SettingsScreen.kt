package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarTab as Tab
import com.torqueobd2.dash.ui.automotive.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.components.RacingHeader
import com.torqueobd2.dash.ui.components.racingPanel
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.data.model.PollingMode

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
    recordingContent: @Composable () -> Unit
) {
    if (LocalCarMode.current) {
        BackHandler(enabled = selectedTabIndex >= 0) { onTabSelected(-1) }
        when (selectedTabIndex) {
            0 -> GeneralSettingsScreen(simulationEnabled, onSimulationChanged, pollingMode, onPollingModeChanged)
            1 -> connectionContent()
            2 -> terminalContent()
            3 -> alertsContent()
            4 -> recordingContent()
            else -> CarMenu(listOf(
                CarMenuEntry("Pantalla y lectura", "Modo coche / móvil", Icons.Default.Tune) { onTabSelected(0) },
                CarMenuEntry("Conexión", "Bluetooth y WiFi", Icons.Default.Bluetooth) { onTabSelected(1) },
                CarMenuEntry("Avisos", "Alertas de sensores", Icons.Default.Notifications) { onTabSelected(3) },
                CarMenuEntry("Grabación", "Sesiones y archivos CSV", Icons.Default.FiberManualRecord) { onTabSelected(4) },
                CarMenuEntry("Terminal", "Comandos del adaptador", Icons.Default.Terminal) { onTabSelected(2) }
            ))
        }
        return
    }
    val tabs = listOf("General", "Conexión", "Terminal", "Avisos", "Grabación")
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
                    0 -> GeneralSettingsScreen(simulationEnabled, onSimulationChanged, pollingMode, onPollingModeChanged)
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
private fun GeneralSettingsScreen(simulationEnabled: Boolean, onSimulationChanged: (Boolean) -> Unit,
    pollingMode: PollingMode, onPollingModeChanged: (PollingMode) -> Unit) {
    Column(Modifier.fillMaxSize().background(CarbonBlack).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!LocalCarMode.current) RacingHeader("Preferencias", "Simulación y lectura de sensores", eyebrow = "CONFIGURACIÓN / GENERAL")
        InterfaceModeSettings()
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lectura de sensores", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            Text("Al cambiar de pantalla se actualiza la selección. Los sensores con avisos activados se consultan en todos los modos aunque no estén visibles, siempre que estén habilitados en Sensores. El escaneo completo de la ECU sigue disponible en Vehículo.",
                color = TextMuted, fontSize = 12.sp)
        }
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Modo simulación", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Datos de prueba para usar los indicadores sin el coche.", color = TextMuted, fontSize = 13.sp)
                }
                Switch(checked = simulationEnabled, onCheckedChange = onSimulationChanged)
            }
            Text("Desactivado por defecto. Al activarlo se desconecta el OBD real. Al desactivarlo se vuelve a intentar conectar al último adaptador guardado.",
                color = TextMuted, fontSize = 12.sp)
        }
        Column(Modifier.fillMaxWidth().racingPanel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conexión automática", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("La app recuerda el último adaptador Bluetooth o WiFi conectado y vuelve a intentarlo mientras no esté disponible. Puedes elegir otro desde la pestaña Conexión. Desconectar detiene los intentos hasta que selecciones un adaptador o vuelvas a abrir la app.",
                color = TextMuted, fontSize = 13.sp)
        }
    }
}


@Composable
private fun InterfaceModeSettings() {
    val car = LocalCarMode.current
    val onChange = LocalOnCarModeChanged.current
    Column(Modifier.fillMaxWidth().racingPanel().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Presentación de la pantalla", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text("Elige el tamaño y la navegación para tu dispositivo.", color = TextMuted)
        listOf(true to "Modo coche", false to "Modo móvil").forEach { (value, title) ->
            Row(Modifier.fillMaxWidth().heightIn(min = if (car) 76.dp else 48.dp)
                .selectable(selected = car == value, role = Role.RadioButton, onClick = { onChange(value) })
                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadioButton(selected = car == value, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(title, color = if (car == value) RacingOrange else TextWhite, fontWeight = FontWeight.Medium)
                    Text(if (value) "Botones grandes y accesos directos" else "Interfaz compacta para teléfono", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
