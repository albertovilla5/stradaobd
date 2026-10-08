package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.data.model.telemetryText

@Composable
fun DigitalHudGauge(
    speedKmh: Float, rpm: Float, maxRpm: Float = 5500f, modifier: Modifier = Modifier,
    showShiftLights: Boolean = true, coolantTempC: Float = Float.NaN, batteryVoltage: Float = Float.NaN,
    speedStatus: String = "Pendiente de consultar", rpmStatus: String = "Pendiente de consultar"
) {
    BoxWithConstraints(modifier.racingPanel(12.dp).drawBehind {
        if (showShiftLights) {
            // Líneas de carrocería suaves: acento racing sin tapar la telemetría.
            repeat(4) { index ->
                val x = size.width * 0.64f + index * 22.dp.toPx()
                drawLine(RacingOrange.copy(alpha = 0.035f), Offset(x, size.height),
                    Offset(x + size.height * 0.5f, 0f), strokeWidth = 12.dp.toPx())
            }
            drawLine(RacingOrange, Offset(0f, 20.dp.toPx()), Offset(0f, size.height - 20.dp.toPx()), strokeWidth = 4.dp.toPx())
        }
    }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        val compact = maxHeight < 180.dp * uiLayoutScale()
        val tight = maxWidth < 220.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            if (showShiftLights) ShiftLightsBar(rpm / maxRpm.coerceAtLeast(1000f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VELOCIDAD", color = if (showShiftLights) RacingOrange else TextMuted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                if (showShiftLights && !tight) Text("km/h", color = TextDim, fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
                    FittedValue(speedKmh.telemetryText(), color = TextWhite, fontSize = if (compact) 56.sp else 88.sp,
                        letterSpacing = (-3).sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                }
                if (!speedKmh.isFinite()) Text(speedStatus, color = TextMuted, fontSize = 10.sp,
                    modifier = Modifier.weight(0.8f).padding(start = 8.dp))
            }
            if (showShiftLights) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FittedValue(if (rpm.isFinite()) "${rpm.telemetryText()} RPM" else "RPM · $rpmStatus", color = RacingOrange, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    FittedValue("${coolantTempC.telemetryText()} °C  /  ${batteryVoltage.telemetryText()} V", color = TextMuted,
                        fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                TelemetryTrack(rpm / maxRpm.coerceAtLeast(1000f), RacingOrange, Modifier.fillMaxWidth().height(3.dp))
            } else {
                Text("km/h", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ShiftLightsBar(rpmFraction: Float, modifier: Modifier = Modifier, totalLeds: Int = 16) {
    val fraction = if (rpmFraction.isFinite()) rpmFraction.coerceIn(0f, 1f) else 0f
    val active = (fraction * totalLeds).toInt()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(totalLeds) { index ->
            val color = when { index >= totalLeds * 0.8f -> DangerRed; index >= totalLeds * 0.55f -> WarningAmber; else -> NeonGreen }
            Canvas(Modifier.weight(1f).height(7.dp)) {
                val segment = Path().apply {
                    moveTo(size.width * 0.2f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width * 0.8f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(segment, if (index < active) color else BorderDark.copy(alpha = 0.55f))
            }
        }
    }
}
