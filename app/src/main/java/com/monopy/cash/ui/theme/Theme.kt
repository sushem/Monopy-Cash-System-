package com.monopy.cash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0F6B3F)
private val GreenLight = Color(0xFF6FD79B)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F1CE),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6355),
    background = Color(0xFFF7FBF6),
    surface = Color(0xFFF7FBF6),
    surfaceVariant = Color(0xFFDCE5DC)
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFFB8F1CE),
    secondary = Color(0xFFB4CCBB),
    background = Color(0xFF10140F),
    surface = Color(0xFF10140F),
    surfaceVariant = Color(0xFF3F493F)
)

@Composable
fun MonopyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
