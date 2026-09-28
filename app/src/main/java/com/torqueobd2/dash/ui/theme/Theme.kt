package com.torqueobd2.dash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
    error = DangerRed
)

@Composable
fun TorqueDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
