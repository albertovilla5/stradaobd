package com.torqueobd2.dash.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import com.torqueobd2.dash.ui.automotive.*
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = RacingOrange,
    secondary = NeonCyan,
    tertiary = NeonGreen,
    background = CarbonBlack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = CarbonBlack,
    onSecondary = CarbonBlack,
    onTertiary = CarbonBlack,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.6f),
    primaryContainer = RacingOrange.copy(alpha = 0.16f),
    onPrimaryContainer = RacingOrange,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextWhite,
    error = DangerRed,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant
)

@Composable
fun TorqueDashboardTheme(
    carMode: Boolean = true,
    onCarModeChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCarMode provides carMode,
        LocalOnCarModeChanged provides onCarModeChanged,
        LocalMinimumInteractiveComponentSize provides if (carMode) 76.dp else 48.dp
    ) {
        MaterialTheme(
            colorScheme = if (carMode) DarkColorScheme.copy(
                primary = CockpitAccent, secondary = CockpitTeal, background = CockpitBackground,
                surface = CockpitSurface, surfaceVariant = CockpitElevated,
                surfaceContainer = CockpitSurface, surfaceContainerHigh = CockpitElevated,
                primaryContainer = CockpitElevated, onPrimaryContainer = CockpitAccent
            ) else DarkColorScheme,
            typography = if (carMode) CarTypography else Typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp)
            ),
            content = { CompositionLocalProvider(LocalContentColor provides TextWhite, content = content) }
        )
    }
}
