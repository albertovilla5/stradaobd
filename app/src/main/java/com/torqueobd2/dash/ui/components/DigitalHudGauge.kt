package com.torqueobd2.dash.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber

/**
 * Indicador de alto contraste estilo display HUD de carreras (GT3 / F1),
 * con barra de luces secuenciales de cambio de marcha (Shift Lights) y velocidad gigante.
 */
@Composable
fun DigitalHudGauge(
    speedKmh: Float,
    rpm: Float,
    maxRpm: Float = 5500f,
    currentGear: Int = 1,
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(targetValue = rpm, label = "hudRpm")
    val rpmFraction = (animatedRpm / maxRpm).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFF28303D), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tira de luces secuenciales de cambio (16 LEDs: 6 verdes, 5 amarillos, 5 rojos)
        ShiftLightsBar(rpmFraction = rpmFraction)

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de Marcha (Gear)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MARCHA",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222834))
                        .border(1.5.dp, RacingOrange, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (speedKmh < 1f && rpm < 1000f) "N" else currentGear.toString(),
                        color = RacingOrange,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Velocímetro digital grande
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = speedKmh.toInt().toString(),
                        color = TextWhite,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KM/H",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Tacómetro digital
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MOTOR",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = animatedRpm.toInt().toString(),
                    color = if (rpmFraction > 0.85f) DangerRed else TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "RPM",
                    color = RacingOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra horizontal de progreso de RPM con zona roja
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF222834))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rpmFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            rpmFraction > 0.85f -> DangerRed
                            rpmFraction > 0.65f -> WarningAmber
                            else -> NeonGreen
                        }
                    )
            )
        }
    }
}

@Composable
private fun ShiftLightsBar(rpmFraction: Float) {
    val totalLeds = 14
    val activeLeds = (rpmFraction * totalLeds).toInt().coerceIn(0, totalLeds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0 until totalLeds) {
            val isActive = i < activeLeds
            val ledColor = when {
                i >= 10 -> DangerRed
                i >= 6 -> WarningAmber
                else -> NeonGreen
            }

            Box(
                modifier = Modifier
                    .size(16.dp, 10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) ledColor else Color(0xFF222834))
                    .border(
                        0.5.dp,
                        if (isActive) ledColor.copy(alpha = 0.8f) else Color(0xFF323B4A),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
