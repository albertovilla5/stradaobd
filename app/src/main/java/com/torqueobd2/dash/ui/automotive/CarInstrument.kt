package com.torqueobd2.dash.ui.automotive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.components.FittedValue
import com.torqueobd2.dash.ui.theme.*

/** Shared contemporary instruments: actual telemetry, custom scales, colours and history. */
@Composable
fun CarInstrument(widget: DashboardWidget, definition: ObdPid?, reading: ObdReading?,
    vehicle: VehicleState, connection: ConnectionState) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(widget.colorHex)) }.getOrDefault(CockpitAccent)
    val status = definition?.telemetryStatus(vehicle, connection) ?: SensorTelemetryStatus.MISSING
    val (minimum, maximum) = widget.scale(definition)
    val fraction = reading?.value?.takeIf { it.isFinite() }?.let {
        ((it - minimum) / (maximum - minimum).coerceAtLeast(.001f)).coerceIn(0f, 1f)
    } ?: 0f
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(CockpitSurface)) {
        val tall = maxHeight >= 220.dp * LocalDensity.current.fontScale + 40.dp
        val analog = widget.type == WidgetType.ANALOG_GAUGE && tall
        val short = maxHeight < 160.dp * LocalDensity.current.fontScale + 40.dp
        if (short) {
            val roomy = maxHeight >= 120.dp * LocalDensity.current.fontScale
            val unit = reading?.units ?: definition?.units.orEmpty()
            val unitMaxWidth = maxWidth * .5f
            val titleSize = if (maxWidth < 190.dp) 16.sp else 22.sp
            val unitSize = if (maxWidth < 190.dp) 16.sp else 22.sp
            Column(Modifier.fillMaxSize().padding(if (roomy) 8.dp else 4.dp)
                .semantics { contentDescription = "${widget.title}. ${widget.displayValue(reading)} $unit. ${status.label}" },
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(widget.title, Modifier.weight(1f), color = CockpitMuted, fontSize = titleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (unit.isNotEmpty()) Text(unit, Modifier.widthIn(max = unitMaxWidth), color = accent,
                        fontSize = unitSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FittedValue(widget.displayValue(reading), if (roomy) 64.sp else 48.sp, Modifier.weight(1f).fillMaxWidth(),
                    color = TextWhite, fontWeight = FontWeight.Medium, fitHeight = true)
                if (roomy && status != SensorTelemetryStatus.AVAILABLE && status != SensorTelemetryStatus.SIMULATED)
                    Text(status.label, color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                else if (status == SensorTelemetryStatus.AVAILABLE || status == SensorTelemetryStatus.SIMULATED) {
                    if (widget.type == WidgetType.SPARKLINE_CHART) Canvas(Modifier.fillMaxWidth().height(if (roomy) 16.dp else 8.dp)) {
                        drawHistory(vehicle.sparklineHistory[definition?.id].orEmpty(), accent,
                            minimum.takeIf { widget.hasCustomScale }, maximum.takeIf { widget.hasCustomScale })
                    } else if (widget.type == WidgetType.HORIZONTAL_BAR || widget.type == WidgetType.ANALOG_GAUGE)
                        CockpitTrack(fraction, accent, Modifier.fillMaxWidth().height(if (roomy) 6.dp else 4.dp))
                }
            }
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(widget.title, color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (analog) Canvas(Modifier.fillMaxSize()) {
                    val diameter = minOf(size.width, size.height) - 12.dp.toPx()
                    val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)
                    drawArc(CockpitElevated, 140f, 260f, false, origin, arcSize, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                    if (fraction > 0f) drawArc(accent, 140f, 260f * fraction, false, origin, arcSize,
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                    if (reading != null) {
                        val angle = Math.toRadians((140f + fraction * 260f).toDouble())
                        val center = Offset(size.width / 2, size.height / 2)
                        val direction = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
                        drawLine(TextWhite, center + direction * (diameter / 2 - 8.dp.toPx()),
                            center + direction * (diameter / 2 + 8.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                Column(Modifier.fillMaxWidth(if (analog) .72f else 1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FittedValue(widget.displayValue(reading), if (tall) 64.sp else 48.sp,
                        color = TextWhite, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text(reading?.units ?: definition?.units.orEmpty(), color = accent, fontSize = 24.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (widget.type == WidgetType.SPARKLINE_CHART) {
                val points = vehicle.sparklineHistory[definition?.id].orEmpty().filter { it.isFinite() }
                Canvas(Modifier.fillMaxWidth().weight(.65f)) {
                    if (points.size > 1) {
                        val low = if (widget.hasCustomScale) minimum else points.minOrNull() ?: 0f
                        val high = if (widget.hasCustomScale) maximum else points.maxOrNull() ?: 1f
                        val path = Path()
                        points.forEachIndexed { index, point ->
                            val x = index * size.width / (points.size - 1)
                            val y = size.height * (1f - ((point - low) / (high - low).coerceAtLeast(.001f)).coerceIn(0f, 1f))
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            } else if (widget.type == WidgetType.HORIZONTAL_BAR || widget.type == WidgetType.ANALOG_GAUGE && !analog) {
                CockpitTrack(fraction, accent, Modifier.fillMaxWidth().height(6.dp))
            }
            if (status != SensorTelemetryStatus.AVAILABLE && status != SensorTelemetryStatus.SIMULATED)
                Text(status.label, color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CockpitTrack(progress: Float, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val width = ((size.width - 11 * gap) / 12).coerceAtLeast(1f)
        val fraction = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        repeat(12) { index ->
            drawRoundRect(if (index < fraction * 12) accent else CockpitElevated,
                Offset(index * (width + gap), 0f), Size(width, size.height),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawHistory(history: List<Float>, accent: Color, scaleMin: Float?, scaleMax: Float?) {
    val points = history.filter { it.isFinite() }
    if (points.size < 2) return
    val low = scaleMin ?: points.minOrNull() ?: 0f
    val high = scaleMax ?: points.maxOrNull() ?: 1f
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = index * size.width / (points.size - 1)
        val y = size.height * (1f - ((point - low) / (high - low).coerceAtLeast(.001f)).coerceIn(0f, 1f))
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
}
