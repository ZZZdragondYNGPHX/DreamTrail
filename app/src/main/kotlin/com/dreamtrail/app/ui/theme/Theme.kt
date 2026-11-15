package com.dreamtrail.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

object DreamTrailColors {
    val NightBackground = Color(0xFF12161E)
    val CardSurface = Color(0xFF1C2330)
    val CardElevated = Color(0xFF252E3E)
    val CardOutline = Color(0xFF394458)
    val ParchmentText = Color(0xFFF2ECE1)
    val MutedInk = Color(0xFFC7C0B3)
    val LanternAmber = Color(0xFFE2A959)
    val LanternInk = Color(0xFF1A140A)
    val SageMist = Color(0xFF7DB49C)
    val SageInk = Color(0xFF0E1D17)
    val CoralWarn = Color(0xFFD88078)
    val WarnSurface = Color(0xFF3A2122)
    val StarlightBlue = Color(0xFF8FB3D9)
}

private val DreamColorScheme = darkColorScheme(
    primary = DreamTrailColors.LanternAmber,
    onPrimary = DreamTrailColors.LanternInk,
    primaryContainer = Color(0xFF3E301C),
    onPrimaryContainer = DreamTrailColors.ParchmentText,
    secondary = DreamTrailColors.SageMist,
    onSecondary = DreamTrailColors.SageInk,
    secondaryContainer = Color(0xFF1E342B),
    onSecondaryContainer = DreamTrailColors.ParchmentText,
    tertiary = DreamTrailColors.StarlightBlue,
    background = DreamTrailColors.NightBackground,
    onBackground = DreamTrailColors.ParchmentText,
    surface = DreamTrailColors.CardSurface,
    onSurface = DreamTrailColors.ParchmentText,
    surfaceVariant = DreamTrailColors.CardElevated,
    onSurfaceVariant = DreamTrailColors.MutedInk,
    outline = DreamTrailColors.CardOutline,
    error = DreamTrailColors.CoralWarn,
    errorContainer = DreamTrailColors.WarnSurface,
    onErrorContainer = DreamTrailColors.ParchmentText
)

private val DreamTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun DreamTrailTheme(
    fontScaleLevel: Int = 0,
    content: @Composable () -> Unit
) {
    val extraScale = when (fontScaleLevel) {
        1 -> 1.12f
        2 -> 1.24f
        else -> 1.0f
    }
    val baseDensity = LocalDensity.current
    val mergedDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * extraScale
    )

    CompositionLocalProvider(LocalDensity provides mergedDensity) {
        MaterialTheme(
            colorScheme = DreamColorScheme,
            typography = DreamTypography,
            content = content
        )
    }
}
