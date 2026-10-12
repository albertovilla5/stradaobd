package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.data.model.telemetryText
import java.util.Locale

@Composable
fun DpfMonitorCard(
    diffPressureKpa: Float, egtTempC: Float, pmGrams: Float, distLastRegenKm: Float,
    fuelLastRegenL: Float, regenCode: String, oilLevelMm: Float,
    exhaustFlowM3h: Float, modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.racingPanel().padding(10.dp)) {
        val fontScale = uiLayoutScale()
        val compact = maxHeight < 230.dp * fontScale && maxWidth >= 220.dp * fontScale
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (!compact) Text(com.torqueobd2.dash.i18n.tr("FILTRO DE PARTÍCULAS", "PARTICULATE FILTER"), color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(com.torqueobd2.dash.i18n.tr("Estado del DPF", "DPF Status"), color = TextWhite, fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusPill(com.torqueobd2.dash.i18n.tr("Código reg.: $regenCode", "Regen code: $regenCode"), TextMuted)
            }
            val meters: @Composable (Modifier, Boolean) -> Unit = { meterModifier, stacked ->
                DpfMeter(if (stacked) com.torqueobd2.dash.i18n.tr("Presión DPF", "DPF Pressure") else com.torqueobd2.dash.i18n.tr("Presión diferencial", "Differential pressure"), diffPressureKpa.telemetryText(2),
                    "kPa", diffPressureKpa / 30, NeonCyan, meterModifier, stacked)
                DpfMeter(if (stacked) com.torqueobd2.dash.i18n.tr("Temp. escape", "Exhaust temp") else com.torqueobd2.dash.i18n.tr("Temperatura de escape", "Exhaust temperature"), egtTempC.telemetryText(),
                    "°C", egtTempC / 800, RacingOrange, meterModifier, stacked)
                DpfMeter(com.torqueobd2.dash.i18n.tr("Hollín (ECU)", "Soot (ECU)"), pmGrams.telemetryText(1),
                    "g", pmGrams / 30, NeonGreen, meterModifier, stacked)
            }
            if (compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                meters(Modifier.weight(1f), true)
            } else meters(Modifier.fillMaxWidth(), false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DpfDetail(com.torqueobd2.dash.i18n.tr("Última regeneración", "Last regeneration"), "${distLastRegenKm.telemetryText()} km", Modifier.weight(1f))
                DpfDetail(com.torqueobd2.dash.i18n.tr("Combustible desde reg.", "Fuel since regen"), "${fuelLastRegenL.telemetryText(1)} L", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DpfDetail(com.torqueobd2.dash.i18n.tr("Nivel de aceite", "Oil level"), "${oilLevelMm.telemetryText(1)} mm", Modifier.weight(1f), TextWhite)
                DpfDetail(com.torqueobd2.dash.i18n.tr("Caudal de escape", "Exhaust flow"), "${exhaustFlowM3h.telemetryText()} m³/h", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DpfMeter(title: String, value: String, unit: String, progress: Float, color: Color,
    modifier: Modifier = Modifier, stacked: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (stacked) Text(title, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!stacked) Text(title, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 2)
            FittedValue(value, fontSize = 18.sp, color = TextWhite, modifier = Modifier.weight(if (stacked) 1f else 0.5f),
                textAlign = if (stacked) TextAlign.Start else TextAlign.End)
            Text(unit, color = color, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp), maxLines = 1)
        }
        TelemetryTrack(progress, color, Modifier.fillMaxWidth().height(3.dp))
    }
}

@Composable
private fun DpfDetail(title: String, value: String, modifier: Modifier = Modifier, color: Color = TextWhite) {
    Column(modifier) {
        Text(title, color = TextDim, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        FittedValue(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
    }
}
