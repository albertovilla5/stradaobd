package com.torqueobd2.dash.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.ui.components.AnalogGauge
import com.torqueobd2.dash.ui.components.ControlPedalSlider
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.RacingOrange

/**
 * Cuadro de mandos analógico deportivo adaptable dinámicamente a orientación
 * vertical (móvil) u horizontal / panorámica (salpicadero / radio de coche).
 */
@Composable
fun SportDashboardScreen(
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
            // DISEÑO PANORÁMICO / HORIZONTAL (Dashboard de coche en salpicadero)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tacómetro
                AnalogGauge(
                    value = vehicleState.rpm,
                    minValue = 0f,
                    maxValue = 6000f,
                    title = "MOTOR",
                    unit = "RPM",
                    redlineStart = 4500f,
                    majorTickStep = 1000f,
                    primaryColor = RacingOrange,
                    modifier = Modifier.weight(1.1f)
                )

                // Turbo
                AnalogGauge(
                    value = vehicleState.boostPressureBar,
                    minValue = 0f,
                    maxValue = 2.5f,
                    title = "TURBO",
                    unit = "BAR",
                    redlineStart = 1.8f,
                    majorTickStep = 0.5f,
                    primaryColor = RacingOrange,
                    modifier = Modifier.weight(0.85f)
                )

                // Temperatura
                AnalogGauge(
                    value = vehicleState.coolantTempC,
                    minValue = 40f,
                    maxValue = 130f,
                    title = "REFRIG.",
                    unit = "°C",
                    redlineStart = 105f,
                    majorTickStep = 20f,
                    primaryColor = NeonCyan,
                    modifier = Modifier.weight(0.85f)
                )

                // Velocímetro
                AnalogGauge(
                    value = vehicleState.speedKmh,
                    minValue = 0f,
                    maxValue = 240f,
                    title = "VELOCIDAD",
                    unit = "KM/H",
                    redlineStart = 200f,
                    majorTickStep = 40f,
                    primaryColor = NeonCyan,
                    modifier = Modifier.weight(1.1f)
                )
            }
        } else {
            // DISEÑO VERTICAL (Móvil en soporte vertical)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalogGauge(
                    value = vehicleState.rpm,
                    minValue = 0f,
                    maxValue = 6000f,
                    title = "MOTOR",
                    unit = "RPM",
                    redlineStart = 4500f,
                    majorTickStep = 1000f,
                    primaryColor = RacingOrange,
                    modifier = Modifier.weight(1f)
                )

                AnalogGauge(
                    value = vehicleState.speedKmh,
                    minValue = 0f,
                    maxValue = 240f,
                    title = "VELOCIDAD",
                    unit = "KM/H",
                    redlineStart = 200f,
                    majorTickStep = 40f,
                    primaryColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalogGauge(
                    value = vehicleState.boostPressureBar,
                    minValue = 0f,
                    maxValue = 2.5f,
                    title = "TURBO",
                    unit = "BAR",
                    redlineStart = 1.8f,
                    majorTickStep = 0.5f,
                    primaryColor = RacingOrange,
                    modifier = Modifier.weight(1f)
                )

                AnalogGauge(
                    value = vehicleState.coolantTempC,
                    minValue = 40f,
                    maxValue = 130f,
                    title = "REFRIG.",
                    unit = "°C",
                    redlineStart = 105f,
                    majorTickStep = 20f,
                    primaryColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isSimulationMode) {
            ControlPedalSlider(onThrottleChanged = onThrottleChanged)
        }
    }
}
