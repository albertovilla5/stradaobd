package com.torqueobd2.dash.ui.automotive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.components.*
import com.torqueobd2.dash.ui.theme.*

/** Four fixed destinations, including a direct entry to OBD diagnostics. */
@Composable
fun CarShell(title: String, vehicle: String, connection: ConnectionState,
    selected: Int, menuVisible: Boolean, onDriving: () -> Unit, onPanels: () -> Unit,
    onMenu: () -> Unit, onDiagnostics: () -> Unit, diagnosticCount: Int? = null, onBack: () -> Unit, onConnection: () -> Unit,
    content: @Composable () -> Unit) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxSize().background(CockpitBackground)) {
        val navigationHeight = maxOf(76.dp, (8f + maxOf(32f, 30f * scale) + 29f * scale).dp)
        val rail = maxWidth > maxHeight && maxWidth >= 560.dp * scale && maxHeight >= navigationHeight * 4 + 8.dp
        val railWidth = 128.dp * scale
        val narrow = maxWidth < 600.dp * scale
        val shortRail = maxHeight < 500.dp * scale
        val railGap = ((maxHeight - navigationHeight * 4 - 8.dp) / 3).coerceIn(0.dp, 8.dp)
        val bottomColumns = if (maxWidth < 480.dp * scale) 2 else 4
        val navigation: @Composable (Boolean, IntRange) -> Unit = { vertical, range ->
            val items = listOf(
                Triple(com.torqueobd2.dash.i18n.tr("Conducción", "Driving"), Icons.Default.Speed, onDriving),
                Triple(com.torqueobd2.dash.i18n.tr("Paneles", "Panels"), Icons.Default.DashboardCustomize, onPanels),
                Triple(com.torqueobd2.dash.i18n.tr("Averías", "Faults"), Icons.Default.CarRepair, onDiagnostics),
                Triple(com.torqueobd2.dash.i18n.tr("Menú", "Menu"), Icons.Default.Apps, onMenu)
            )
            items.forEachIndexed { index, item ->
                if (index !in range) return@forEachIndexed
                val active = when (index) { 0 -> !menuVisible && selected == 0; 1 -> !menuVisible && selected == 1; 2 -> !menuVisible && selected == 5; else -> menuVisible || selected in 2..4 }
                Surface(onClick = item.third, modifier = if (vertical) Modifier.semantics { contentDescription = item.first }.fillMaxWidth().heightIn(min = if (shortRail) navigationHeight else 92.dp)
                    else Modifier.semantics { contentDescription = item.first }.width((maxWidth - 24.dp) / bottomColumns).heightIn(min = 76.dp),
                    color = if (active) CockpitAccent else Color.Transparent,
                    shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = if (shortRail && vertical) 2.dp else 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (shortRail && vertical) 4.dp else 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(item.second, null, Modifier.size(32.dp), tint = if (active) CockpitBackground else CockpitMuted)
                            if (index == 2 && diagnosticCount != null) Surface(color = DangerRed, shape = CircleShape) {
                                Text(diagnosticCount.toString(), Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = CarbonBlack, fontSize = 20.sp)
                            }
                        }
                        Text(if (index == 0) com.torqueobd2.dash.i18n.tr("Inicio", "Home") else item.first, color = if (active) CockpitBackground else CockpitMuted, fontSize = 24.sp,
                            fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxSize()) {
            if (rail) Column(Modifier.width(railWidth).fillMaxHeight().padding(horizontal = 12.dp, vertical = if (shortRail) 4.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (shortRail) railGap else 20.dp)) {
                if (!shortRail) Text("STRADA", Modifier.fillMaxWidth().padding(vertical = 8.dp), color = CockpitMuted, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 2.5.sp)
                navigation(true, 0..3)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (menuVisible || selected > 1) CarIconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, com.torqueobd2.dash.i18n.tr("Volver", "Back"), Modifier.size(32.dp), tint = TextWhite)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (rail) Text(vehicle, color = CockpitMuted, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val label = when (connection.status) {
                        ConnectionStatus.CONNECTED -> if (connection.type == ConnectionType.SIMULATION) com.torqueobd2.dash.i18n.tr("Simulación", "Simulation") else com.torqueobd2.dash.i18n.tr("OBD conectado", "OBD connected")
                        ConnectionStatus.DISCONNECTED -> com.torqueobd2.dash.i18n.tr("Conectar OBD", "Connect OBD")
                        ConnectionStatus.ERROR -> com.torqueobd2.dash.i18n.tr("Error OBD", "OBD error")
                        ConnectionStatus.RECONNECTING -> com.torqueobd2.dash.i18n.tr("Reconectando", "Reconnecting")
                        else -> com.torqueobd2.dash.i18n.tr("Conectando", "Connecting")
                    }
                    if (narrow) {
                        if (connection.status == ConnectionStatus.CONNECTED && connection.type == ConnectionType.SIMULATION) {
                            CarTextButton(onConnection) { Text("Demo", color = CockpitTeal, fontSize = 24.sp) }
                        } else CarIconButton(onConnection) {
                            Icon(Icons.Default.SettingsInputAntenna, label, Modifier.size(32.dp),
                                tint = if (connection.status == ConnectionStatus.CONNECTED) CockpitTeal else CockpitAccent)
                        }
                    } else CarTextButton(onConnection) {
                        Icon(if (connection.type == ConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.SettingsInputAntenna,
                            null, Modifier.size(32.dp), tint = if (connection.status == ConnectionStatus.CONNECTED) CockpitTeal else CockpitAccent)
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = TextWhite, fontSize = 24.sp)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                if (!rail) Column(Modifier.fillMaxWidth().background(CockpitBackground).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4 / bottomColumns) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically) { navigation(false, (row * bottomColumns) until ((row + 1) * bottomColumns)) }
                    }
                }
            }
        }
    }
}

