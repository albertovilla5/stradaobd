package com.torqueobd2.dash.ui.automotive

import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.CockpitAccent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

/** Shared car sizing in dp with system font scaling; mobile retains its original presentation. */
val LocalCarMode = staticCompositionLocalOf { false }
val LocalOnCarModeChanged = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

fun carFontSize(size: TextUnit): TextUnit = when {
    size == TextUnit.Unspecified -> 24.sp
    size.value <= 14f -> 24.sp
    size.value <= 18f -> 28.sp
    size.value < 24f -> 32.sp
    else -> size
}

@Composable
fun uiLayoutScale(): Float = LocalDensity.current.fontScale.coerceAtLeast(1f) *
    if (LocalCarMode.current) 1.8f else 1f

@Composable
fun Modifier.carTouchTarget(): Modifier = if (LocalCarMode.current)
    Modifier.sizeIn(minWidth = 76.dp, minHeight = 76.dp).then(this) else this

@Composable
fun CarText(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null, fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified, textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val car = LocalCarMode.current
    val size = if (car) carFontSize(if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize) else fontSize
    val height = if (car) maxOf(size.value * 1.2f, if (lineHeight == TextUnit.Unspecified) 0f else lineHeight.value).sp else lineHeight
    Text(text, modifier, if (car && color == RacingOrange) CockpitAccent else color, size, fontStyle, fontWeight, fontFamily, letterSpacing,
        textDecoration, textAlign, height, overflow, softWrap, maxLines, minLines, onTextLayout, style)
}

@Composable
fun CarIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current) {
    Icon(imageVector, contentDescription,
        if (LocalCarMode.current) Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp).then(modifier) else modifier, if (LocalCarMode.current && tint == RacingOrange) CockpitAccent else tint)
}

@Composable
fun CarIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    IconButton(onClick, modifier.carTouchTarget(), enabled, colors, interactionSource, content)
}

@Composable
fun CarIconToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    IconToggleButton(checked, onCheckedChange, modifier.carTouchTarget(), enabled, colors, interactionSource, content)
}

@Composable
fun CarButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.carTouchTarget(), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)
}

@Composable
fun CarOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null, border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier.carTouchTarget(), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)
}

@Composable
fun CarTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier.carTouchTarget(), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)
}

@Composable
fun CarTab(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null) {
    Tab(selected, onClick, modifier.carTouchTarget(), enabled, text, icon,
        selectedContentColor, unselectedContentColor, interactionSource)
}

@Composable
fun CarDropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit,
    modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null) {
    DropdownMenuItem(text, onClick, modifier.carTouchTarget(), leadingIcon, trailingIcon,
        enabled, colors, contentPadding, interactionSource)
}

@Composable
fun CarFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null, shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null) {
    FilterChip(selected, onClick, label, modifier.carTouchTarget(), enabled, leadingIcon,
        trailingIcon, shape, colors, elevation, border, interactionSource)
}

@Composable
fun CarSuggestionChip(onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null, shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: BorderStroke? = SuggestionChipDefaults.suggestionChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null) {
    SuggestionChip(onClick, label, modifier.carTouchTarget(), enabled, icon,
        shape, colors, elevation, border, interactionSource)
}
