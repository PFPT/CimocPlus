package com.haleydu.cimoc.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1F5C52)
private val GreenLight = Color(0xFF7DB8AC)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    secondary = GreenLight,
    onSecondary = Color.White,
    background = Color(0xFF121413),
    onBackground = Color(0xFFE6E8E6),
    surface = Color(0xFF1A1C1B),
    onSurface = Color(0xFFE6E8E6),
    surfaceVariant = Color(0xFF2A2D2C),
    onSurfaceVariant = Color(0xFFC4C7C5),
    error = Color(0xFFF2B8B5)
)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Green,
    onSecondary = Color.White,
    background = Color(0xFFF6F7F6),
    onBackground = Color(0xFF1A1C1B),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFE1E3E1),
    onSurfaceVariant = Color(0xFF444746),
    error = Color(0xFFBA1A1A)
)

@Composable
fun CimocTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
