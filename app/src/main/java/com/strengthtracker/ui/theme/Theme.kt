package com.strengthtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

// Force dark-only — no light mode for a gym tool
private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = GrayMuted,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = GraySurface,
    onSurface = White,
    surfaceVariant = GrayDark,
    onSurfaceVariant = GrayMuted,
    outline = GrayBorder
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun StrengthTrackerTheme(
    themeMode: String = "system",
    fontSizeSp: Int = 16,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    val density = LocalDensity.current
    val type = AppTypography(fontSizeSp, density)

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = type,
        content = content
    )
}
