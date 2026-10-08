package com.torqueobd2.dash.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun raceType(size: Int, weight: FontWeight, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp,
    letterSpacing = spacing.sp, fontFeatureSettings = "tnum"
)

val Typography = Typography(
    displayLarge = raceType(64, FontWeight.Black, -2f),
    displayMedium = raceType(48, FontWeight.Bold, -1.5f),
    displaySmall = raceType(36, FontWeight.Bold, -1f),
    headlineLarge = raceType(28, FontWeight.Bold, -0.5f),
    headlineMedium = raceType(24, FontWeight.Bold, -0.4f),
    headlineSmall = raceType(20, FontWeight.Bold, -0.3f),
    titleLarge = raceType(18, FontWeight.SemiBold),
    titleMedium = raceType(15, FontWeight.SemiBold),
    titleSmall = raceType(13, FontWeight.SemiBold, 0.2f),
    bodyLarge = raceType(16, FontWeight.Normal),
    bodyMedium = raceType(14, FontWeight.Normal),
    bodySmall = raceType(12, FontWeight.Normal),
    labelLarge = raceType(13, FontWeight.SemiBold, 0.2f),
    labelMedium = raceType(11, FontWeight.SemiBold, 0.5f),
    labelSmall = raceType(10, FontWeight.SemiBold, 0.8f)
)


val CarTypography = Typography(
    displayLarge = raceType(96, FontWeight.Medium),
    displayMedium = raceType(72, FontWeight.Medium),
    displaySmall = raceType(48, FontWeight.Medium),
    headlineLarge = raceType(40, FontWeight.Medium),
    headlineMedium = raceType(36, FontWeight.Medium),
    headlineSmall = raceType(32, FontWeight.Medium),
    titleLarge = raceType(32, FontWeight.Medium),
    titleMedium = raceType(28, FontWeight.Medium),
    titleSmall = raceType(24, FontWeight.Medium),
    bodyLarge = raceType(24, FontWeight.Normal),
    bodyMedium = raceType(24, FontWeight.Normal),
    bodySmall = raceType(24, FontWeight.Normal),
    labelLarge = raceType(24, FontWeight.Medium),
    labelMedium = raceType(24, FontWeight.Medium),
    labelSmall = raceType(24, FontWeight.Medium)
)
