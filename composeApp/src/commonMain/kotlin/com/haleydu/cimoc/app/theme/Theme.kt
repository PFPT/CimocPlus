package com.haleydu.cimoc.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.haleydu.cimoc.shared.model.Appearance

private data class Palette(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    val background: Color,
    val surface: Color,
    val variant: Color,
    val onSurface: Color,
    val onVariant: Color,
    val error: Color
)

private fun scheme(dark: Boolean, palette: Palette): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.container,
            onPrimaryContainer = palette.onContainer,
            secondary = palette.primary,
            onSecondary = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onSurface,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.variant,
            onSurfaceVariant = palette.onVariant,
            error = palette.error
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.container,
            onPrimaryContainer = palette.onContainer,
            secondary = palette.primary,
            onSecondary = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onSurface,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.variant,
            onSurfaceVariant = palette.onVariant,
            error = palette.error
        )
    }
}

private val inkLight = Palette(
    primary = Color(0xFF1F5C52),
    onPrimary = Color.White,
    container = Color(0xFFD3EBE4),
    onContainer = Color(0xFF0D332D),
    background = Color(0xFFF6F7F6),
    surface = Color.White,
    variant = Color(0xFFE1E3E1),
    onSurface = Color(0xFF1A1C1B),
    onVariant = Color(0xFF444746),
    error = Color(0xFFBA1A1A)
)

private val inkDark = Palette(
    primary = Color(0xFF7DB8AC),
    onPrimary = Color.White,
    container = Color(0xFF244843),
    onContainer = Color(0xFFD3EBE4),
    background = Color(0xFF121413),
    surface = Color(0xFF1A1C1B),
    variant = Color(0xFF2A2D2C),
    onSurface = Color(0xFFE6E8E6),
    onVariant = Color(0xFFC4C7C5),
    error = Color(0xFFF2B8B5)
)

private val cinnabarLight = Palette(
    primary = Color(0xFF8E2F2B),
    onPrimary = Color.White,
    container = Color(0xFFF4D6D2),
    onContainer = Color(0xFF4A1210),
    background = Color(0xFFF8F3F2),
    surface = Color(0xFFFFFCFB),
    variant = Color(0xFFEFE0DC),
    onSurface = Color(0xFF2A1614),
    onVariant = Color(0xFF6E534F),
    error = Color(0xFFBA1A1A)
)

private val cinnabarDark = Palette(
    primary = Color(0xFFE7B0A8),
    onPrimary = Color(0xFF3F1210),
    container = Color(0xFF5C2A26),
    onContainer = Color(0xFFF4D6D2),
    background = Color(0xFF1A1212),
    surface = Color(0xFF241818),
    variant = Color(0xFF3A2826),
    onSurface = Color(0xFFF3E6E3),
    onVariant = Color(0xFFD7C2BE),
    error = Color(0xFFF2B8B5)
)

private val indigoLight = Palette(
    primary = Color(0xFF243E78),
    onPrimary = Color.White,
    container = Color(0xFFD5E0F7),
    onContainer = Color(0xFF101E40),
    background = Color(0xFFF4F6FA),
    surface = Color.White,
    variant = Color(0xFFE0E5F0),
    onSurface = Color(0xFF141821),
    onVariant = Color(0xFF4A5568),
    error = Color(0xFFBA1A1A)
)

private val indigoDark = Palette(
    primary = Color(0xFFB7C7EE),
    onPrimary = Color(0xFF101E40),
    container = Color(0xFF2A3D68),
    onContainer = Color(0xFFD5E0F7),
    background = Color(0xFF10141C),
    surface = Color(0xFF171C27),
    variant = Color(0xFF283044),
    onSurface = Color(0xFFE4E8F2),
    onVariant = Color(0xFFC3CAD8),
    error = Color(0xFFF2B8B5)
)

private val amberLight = Palette(
    primary = Color(0xFF8A5A12),
    onPrimary = Color.White,
    container = Color(0xFFF6E4C4),
    onContainer = Color(0xFF3E2806),
    background = Color(0xFFFBF6EE),
    surface = Color(0xFFFFFCF7),
    variant = Color(0xFFF0E6D4),
    onSurface = Color(0xFF241C12),
    onVariant = Color(0xFF6B5A40),
    error = Color(0xFFBA1A1A)
)

private val amberDark = Palette(
    primary = Color(0xFFE7C48A),
    onPrimary = Color(0xFF3E2806),
    container = Color(0xFF5A4218),
    onContainer = Color(0xFFF6E4C4),
    background = Color(0xFF1A1610),
    surface = Color(0xFF241E16),
    variant = Color(0xFF3A3124),
    onSurface = Color(0xFFF6EFE4),
    onVariant = Color(0xFFD9CBB6),
    error = Color(0xFFF2B8B5)
)

private val paperLight = Palette(
    primary = Color(0xFF5C5346),
    onPrimary = Color.White,
    container = Color(0xFFE7E0D4),
    onContainer = Color(0xFF2A261F),
    background = Color(0xFFF7F1E6),
    surface = Color(0xFFFFFBF5),
    variant = Color(0xFFEBE4D8),
    onSurface = Color(0xFF2C2822),
    onVariant = Color(0xFF6A6256),
    error = Color(0xFF8C3A32)
)

private val paperDark = Palette(
    primary = Color(0xFFD4CBBC),
    onPrimary = Color(0xFF2A261F),
    container = Color(0xFF4A4338),
    onContainer = Color(0xFFE7E0D4),
    background = Color(0xFF161410),
    surface = Color(0xFF211E1A),
    variant = Color(0xFF332E28),
    onSurface = Color(0xFFEFE8DC),
    onVariant = Color(0xFFD0C6B8),
    error = Color(0xFFE7B0A8)
)

private val lightPalettes = listOf(inkLight, cinnabarLight, indigoLight, amberLight, paperLight)
private val darkPalettes = listOf(inkDark, cinnabarDark, indigoDark, amberDark, paperDark)

fun themeSwatch(id: Int): Color = lightPalettes[Appearance.theme(id)].primary

@Composable
fun CimocTheme(theme: Int = 0, dark: Boolean = true, content: @Composable () -> Unit) {
    val index = Appearance.theme(theme)
    val palette = if (dark) darkPalettes[index] else lightPalettes[index]
    MaterialTheme(
        colorScheme = scheme(dark, palette),
        content = content
    )
}
