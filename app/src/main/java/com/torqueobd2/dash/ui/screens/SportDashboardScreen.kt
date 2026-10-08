package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.CarDrivingDashboard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.components.*
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.data.model.telemetryText

@Composable
fun SportDashboardScreen(
    vehicleState: VehicleState, availablePids: List<ObdPid>, connectionState: ConnectionState, isSimulationMode: Boolean, onThrottleChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier, maxRpm: Float = 6000f
) {
    if (LocalCarMode.current) {
        CarDrivingDashboard(vehicleState, availablePids, connectionState, maxRpm)
        return
    }
    fun status(id: String): SensorTelemetryStatus = availablePids.find { it.id == id }
        ?.telemetryStatus(vehicleState, connectionState) ?: SensorTelemetryStatus.MISSING
    val rpmStatus = status(SensorIds.RPM)
    val speedStatus = status(SensorIds.SPEED)
    val coolantStatus = status(SensorIds.COOLANT)
    val voltageStatus = status(SensorIds.VOLTAGE)
    var throttleExpanded by rememberSaveable { mutableStateOf(false) }
    val charts: @Composable (Modifier) -> Unit = { chartModifier ->
        Column(chartModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RealtimeChart(if (vehicleState.rpm.isFinite()) "Régimen motor" else "RPM · ${rpmStatus.label}", vehicleState.sparklineHistory[SensorIds.RPM] ?: emptyList(),
                vehicleState.rpm.telemetryText(), "RPM", RacingOrange, modifier = Modifier.weight(1f).fillMaxWidth())
            RealtimeChart(if (vehicleState.speedKmh.isFinite()) "Velocidad" else "Velocidad · ${speedStatus.label}", vehicleState.sparklineHistory[SensorIds.SPEED] ?: emptyList(),
                vehicleState.speedKmh.telemetryText(), "km/h", NeonCyan, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
    ResponsiveCanvas(modifier.background(CarbonBlack), minimumHeight = { width, scale ->
        (if (width >= 500.dp * scale) 215.dp else 475.dp) * scale + 44.dp +
            (if (isSimulationMode) (if (throttleExpanded) 104.dp else 54.dp) * scale else 0.dp)
    }) {
        val wide = maxWidth >= 500.dp * uiLayoutScale()
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DigitalHudGauge(vehicleState.speedKmh, vehicleState.rpm, maxRpm, Modifier.weight(1.8f).fillMaxWidth(),
                            coolantTempC = vehicleState.coolantTempC, batteryVoltage = vehicleState.batteryVoltage,
                            speedStatus = speedStatus.label, rpmStatus = rpmStatus.label)
                        Row(Modifier.weight(0.85f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TelemetryMetric("Refrigerante", vehicleState.coolantTempC.telemetryText(),
                                if (vehicleState.coolantTempC.isFinite()) "°C" else coolantStatus.label, Modifier.weight(1f).fillMaxHeight())
                            TelemetryMetric("Voltaje ECU", vehicleState.batteryVoltage.telemetryText(),
                                if (vehicleState.batteryVoltage.isFinite()) "V" else voltageStatus.label, Modifier.weight(1f).fillMaxHeight(), accent = NeonCyan)
                        }
                    }
                    charts(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                DigitalHudGauge(vehicleState.speedKmh, vehicleState.rpm, maxRpm, Modifier.weight(1f).fillMaxWidth(),
                    coolantTempC = vehicleState.coolantTempC, batteryVoltage = vehicleState.batteryVoltage,
                    speedStatus = speedStatus.label, rpmStatus = rpmStatus.label)
                Row(Modifier.weight(0.5f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryMetric("Refrigerante", vehicleState.coolantTempC.telemetryText(),
                        if (vehicleState.coolantTempC.isFinite()) "°C" else coolantStatus.label, Modifier.weight(1f).fillMaxHeight())
                    TelemetryMetric("Voltaje ECU", vehicleState.batteryVoltage.telemetryText(),
                        if (vehicleState.batteryVoltage.isFinite()) "V" else voltageStatus.label, Modifier.weight(1f).fillMaxHeight(), accent = NeonCyan)
                }
                charts(Modifier.weight(1f).fillMaxWidth())
            }
            if (isSimulationMode) ControlPedalSlider(onThrottleChanged, onExpandedChanged = { throttleExpanded = it })
        }
    }
}
