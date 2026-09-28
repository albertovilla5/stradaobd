package com.torqueobd2.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextDim
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber

/**
 * Tarjeta especializada en parámetros de regeneración y filtro de partículas (DPF)
 * correspondiente a los PIDs específicos de Honda i-DTEC importados.
 */
@Composable
fun DpfMonitorCard(
    diffPressureKpa: Float,
    egtTempC: Float,
    pmGrams: Float,
    distLastRegenKm: Float,
    fuelLastRegenL: Float,
    isRegenActive: Boolean,
    oilLevelMm: Float,
    exhaustFlowM3h: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFF28303D), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Encabezado con estado de Regeneración Activa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FILTRO DE PARTÍCULAS (DPF / i-DTEC)",
                color = RacingOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Indicador de regeneración activa
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isRegenActive) DangerRed else Color(0xFF202632))
                    .border(
                        1.dp,
                        if (isRegenActive) DangerRed else Color(0xFF354052),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isRegenActive) "● REGENERANDO" else "PASIVO / NORMAL",
                    color = if (isRegenActive) TextWhite else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bloque de presión diferencial y temperatura EGT
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Presión diferencial
            DpfMetricTile(
                title = "Presión Diferencial",
                value = String.format("%.2f", diffPressureKpa),
                unit = "kPa",
                accentColor = when {
                    diffPressureKpa > 20f -> DangerRed
                    diffPressureKpa > 10f -> WarningAmber
                    else -> NeonCyan
                },
                modifier = Modifier.weight(1f)
            )

            // Temperatura de escape EGT
            DpfMetricTile(
                title = "Temperatura EGT 1",
                value = egtTempC.toInt().toString(),
                unit = "°C",
                accentColor = when {
                    egtTempC > 600f -> DangerRed
                    egtTempC > 450f -> RacingOrange
                    else -> NeonGreen
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bloque de hollín y flujo
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Hollín PM estimado
            DpfMetricTile(
                title = "Masa de Hollín PM",
                value = String.format("%.1f", pmGrams),
                unit = "g",
                accentColor = if (pmGrams > 22f) WarningAmber else NeonCyan,
                modifier = Modifier.weight(1f)
            )

            // Nivel de aceite motor filtrado
            DpfMetricTile(
                title = "Nivel Aceite (Filt)",
                value = String.format("%.1f", oilLevelMm),
                unit = "mm",
                accentColor = if (oilLevelMm > 115f) DangerRed else TextWhite,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila inferior: Distancia y Combustible desde última regeneración
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2430))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Distancia post-regen", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = "${distLastRegenKm.toInt()} km",
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Combustible post-regen", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = "${String.format("%.1f", fuelLastRegenL)} L",
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DpfMetricTile(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2430))
            .border(1.dp, Color(0xFF2C3545), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
