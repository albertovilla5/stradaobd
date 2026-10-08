package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import androidx.compose.material3.Text
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.carFontSize
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.torqueobd2.dash.ui.theme.*

/** Keeps weighted dashboards bounded, with scrolling only when their minimum readable size cannot fit. */
@Composable
fun ResponsiveCanvas(
    modifier: Modifier = Modifier,
    minimumHeight: (width: Dp, fontScale: Float) -> Dp,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val fontScale = uiLayoutScale()
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val contentHeight = maxOf(maxHeight, minimumHeight(maxWidth, fontScale))
        Box(
            Modifier.fillMaxWidth().verticalScroll(scrollState, enabled = contentHeight > maxHeight)
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(contentHeight), content = content)
        }
    }
}

/** Numeric readings shrink to the measured width; critical digits are never replaced by an ellipsis. */
@Composable
fun FittedValue(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textAlign: TextAlign = TextAlign.Start,
    letterSpacing: TextUnit = 0.sp,
    fillWidth: Boolean = true,
    fitHeight: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val displaySize = if (LocalCarMode.current) carFontSize(fontSize) else fontSize
    val baseStyle = LocalTextStyle.current.copy(fontSize = displaySize, fontWeight = fontWeight, letterSpacing = letterSpacing, lineHeight = TextUnit.Unspecified)
    BoxWithConstraints(modifier) {
        val measured = textMeasurer.measure(AnnotatedString(text), style = baseStyle, softWrap = false, maxLines = 1)
        val ratio = if (constraints.hasBoundedWidth && measured.size.width > 0) {
            (constraints.maxWidth.toFloat() / measured.size.width).coerceIn(0.01f, 1f)
        } else 1f
        val heightRatio = if (fitHeight && constraints.hasBoundedHeight && measured.size.height > 0)
            (constraints.maxHeight.toFloat() / measured.size.height).coerceIn(0.01f, 1f) else 1f
        val fittedRatio = minOf(ratio, heightRatio)
        Text(text, modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier, style = baseStyle.copy(fontSize = (displaySize.value * fittedRatio).sp),
            color = color, textAlign = textAlign, maxLines = 1, softWrap = false)
    }
}

/** Uses the host window's safe area: Compose 1.7 custom dialogs otherwise measure the whole display. */
@Composable
fun ResponsiveDialog(
    onDismissRequest: () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val host = LocalView.current
    var hostSize by remember(host) { mutableStateOf(IntSize(host.width, host.height)) }
    DisposableEffect(host) {
        val listener = android.view.View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            hostSize = IntSize(right - left, bottom - top)
        }
        host.addOnLayoutChangeListener(listener)
        onDispose { host.removeOnLayoutChangeListener(listener) }
    }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val safeArea = WindowInsets.safeDrawing
    val keyboard = WindowInsets.ime
    val width = with(density) {
        val hostWidth = hostSize.width.takeIf { it > 0 } ?: configuration.screenWidthDp.dp.roundToPx()
        (hostWidth - safeArea.getLeft(this, androidx.compose.ui.unit.LayoutDirection.Ltr) -
            safeArea.getRight(this, androidx.compose.ui.unit.LayoutDirection.Ltr)).coerceAtLeast(1).toDp()
    }
    val height = with(density) {
        val hostHeight = hostSize.height.takeIf { it > 0 } ?: configuration.screenHeightDp.dp.roundToPx()
        (hostHeight - safeArea.getTop(this) - maxOf(safeArea.getBottom(this), keyboard.getBottom(this)))
            .coerceAtLeast(1).toDp()
    }
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.width(width * 0.95f).heightIn(max = (height - 8.dp).coerceAtLeast(1.dp)),
            contentAlignment = Alignment.Center, content = content)
    }
}

/** Keeps editable forms scrollable while their actions remain outside the keyboard. */
@Composable
fun ResponsiveFormDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    validationMessage: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = uiLayoutScale()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    ResponsiveDialog(onDismiss) {
        val short = maxHeight < 240.dp * scale
        val hideHeading = keyboardVisible && maxHeight < 180.dp * scale
        Surface(Modifier.widthIn(max = 700.dp).fillMaxWidth().height(minOf(maxHeight, 620.dp * scale)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = CardBackground) {
            Column(Modifier.fillMaxSize().padding(if (short) 8.dp else 16.dp)) {
                if (!hideHeading) {
                    com.torqueobd2.dash.ui.automotive.CarText(title, color = TextWhite, fontSize = if (short) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
                validationMessage?.let {
                    com.torqueobd2.dash.ui.automotive.CarText(it, color = DangerRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(if (short) 2.dp else 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onDismiss, Modifier.weight(1f), contentPadding = PaddingValues(if (short) 4.dp else 8.dp)) {
                        FittedValue("Cancelar", 13.sp, color = TextMuted, textAlign = TextAlign.Center)
                    }
                    Button(onConfirm, Modifier.weight(1f), contentPadding = PaddingValues(if (short) 4.dp else 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)) {
                        FittedValue(confirmLabel, 13.sp, color = Color.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
