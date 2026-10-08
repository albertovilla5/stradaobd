package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.ui.components.*
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.data.model.telemetryText
import java.util.Locale

@Composable
fun DpfDiagnosticsScreen(vehicleState: VehicleState, isSimulationMode: Boolean,
    onThrottleChanged: (Float?) -> Unit, modifier: Modifier = Modifier) {
    var throttleExpanded by rememberSaveable { mutableStateOf(false) }
    val card: @Composable (Modifier) -> Unit = { cardModifier ->
        DpfMonitorCard(vehicleState.dpfDifferentialPressureKpa, vehicleState.exhaustGasTemp1C,
            vehicleState.dpfCalculatedPmGrams, vehicleState.dpfDistanceLastRegenKm, vehicleState.dpfFuelLastRegenL,
            vehicleState.readings["HONDA_DPF active reg"]?.formatted() ?: "—", vehicleState.engineOilLevelMm, vehicleState.dpfExhaustFlowM3h, cardModifier)
    }
    val charts: @Composable (Modifier) -> Unit = { chartModifier ->
        Column(chartModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RealtimeChart("Contrapresión", vehicleState.sparklineHistory["HONDA_DPF diff.pres"] ?: emptyList(),
                vehicleState.dpfDifferentialPressureKpa.telemetryText(2), "kPa", NeonCyan,
                modifier = Modifier.weight(1f).fillMaxWidth())
            RealtimeChart("Temperatura EGT", vehicleState.sparklineHistory["HONDA_EGT"] ?: emptyList(),
                vehicleState.exhaustGasTemp1C.telemetryText(), "°C", RacingOrange,
                modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
    ResponsiveCanvas(modifier.background(CarbonBlack), minimumHeight = { width, scale ->
        (if (width >= 500.dp * scale) 210.dp else 490.dp) * scale + 44.dp +
            (if (isSimulationMode) (if (throttleExpanded) 104.dp else 54.dp) * scale else 0.dp)
    }) {
        val wide = maxWidth >= 500.dp * uiLayoutScale()
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                card(Modifier.weight(1.25f).fillMaxHeight())
                charts(Modifier.weight(1f).fillMaxHeight())
            } else {
                card(Modifier.fillMaxWidth())
                charts(Modifier.weight(1f).fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().racingPanel(12.dp).padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text("PIDs Honda según CSV · Los códigos de regeneración requieren validación para tu ECU.",
                    color = TextMuted, fontSize = 11.sp, maxLines = 2)
            }
            if (isSimulationMode) ControlPedalSlider(onThrottleChanged, onExpandedChanged = { throttleExpanded = it })
        }
    }
}
