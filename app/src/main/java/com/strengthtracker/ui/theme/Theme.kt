package com.strengthtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun StrengthTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
