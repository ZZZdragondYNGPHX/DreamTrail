package com.dreamtrail.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import java.util.Calendar

enum class ThemeMode(val id: String, val displayName: String) {
    NIGHT_INK("NIGHT_INK", "夜墨星灯"),
    DAY_PARCHMENT("DAY_PARCHMENT", "晴岚羊皮"),
    AUTO_SOLUNA("AUTO_SOLUNA", "随昼夜自动");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: NIGHT_INK
    }
}

data class SolunaPalette(
    val nightBackground: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val cardOutline: Color,
    val parchmentText: Color,
    val mutedInk: Color,
    val lanternAmber: Color,
    val lanternInk: Color,
    val sageMist: Color,
    val sageInk: Color,
    val coralWarn: Color,
    val warnSurface: Color,
    val starlightBlue: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val isLight: Boolean
)

val NightInkPalette = SolunaPalette(
    nightBackground = Color(0xFF12161E),
    cardSurface = Color(0xFF1C2330),
    cardElevated = Color(0xFF252E3E),
    cardOutline = Color(0xFF394458),
    parchmentText = Color(0xFFF2ECE1),
    mutedInk = Color(0xFFC7C0B3),
    lanternAmber = Color(0xFFE2A959),
    lanternInk = Color(0xFF1A140A),
    sageMist = Color(0xFF7DB49C),
    sageInk = Color(0xFF0E1D17),
    coralWarn = Color(0xFFD88078),
    warnSurface = Color(0xFF3A2122),
    starlightBlue = Color(0xFF8FB3D9),
    primaryContainer = Color(0xFF3E301C),
    secondaryContainer = Color(0xFF1E342B),
    isLight = false
)

val DayParchmentPalette = SolunaPalette(
    nightBackground = Color(0xFFF4EDE1),
    cardSurface = Color(0xFFFDF8F0),
    cardElevated = Color(0xFFEBE2D1),
    cardOutline = Color(0xFFCEBFAA),
    parchmentText = Color(0xFF2A231B),
    mutedInk = Color(0xFF635749),
    lanternAmber = Color(0xFFB56E1B),
    lanternInk = Color(0xFFFFFBF4),
    sageMist = Color(0xFF2F7259),
    sageInk = Color(0xFFF7FBF8),
    coralWarn = Color(0xFFB94E46),
    warnSurface = Color(0xFFF7E0DD),
    starlightBlue = Color(0xFF3B6399),
    primaryContainer = Color(0xFFF3DFC1),
    secondaryContainer = Color(0xFFD9EDE3),
    isLight = true
)

object DreamTrailColors {
    var activePalette by mutableStateOf(NightInkPalette)
        internal set

    val NightBackground: Color get() = activePalette.nightBackground
    val CardSurface: Color get() = activePalette.cardSurface
    val CardElevated: Color get() = activePalette.cardElevated
    val CardOutline: Color get() = activePalette.cardOutline
    val ParchmentText: Color get() = activePalette.parchmentText
    val MutedInk: Color get() = activePalette.mutedInk
    val LanternAmber: Color get() = activePalette.lanternAmber
    val LanternInk: Color get() = activePalette.lanternInk
    val SageMist: Color get() = activePalette.sageMist
    val SageInk: Color get() = activePalette.sageInk
    val CoralWarn: Color get() = activePalette.coralWarn
    val WarnSurface: Color get() = activePalette.warnSurface
    val StarlightBlue: Color get() = activePalette.starlightBlue
}

private fun buildColorScheme(p: SolunaPalette) = if (p.isLight) {
    lightColorScheme(
        primary = p.lanternAmber,
        onPrimary = p.lanternInk,
        primaryContainer = p.primaryContainer,
        onPrimaryContainer = p.parchmentText,
        secondary = p.sageMist,
        onSecondary = p.sageInk,
        secondaryContainer = p.secondaryContainer,
        onSecondaryContainer = p.parchmentText,
        tertiary = p.starlightBlue,
        background = p.nightBackground,
        onBackground = p.parchmentText,
        surface = p.cardSurface,
        onSurface = p.parchmentText,
        surfaceVariant = p.cardElevated,
        onSurfaceVariant = p.mutedInk,
        outline = p.cardOutline,
        error = p.coralWarn,
        errorContainer = p.warnSurface,
        onErrorContainer = p.parchmentText
    )
} else {
    darkColorScheme(
        primary = p.lanternAmber,
        onPrimary = p.lanternInk,
        primaryContainer = p.primaryContainer,
        onPrimaryContainer = p.parchmentText,
        secondary = p.sageMist,
        onSecondary = p.sageInk,
        secondaryContainer = p.secondaryContainer,
        onSecondaryContainer = p.parchmentText,
        tertiary = p.starlightBlue,
        background = p.nightBackground,
        onBackground = p.parchmentText,
        surface = p.cardSurface,
        onSurface = p.parchmentText,
        surfaceVariant = p.cardElevated,
        onSurfaceVariant = p.mutedInk,
        outline = p.cardOutline,
        error = p.coralWarn,
        errorContainer = p.warnSurface,
        onErrorContainer = p.parchmentText
    )
}

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
    themeMode: ThemeMode = ThemeMode.NIGHT_INK,
    isFocusActive: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = when (themeMode) {
        ThemeMode.NIGHT_INK -> NightInkPalette
        ThemeMode.DAY_PARCHMENT -> DayParchmentPalette
        ThemeMode.AUTO_SOLUNA -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (isFocusActive || hour in 6..18) DayParchmentPalette else NightInkPalette
        }
    }
    DreamTrailColors.activePalette = palette

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
            colorScheme = buildColorScheme(palette),
            typography = DreamTypography,
            content = content
        )
    }
}
