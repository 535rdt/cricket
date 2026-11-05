package com.cricket.scoreboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TurfGreenLight,
    onPrimary = Color.Black,
    primaryContainer = TurfGreenDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = PitchGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF422C08),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    error = WicketRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TurfGreenDark,
    onPrimary = Color.White,
    primaryContainer = TurfGreenMedium,
    onPrimaryContainer = Color.White,
    secondary = BoundaryGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceElevated,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = WicketRed,
    onError = Color.White
)

@Composable
fun CricketScoreboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
