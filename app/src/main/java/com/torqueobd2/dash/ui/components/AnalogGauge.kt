package com.torqueobd2.dash.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.GaugeRedline
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Medidor circular analógico autoescalable y adaptativo para cualquier resolución y pantalla.
 * Utiliza BoxWithConstraints para ajustar grosores, tamaños de aguja y tipografías proporcionalmente.
 */
@Composable
fun AnalogGauge(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 8000f,
    title: String = "RPM",
    unit: String = "RPM",
    redlineStart: Float = 6000f,
    majorTickStep: Float = 1000f,
    primaryColor: Color = RacingOrange,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(minValue, maxValue),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "gaugeNeedle"
    )

    val startAngle = 135f
    val sweepAngle = 270f
    val progress = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val needleAngleDeg = startAngle + (progress * sweepAngle)

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val sizeDp = min(maxWidth.value, maxHeight.value)
        // Escalar tipografías proporcionalmente a la resolución / tamaño disponible
        val scale = (sizeDp / 180f).coerceIn(0.6f, 2.2f)
        val titleFontSize = (10f * scale).sp
        val valueFontSize = (20f * scale).sp
        val unitFontSize = (10f * scale).sp
        val topPadding = (sizeDp * 0.36f).dp

        Canvas(modifier = Modifier.fillMaxSize().padding((sizeDp * 0.05f).dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - (8f * scale)

            // Fondo circular con gradiente radial
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B202A), CarbonBlack),
                    center = center,
                    radius = radius + (8f * scale)
                ),
                radius = radius + (4f * scale),
                center = center
            )

            // Borde exterior
            drawCircle(
                color = Color(0xFF2C3542),
                radius = radius + (4f * scale),
                center = center,
                style = Stroke(width = 2.5f * scale)
            )

            // Arco base
            drawArc(
                color = Color(0xFF1E2530),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8f * scale, cap = StrokeCap.Round)
            )

            // Arco activo
            val activeSweep = (progress * sweepAngle).coerceAtLeast(1f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, primaryColor, GaugeRedline),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8f * scale, cap = StrokeCap.Round)
            )

            // Arco de zona roja
            if (redlineStart < maxValue) {
                val redlineProgress = ((redlineStart - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                val redlineSweep = sweepAngle * (1f - redlineProgress)
                val redlineStartAngle = startAngle + (redlineProgress * sweepAngle)

                drawArc(
                    color = DangerRed.copy(alpha = 0.45f),
                    startAngle = redlineStartAngle,
                    sweepAngle = redlineSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 11f * scale, cap = StrokeCap.Butt)
                )
            }

            // Marcas radiales
            val numSteps = ((maxValue - minValue) / majorTickStep).toInt().coerceAtLeast(1)
            for (step in 0..numSteps) {
                val tickVal = minValue + step * majorTickStep
                val tickProgress = (tickVal - minValue) / (maxValue - minValue)
                val tickAngle = startAngle + (tickProgress * sweepAngle)
                val rad = (tickAngle * PI / 180.0).toFloat()

                val isRedlineTick = tickVal >= redlineStart
                val tickColor = if (isRedlineTick) GaugeRedline else TextMuted
                val tickLength = 10f * scale

                val outerX = center.x + (radius - 6f * scale) * cos(rad)
                val outerY = center.y + (radius - 6f * scale) * sin(rad)
                val innerX = center.x + (radius - 6f * scale - tickLength) * cos(rad)
                val innerY = center.y + (radius - 6f * scale - tickLength) * sin(rad)

                drawLine(
                    color = tickColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = 2f * scale,
                    cap = StrokeCap.Round
                )
            }

            // Aguja indicadora
            val needleRad = (needleAngleDeg * PI / 180.0).toFloat()
            val needleLength = radius - (12f * scale)
            val needleTipX = center.x + needleLength * cos(needleRad)
            val needleTipY = center.y + needleLength * sin(needleRad)

            val tailLength = 14f * scale
            val tailX = center.x - tailLength * cos(needleRad)
            val tailY = center.y - tailLength * sin(needleRad)

            drawLine(
                color = primaryColor,
                start = Offset(tailX, tailY),
                end = Offset(needleTipX, needleTipY),
                strokeWidth = 3.5f * scale,
                cap = StrokeCap.Round
            )

            // Eje central
            drawCircle(
                color = Color(0xFF333E4D),
                radius = 9f * scale,
                center = center
            )
            drawCircle(
                color = primaryColor,
                radius = 5f * scale,
                center = center
            )
        }

        // Display central integrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = topPadding)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = if (maxValue >= 1000f) {
                    animatedValue.toInt().toString()
                } else {
                    String.format("%.1f", animatedValue)
                },
                color = TextWhite,
                fontSize = valueFontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = unit,
                color = primaryColor,
                fontSize = unitFontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
