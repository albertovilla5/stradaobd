package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun AnalogGauge(
    value: Float, minValue: Float = 0f, maxValue: Float = 8000f,
    title: String = "RPM", unit: String = "RPM", redlineStart: Float = 6000f,
    majorTickStep: Float = 1000f, primaryColor: Color = RacingOrange, modifier: Modifier = Modifier,
    formattedValue: String? = null
) {
    val car = LocalCarMode.current
    val span = (maxValue - minValue).takeIf { it.isFinite() && it > 0f } ?: 1f
    val safeValue = if (value.isFinite()) value else minValue
    val animated by animateFloatAsState(safeValue.coerceIn(minValue, minValue + span), tween(180), label = "racingGauge")
    val progress = ((animated - minValue) / span).coerceIn(0f, 1f)
    val tickPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) } }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight)
        val scale = (diameter.value / 220f).coerceIn(0.45f, 2.2f)
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension * 0.45f
                val arcSize = Size(radius * 2, radius * 2)
                val arcOrigin = center - Offset(radius, radius)
                drawCircle(Brush.radialGradient(listOf(CardBackground, DarkSurface), center, radius), radius, center)
                drawArc(BorderDark.copy(alpha = 0.6f), 120f, 300f, false, arcOrigin, arcSize, style = Stroke(5.dp.toPx() * scale))
                val redStart = ((redlineStart - minValue) / span).coerceIn(0f, 1f)
                drawArc(DangerRed.copy(alpha = 0.3f), 120f + redStart * 300f, (1 - redStart) * 300f, false,
                    arcOrigin, arcSize, style = Stroke(5.dp.toPx() * scale))
                drawArc(primaryColor.copy(alpha = 0.12f), 120f, progress * 300f, false, arcOrigin, arcSize,
                    style = Stroke(12.dp.toPx() * scale, cap = StrokeCap.Round))
                drawArc(primaryColor, 120f, progress * 300f, false, arcOrigin, arcSize,
                    style = Stroke(4.dp.toPx() * scale, cap = StrokeCap.Round))
                val majorSteps = if (majorTickStep.isFinite() && majorTickStep > 0f)
                    (span / majorTickStep).roundToInt().coerceIn(1, 16) else 5
                val minorSteps = majorSteps * 4
                for (i in 0..minorSteps) {
                    val fraction = i.toFloat() / minorSteps
                    val angle = Math.toRadians((120 + fraction * 300).toDouble())
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val major = i % 4 == 0
                    drawLine(if (fraction >= redStart) DangerRed.copy(alpha = 0.8f) else TextMuted.copy(alpha = if (major) 0.8f else 0.3f),
                        center + direction * (radius * if (major) 0.80f else 0.86f), center + direction * (radius * 0.91f),
                        strokeWidth = (if (major) 1.5f else 0.8f).dp.toPx() * scale)
                    if (major && diameter.value >= if (car) 300 else 155) {
                        val tick = minValue + fraction * span
                        val label = when {
                            kotlin.math.abs(tick) >= 1_000_000_000f -> String.format(Locale.US, "%.1fG", tick / 1_000_000_000f)
                            kotlin.math.abs(tick) >= 1_000_000f -> String.format(Locale.US, "%.1fM", tick / 1_000_000f)
                            kotlin.math.abs(tick) >= 1000f -> String.format(Locale.US, "%.1fk", tick / 1000f)
                            majorTickStep < 1f -> {
                                val precision = if (majorTickStep > 0f) (-floor(log10(majorTickStep.toDouble()))).toInt().coerceAtLeast(1) else 7
                                if (precision > 6) String.format(Locale.US, "%.1e", tick)
                                else String.format(Locale.US, "%.${precision}f", tick)
                            }
                            else -> String.format(Locale.US, "%.0f", tick)
                        }
                        val point = center + direction * (radius * 0.67f)
                        tickPaint.textSize = if (car) 24.sp.toPx() else 10.sp.toPx() * scale
                        tickPaint.color = (if (fraction >= redStart) DangerRed else TextMuted).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(label, point.x, point.y + tickPaint.textSize * 0.35f, tickPaint)
                    }
                }
                val angle = Math.toRadians((120 + progress * 300).toDouble())
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                if (value.isFinite()) drawLine(TextWhite, center + direction * radius * 0.75f, center + direction * radius * 1.02f,
                    strokeWidth = 2.5.dp.toPx() * scale, cap = StrokeCap.Round)
            }
            Column(Modifier.width(diameter * 0.68f).padding(top = (diameter.value * 0.14f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = TextMuted, fontSize = (10 * scale).coerceAtLeast(9f).sp, letterSpacing = 0.3.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FittedValue(formattedValue ?: if (!value.isFinite()) "—" else if (maxValue >= 1000 || majorTickStep >= 1) String.format(Locale.US, "%.0f", safeValue) else String.format(Locale.US, "%.1f", safeValue),
                    color = TextWhite, fontSize = (38 * scale).coerceAtLeast(22f).sp, letterSpacing = (-1.5 * scale).sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(unit, color = primaryColor, fontSize = (10 * scale).coerceAtLeast(9f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
