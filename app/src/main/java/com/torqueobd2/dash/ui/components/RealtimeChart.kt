package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.CarText as Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*

@Composable
fun RealtimeChart(
    title: String, points: List<Float>, currentValue: String, unit: String,
    lineColor: Color = NeonCyan, chartHeight: Dp = 100.dp, modifier: Modifier = Modifier,
    headerEndPadding: Dp = 0.dp, scaleMin: Float? = null, scaleMax: Float? = null
) {
    BoxWithConstraints(modifier.fillMaxWidth().racingPanel().padding(14.dp)) {
        val bounded = constraints.hasBoundedHeight
        val validPoints = points.filter { it.isFinite() }
        Column(if (bounded) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(end = headerEndPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                FittedValue("$currentValue $unit", color = TextWhite, fontSize = 14.sp,
                    modifier = Modifier.weight(0.85f), textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(12.dp))
            Box(if (bounded) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().height(chartHeight)) {
                Canvas(Modifier.fillMaxSize()) {
                    repeat(4) { row ->
                        val y = size.height * row / 3f
                        drawLine(BorderDark.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), 0.7.dp.toPx())
                    }
                    if (validPoints.size >= 2) {
                        val customScale = scaleMin != null && scaleMax != null && scaleMin.isFinite() && scaleMax.isFinite() && scaleMin < scaleMax
                        val high = if (customScale) scaleMax!! else validPoints.maxOrNull() ?: 1f
                        val low = if (customScale) scaleMin!! else validPoints.minOrNull() ?: 0f
                        val range = if (customScale) high - low else (high - low).coerceAtLeast(1f)
                        val path = Path()
                        validPoints.forEachIndexed { index, point ->
                            val x = size.width * index / (validPoints.size - 1)
                            val y = size.height * 0.92f - ((point - low) / range).coerceIn(0f, 1f) * size.height * 0.82f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        val fill = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)))
                        drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        val y = size.height * 0.92f - ((validPoints.last() - low) / range).coerceIn(0f, 1f) * size.height * 0.82f
                        drawCircle(lineColor, 3.dp.toPx(), Offset(size.width - 3.dp.toPx(), y))
                    }
                }
                if (validPoints.size < 2) Text(com.torqueobd2.dash.i18n.tr("Esperando telemetría", "Waiting for telemetry"), color = TextDim, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
