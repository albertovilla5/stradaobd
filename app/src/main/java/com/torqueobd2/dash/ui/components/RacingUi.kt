package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.CarText as Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*

@Composable
fun Modifier.racingPanel(radius: Dp = 18.dp): Modifier = if (LocalCarMode.current)
    clip(RoundedCornerShape(28.dp)).background(CockpitSurface)
else clip(RoundedCornerShape(radius))
    .background(Brush.linearGradient(listOf(CardBackground, DarkSurface)))
    .border(1.dp, Brush.linearGradient(listOf(BorderDark, BorderDark.copy(alpha = 0.3f))), RoundedCornerShape(radius))

@Composable
fun RacingPanel(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.racingPanel().padding(padding), content = content)
}

@Composable
fun RacingHeader(title: String, subtitle: String, modifier: Modifier = Modifier, eyebrow: String = com.torqueobd2.dash.i18n.tr("STRADA / TELEMETRÍA", "STRADA / TELEMETRY")) {
    Column(modifier.fillMaxWidth()) {
        Text(eyebrow, color = RacingOrange, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(title, color = TextWhite, fontSize = 23.sp, letterSpacing = (-0.6).sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun StatusPill(text: String, color: Color = NeonGreen, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun TelemetryMetric(
    title: String, value: String, unit: String, modifier: Modifier = Modifier,
    accent: Color = RacingOrange, progress: Float? = null, subtitle: String? = null
) {
    BoxWithConstraints(modifier.racingPanel().padding(horizontal = 14.dp, vertical = 10.dp)) {
        val compact = maxHeight < 110.dp * uiLayoutScale()
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                Spacer(Modifier.width(7.dp))
                Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FittedValue(value, color = TextWhite, fontSize = if (compact) 27.sp else 36.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, modifier = Modifier.weight(1f))
                Text(unit, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 5.dp), maxLines = 1)
            }
            if (progress != null) {
                TelemetryTrack(progress, accent, Modifier.fillMaxWidth().height(3.dp))
            } else if (!compact && subtitle != null) {
                Text(subtitle, color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TelemetryTrack(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(3.dp))) {
        drawRect(BorderDark)
        val fraction = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        drawRect(color, size = size.copy(width = size.width * fraction))
    }
}

@Composable
fun RacingMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        val cx = size.width * 0.5f
        val cy = size.height * 0.48f
        val outerR = side * 0.42f
        val innerR = side * 0.31f
        val outerStroke = (side * 0.085f).coerceAtLeast(1.6f)
        val innerStroke = (side * 0.05f).coerceAtLeast(1.1f)

        // Inner cyan telemetry arc
        drawArc(
            color = NeonCyan,
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(cx - innerR, cy - innerR),
            size = Size(innerR * 2f, innerR * 2f),
            style = Stroke(width = innerStroke, cap = StrokeCap.Round)
        )
        // Outer white tachometer arc
        drawArc(
            color = TextWhite,
            startAngle = 145f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - outerR, cy - outerR),
            size = Size(outerR * 2f, outerR * 2f),
            style = Stroke(width = outerStroke, cap = StrokeCap.Butt)
        )
        // Redline orange arc
        drawArc(
            color = RacingOrange,
            startAngle = 305f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - outerR, cy - outerR),
            size = Size(outerR * 2f, outerR * 2f),
            style = Stroke(width = outerStroke * 1.12f, cap = StrokeCap.Butt)
        )
        // Perspective road rails ('Strada')
        val roadTopY = cy + side * 0.11f
        val roadBottomY = cy + side * 0.46f
        drawLine(
            color = TextWhite,
            start = Offset(cx - side * 0.09f, roadTopY),
            end = Offset(cx - side * 0.28f, roadBottomY),
            strokeWidth = innerStroke * 1.15f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = TextWhite,
            start = Offset(cx + side * 0.09f, roadTopY),
            end = Offset(cx + side * 0.28f, roadBottomY),
            strokeWidth = innerStroke * 1.15f,
            cap = StrokeCap.Round
        )
        // Dashed orange road centerline
        drawLine(
            color = RacingOrange,
            start = Offset(cx, roadTopY + side * 0.02f),
            end = Offset(cx, roadTopY + side * 0.14f),
            strokeWidth = innerStroke * 1.1f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = RacingOrange,
            start = Offset(cx, roadTopY + side * 0.22f),
            end = Offset(cx, roadBottomY),
            strokeWidth = innerStroke * 1.3f,
            cap = StrokeCap.Round
        )
        // Tachometer needle pointing to upper-right redline apex
        drawLine(
            color = RacingOrange,
            start = Offset(cx - side * 0.07f, cy + side * 0.05f),
            end = Offset(cx + outerR * 0.78f, cy - outerR * 0.62f),
            strokeWidth = outerStroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawCircle(color = RacingOrange, radius = side * 0.075f, center = Offset(cx, cy))
    }
}

