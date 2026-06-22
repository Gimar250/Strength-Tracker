package com.strengthtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

private val fontSizeMultiplier = 1.0f

val AppTypography = Typography(
    // Workout name on home screen
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = (22 * fontSizeMultiplier).sp,
        lineHeight = (28 * fontSizeMultiplier).sp
    ),
    // Exercise name on active screen
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = (30 * fontSizeMultiplier).sp,
        lineHeight = (36 * fontSizeMultiplier).sp
    ),
    // Timer countdown
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = (80 * fontSizeMultiplier).sp,
        lineHeight = (88 * fontSizeMultiplier).sp
    ),
    // Set counter, labels
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (16 * fontSizeMultiplier).sp,
        lineHeight = (24 * fontSizeMultiplier).sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (14 * fontSizeMultiplier).sp,
        lineHeight = (20 * fontSizeMultiplier).sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = (16 * fontSizeMultiplier).sp
    )
)

fun AppTypography(fontSizeSp: Int, density: Density): Typography {
    val base = 16f
    val ratio = fontSizeSp / base

    return Typography(
        titleLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = (22 * ratio).sp,
            lineHeight = (28 * ratio).sp
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.ExtraBold,
            fontSize = (30 * ratio).sp,
            lineHeight = (36 * ratio).sp
        ),
        displayLarge = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = (80 * ratio).sp,
            lineHeight = (88 * ratio).sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = (16 * ratio).sp,
            lineHeight = (24 * ratio).sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = (14 * ratio).sp,
            lineHeight = (20 * ratio).sp
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * ratio).sp
        )
    )
}