data class CarMenuEntry(val title: String, val detail: String, val icon: ImageVector, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
fun CarMenu(entries: List<CarMenuEntry>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val columns = (maxWidth / (400.dp * scale)).toInt().coerceIn(1, 3)
        LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items(entries, key = { it.title }) { entry ->
                Surface(onClick = entry.onClick, enabled = entry.enabled, shape = RoundedCornerShape(28.dp), color = CockpitSurface, modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).background(CockpitElevated, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                            Icon(entry.icon, null, Modifier.size(32.dp), tint = CockpitAccent)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.title, color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Medium)
                            if (entry.detail.isNotBlank()) Text(entry.detail, color = TextMuted, fontSize = 24.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(28.dp), tint = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun CarDrivingDashboard(vehicle: VehicleState, pids: List<ObdPid>, connection: ConnectionState, maxRpm: Float) {
    val now = System.nanoTime()
    fun reading(id: String) = pids.find { it.id == id }?.currentReading(vehicle, connection, now)
    fun status(id: String) = pids.find { it.id == id }?.telemetryStatus(vehicle, connection, now)?.label ?: com.torqueobd2.dash.i18n.tr("Sin sensor", "No sensor")
    val rpm = reading(SensorIds.RPM)?.value
    val speed = reading(SensorIds.SPEED)?.value
    val coolant = reading(SensorIds.COOLANT)?.value
    val voltage = reading(SensorIds.VOLTAGE)?.value
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val wide = maxWidth >= 440.dp * scale
        val compact = maxHeight < 300.dp * scale
        val speedInstrument: @Composable (Modifier) -> Unit = { instrumentModifier ->
            BoxWithConstraints(instrumentModifier, contentAlignment = Alignment.Center) {
                val instrumentHeight = maxHeight
                val speedTextSize = ((instrumentHeight.value - 110f * scale) / (1.2f * scale)).coerceIn(48f, 144f).sp
                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(com.torqueobd2.dash.i18n.tr("Velocidad", "Speed"), color = CockpitMuted, fontSize = 24.sp)
                    FittedValue(speed?.telemetryText() ?: "—", speedTextSize,
                        color = TextWhite, fontWeight = FontWeight.Light, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = (-4).sp)
                    Text("km/h", color = CockpitTeal, fontSize = 28.sp)
                    if (speed == null && instrumentHeight > 260.dp * scale) Text(status(SensorIds.SPEED),
                        color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val auxiliary: @Composable (Modifier) -> Unit = { auxiliaryModifier ->
            Column(auxiliaryModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (compact) {
                    CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Motor", "Engine"), rpm?.telemetryText() ?: "—", "RPM", CockpitAccent, Modifier.weight(1f))
                    CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Agua", "Coolant"), coolant?.telemetryText() ?: "—", "°C", CockpitTeal, Modifier.weight(1f))
                    CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Voltaje", "Voltage"), voltage?.telemetryText(1) ?: "—", "V", CockpitAccent, Modifier.weight(1f))
                } else {
                    Column(Modifier.weight(1.15f).fillMaxWidth().background(CockpitSurface, RoundedCornerShape(28.dp)).padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Text(com.torqueobd2.dash.i18n.tr("Régimen motor", "Engine RPM"), color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FittedValue(rpm?.telemetryText() ?: "—", 64.sp, Modifier.weight(1f), color = TextWhite, fontWeight = FontWeight.Medium)
                            Text("RPM", color = CockpitAccent, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        CockpitTrack((rpm ?: 0f) / maxRpm.coerceAtLeast(1000f), CockpitAccent, Modifier.fillMaxWidth().height(8.dp))
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SecondaryDrivingMetric(com.torqueobd2.dash.i18n.tr("Agua", "Coolant"), coolant?.telemetryText() ?: "—", "°C", CockpitTeal, Modifier.weight(1f).fillMaxHeight())
                        SecondaryDrivingMetric(com.torqueobd2.dash.i18n.tr("Voltaje", "Voltage"), voltage?.telemetryText(1) ?: "—", "V", CockpitAccent, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            speedInstrument(Modifier.weight(1f).fillMaxHeight())
            auxiliary(Modifier.weight(1.1f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            speedInstrument(Modifier.weight(1f).fillMaxWidth())
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Motor", "Engine"), rpm?.telemetryText() ?: "—", "RPM", CockpitAccent, Modifier.weight(1f))
                CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Agua", "Coolant"), coolant?.telemetryText() ?: "—", "°C", CockpitTeal, Modifier.weight(1f))
                CompactDrivingMetric(com.torqueobd2.dash.i18n.tr("Voltaje", "Voltage"), voltage?.telemetryText(1) ?: "—", "V", CockpitAccent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SecondaryDrivingMetric(title: String, value: String, unit: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(CockpitSurface, RoundedCornerShape(28.dp)).padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = CockpitMuted, fontSize = 24.sp, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FittedValue(value, 44.sp, Modifier.weight(1f), color = TextWhite, fontWeight = FontWeight.Medium)
            Text(unit, color = accent, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun CompactDrivingMetric(title: String, value: String, unit: String, accent: Color, modifier: Modifier) {
    Row(modifier.fillMaxWidth().background(CockpitSurface, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = CockpitMuted, fontSize = 24.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        FittedValue(value, 36.sp, Modifier.weight(1f), color = TextWhite, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text(unit, color = accent, fontSize = 24.sp, maxLines = 1)
    }
}
