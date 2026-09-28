package com.torqueobd2.dash.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.ui.components.ControlPedalSlider
import com.torqueobd2.dash.ui.components.DpfMonitorCard
import com.torqueobd2.dash.ui.components.RealtimeChart
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite

/**
 * Pantalla especializada de diagnóstico para Filtro de Partículas (DPF) y PIDs específicos de Honda i-DTEC.
 */
@Composable
fun DpfDiagnosticsScreen(
    vehicleState: VehicleState,
    isSimulationMode: Boolean,
    onThrottleChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tarjeta principal de estado del DPF y gases
        DpfMonitorCard(
            diffPressureKpa = vehicleState.dpfDifferentialPressureKpa,
            egtTempC = vehicleState.exhaustGasTemp1C,
            pmGrams = vehicleState.dpfCalculatedPmGrams,
            distLastRegenKm = vehicleState.dpfDistanceLastRegenKm,
            fuelLastRegenL = vehicleState.dpfFuelLastRegenL,
            isRegenActive = vehicleState.dpfActiveRegen,
            oilLevelMm = vehicleState.engineOilLevelMm,
            exhaustFlowM3h = vehicleState.dpfExhaustFlowM3h
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gráficos en tiempo real de Presión diferencial y Temperatura EGT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val diffPoints = vehicleState.sparklineHistory["DPF diff.pres"] ?: emptyList()
            RealtimeChart(
                title = "Presión Diferencial (kPa)",
                points = diffPoints,
                currentValue = String.format("%.2f", vehicleState.dpfDifferentialPressureKpa),
                unit = "kPa",
                lineColor = if (vehicleState.dpfDifferentialPressureKpa > 20f) DangerRed else NeonCyan,
                modifier = Modifier.weight(1f)
            )

            val egtPoints = vehicleState.sparklineHistory["EGT"] ?: emptyList()
            RealtimeChart(
                title = "Temperatura EGT 1 (°C)",
                points = egtPoints,
                currentValue = vehicleState.exhaustGasTemp1C.toInt().toString(),
                unit = "°C",
                lineColor = RacingOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta informativa con diagnóstico de salud del DPF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, Color(0xFF28303D), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "Diagnóstico del Sistema de Escape",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        vehicleState.dpfActiveRegen -> "Regeneración activa en curso. Mantener el motor a régimen medio (2000-2500 RPM) hasta completar el ciclo."
                        vehicleState.dpfDifferentialPressureKpa > 20f -> "Presión diferencial alta (>20 kPa): El filtro requiere regeneración próxima."
                        vehicleState.engineOilLevelMm > 115f -> "Advertencia: Nivel de aceite alto (>115 mm), posible dilución por regeneraciones fallidas."
                        else -> "Valores nominales saludables. La contrapresión del escape y el cálculo de hollín se encuentran en rango óptimo."
                    },
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSimulationMode) {
            ControlPedalSlider(onThrottleChanged = onThrottleChanged)
        }
    }
}
