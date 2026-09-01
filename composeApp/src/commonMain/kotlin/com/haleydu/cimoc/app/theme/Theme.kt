package com.haleydu.cimoc.app.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1F5C52)
private val GreenLight = Color(0xFF7DB8AC)

@Composable
fun CimocTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColors(
            primary = GreenLight,
            primaryVariant = Color(0xFF5A9489),
            secondary = GreenLight,
            background = Color(0xFF121413),
            surface = Color(0xFF1A1C1B),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFE6E8E6),
            onSurface = Color(0xFFE6E8E6)
        )
    } else {
        lightColors(
            primary = Green,
            primaryVariant = Color(0xFF16453E),
            secondary = Green,
            background = Color(0xFFF6F7F6),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1A1C1B),
            onSurface = Color(0xFF1A1C1B)
        )
    }
    MaterialTheme(colors = colors, content = content)
}
