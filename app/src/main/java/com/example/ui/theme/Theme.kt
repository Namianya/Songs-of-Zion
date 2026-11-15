package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light Sanctuary"),
    DARK("Dark Sanctuary")
}

private val SanctuaryLightColorScheme = lightColorScheme(
    primary = SanctuaryIndigo,
    onPrimary = Color.White,
    primaryContainer = SanctuaryIndigoContainer,
    onPrimaryContainer = SanctuaryOnIndigoContainer,
    secondary = SanctuaryGold,
    onSecondary = Color.White,
    secondaryContainer = SanctuaryGoldContainer,
    onSecondaryContainer = SanctuaryOnGoldContainer,
    tertiary = OfflineSage,
    onTertiary = Color.White,
    tertiaryContainer = OfflineSageContainer,
    onTertiaryContainer = Color(0xFF0A2E1B),
    background = SanctuaryAlabaster,
    onBackground = SanctuaryInk,
    surface = SanctuaryAlabaster,
    onSurface = SanctuaryInk,
    surfaceVariant = SanctuarySurfaceVariant,
    onSurfaceVariant = SanctuaryMutedPewter,
    surfaceContainerLowest = SanctuaryPaperWhite,
    surfaceContainerLow = Color(0xFFF6F4EE),
    surfaceContainer = SanctuaryPaperWhite,
    surfaceContainerHigh = Color(0xFFECE8DE),
    outline = SanctuaryOutline,
    outlineVariant = Color(0xFFE7E2D8),
    error = ErrorCrimson,
    onError = Color.White
)

private val SanctuaryDarkColorScheme = darkColorScheme(
    primary = SkyIndigo,
    onPrimary = Color(0xFF091B32),
    primaryContainer = SkyIndigoContainer,
    onPrimaryContainer = SkyOnIndigoContainer,
    secondary = SoftGold,
    onSecondary = Color(0xFF2B1F00),
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = SoftOnGoldContainer,
    tertiary = OfflineSageDark,
    onTertiary = Color(0xFF062112),
    tertiaryContainer = OfflineSageContainerDark,
    onTertiaryContainer = Color(0xFFC5F4DA),
    background = MidnightSlate,
    onBackground = WarmOffWhite,
    surface = MidnightSlate,
    onSurface = WarmOffWhite,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = DarkMutedSilver,
    surfaceContainerLowest = Color(0xFF0B0F16),
    surfaceContainerLow = MidnightElevated,
    surfaceContainer = MidnightElevated,
    surfaceContainerHigh = Color(0xFF1F2A3A),
    outline = DarkOutline,
    outlineVariant = Color(0xFF232C3B),
    error = ErrorCrimsonDark,
    onError = Color(0xFF601410)
)

val SanctuaryShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SongsOfZionTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) SanctuaryDarkColorScheme else SanctuaryLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SongsOfZionTypography,
        shapes = SanctuaryShapes,
        content = content
    )
}
