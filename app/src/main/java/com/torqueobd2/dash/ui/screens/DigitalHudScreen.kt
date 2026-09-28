package com.torqueobd2.dash.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.ui.components.ControlPedalSlider
import com.torqueobd2.dash.ui.components.DigitalHudGauge
import com.torqueobd2.dash.ui.components.RealtimeChart
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.RacingOrange

/**
 * Pantalla de visualización digital estilo HUD de carreras, adaptable fluidamente
 * a modo horizontal (salpicadero) o vertical.
 */
@Composable
fun DigitalHudScreen(
    vehicleState: VehicleState,
    isSimulationMode: Boolean,
    onThrottleChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .verticalScroll(rememberScrollState())
            .padding(if (isLandscape) 8.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            // Diseño apaisado de salpicadero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HUD digital ocupando el lado izquierdo
                DigitalHudGauge(
                    speedKmh = vehicleState.speedKmh,
                    rpm = vehicleState.rpm,
                    currentGear = vehicleState.gearEstimated,
                    modifier = Modifier.weight(1.2f)
                )

                // Telemetría rápida en el lado derecho
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rpmPoints = vehicleState.sparklineHistory["RPM"] ?: emptyList()
                    RealtimeChart(
                        title = "Telemetría RPM",
                        points = rpmPoints,
                        currentValue = vehicleState.rpm.toInt().toString(),
                        unit = "RPM",
                        lineColor = RacingOrange,
                        chartHeight = 70.dp
                    )

                    val speedPoints = vehicleState.sparklineHistory["Velocidad"] ?: emptyList()
                    RealtimeChart(
                        title = "Velocidad km/h",
                        points = speedPoints,
                        currentValue = vehicleState.speedKmh.toInt().toString(),
                        unit = "km/h",
                        lineColor = NeonCyan,
                        chartHeight = 70.dp
                    )
                }
            }
        } else {
            // Diseño vertical
            DigitalHudGauge(
                speedKmh = vehicleState.speedKmh,
                rpm = vehicleState.rpm,
                currentGear = vehicleState.gearEstimated
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val rpmPoints = vehicleState.sparklineHistory["RPM"] ?: emptyList()
                RealtimeChart(
                    title = "Telemetría RPM",
                    points = rpmPoints,
                    currentValue = vehicleState.rpm.toInt().toString(),
                    unit = "RPM",
                    lineColor = RacingOrange,
                    modifier = Modifier.weight(1f)
                )

                val speedPoints = vehicleState.sparklineHistory["Velocidad"] ?: emptyList()
                RealtimeChart(
                    title = "Velocidad en tiempo real",
                    points = speedPoints,
                    currentValue = vehicleState.speedKmh.toInt().toString(),
                    unit = "km/h",
                    lineColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isSimulationMode) {
            ControlPedalSlider(onThrottleChanged = onThrottleChanged)
        }
    }
}
