package com.haleydu.cimoc.app.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.haleydu.cimoc.shared.model.Appearance

data class AppIconSet(
    val back: ImageVector,
    val forward: ImageVector,
    val home: ImageVector,
    val library: ImageVector,
    val search: ImageVector,
    val browser: ImageVector,
    val sources: ImageVector,
    val profile: ImageVector,
    val star: ImageVector,
    val starOutline: ImageVector,
    val refresh: ImageVector,
    val list: ImageVector,
    val delete: ImageVector,
    val favorite: ImageVector,
    val favoriteOutline: ImageVector,
    val swap: ImageVector,
    val sort: ImageVector,
    val pause: ImageVector,
    val replay: ImageVector,
    val play: ImageVector,
    val pages: ImageVector,
    val rotate: ImageVector,
    val night: ImageVector,
    val up: ImageVector,
    val left: ImageVector,
    val right: ImageVector
)

fun iconsFor(style: Int): AppIconSet = when (Appearance.iconStyle(style)) {
    1 -> outlinedIcons()
    2 -> roundedIcons()
    3 -> inkGlyphs()
    4 -> sealGlyphs()
    else -> filledIcons()
}

private fun filledIcons() = AppIconSet(
    back = Icons.AutoMirrored.Filled.ArrowBack,
    forward = Icons.AutoMirrored.Filled.ArrowForward,
    home = Icons.Filled.Home,
    library = Icons.AutoMirrored.Filled.MenuBook,
    search = Icons.Filled.Search,
    browser = Icons.Filled.Language,
    sources = Icons.Filled.Explore,
    profile = Icons.Filled.Person,
    star = Icons.Filled.Star,
    starOutline = Icons.Filled.StarBorder,
    refresh = Icons.Filled.Refresh,
    list = Icons.AutoMirrored.Filled.List,
    delete = Icons.Filled.Delete,
    favorite = Icons.Filled.Favorite,
    favoriteOutline = Icons.Filled.FavoriteBorder,
    swap = Icons.Filled.SwapHoriz,
    sort = Icons.Filled.SwapVert,
    pause = Icons.Filled.Pause,
    replay = Icons.Filled.Replay,
    play = Icons.Filled.PlayArrow,
    pages = Icons.Filled.ViewDay,
    rotate = Icons.Filled.ScreenRotation,
    night = Icons.Filled.DarkMode,
    up = Icons.Filled.KeyboardArrowUp,
    left = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
    right = Icons.AutoMirrored.Filled.KeyboardArrowRight
)

private fun outlinedIcons() = AppIconSet(
    back = Icons.AutoMirrored.Outlined.ArrowBack,
    forward = Icons.AutoMirrored.Outlined.ArrowForward,
    home = Icons.Outlined.Home,
    library = Icons.AutoMirrored.Outlined.MenuBook,
    search = Icons.Outlined.Search,
    browser = Icons.Outlined.Language,
    sources = Icons.Outlined.Explore,
    profile = Icons.Outlined.Person,
    star = Icons.Outlined.Star,
    starOutline = Icons.Outlined.StarBorder,
    refresh = Icons.Outlined.Refresh,
    list = Icons.AutoMirrored.Outlined.List,
    delete = Icons.Outlined.Delete,
    favorite = Icons.Outlined.Favorite,
    favoriteOutline = Icons.Outlined.FavoriteBorder,
    swap = Icons.Outlined.SwapHoriz,
    sort = Icons.Outlined.SwapVert,
    pause = Icons.Outlined.Pause,
    replay = Icons.Outlined.Replay,
    play = Icons.Outlined.PlayArrow,
    pages = Icons.Outlined.ViewDay,
    rotate = Icons.Outlined.ScreenRotation,
    night = Icons.Outlined.DarkMode,
    up = Icons.Outlined.KeyboardArrowUp,
    left = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
    right = Icons.AutoMirrored.Outlined.KeyboardArrowRight
)

private fun roundedIcons() = AppIconSet(
    back = Icons.AutoMirrored.Rounded.ArrowBack,
    forward = Icons.AutoMirrored.Rounded.ArrowForward,
    home = Icons.Rounded.Home,
    library = Icons.AutoMirrored.Rounded.MenuBook,
    search = Icons.Rounded.Search,
    browser = Icons.Rounded.Language,
    sources = Icons.Rounded.Explore,
    profile = Icons.Rounded.Person,
    star = Icons.Rounded.Star,
    starOutline = Icons.Rounded.StarBorder,
    refresh = Icons.Rounded.Refresh,
    list = Icons.AutoMirrored.Rounded.List,
    delete = Icons.Rounded.Delete,
    favorite = Icons.Rounded.Favorite,
    favoriteOutline = Icons.Rounded.FavoriteBorder,
    swap = Icons.Rounded.SwapHoriz,
    sort = Icons.Rounded.SwapVert,
    pause = Icons.Rounded.Pause,
    replay = Icons.Rounded.Replay,
    play = Icons.Rounded.PlayArrow,
    pages = Icons.Rounded.ViewDay,
    rotate = Icons.Rounded.ScreenRotation,
    night = Icons.Rounded.DarkMode,
    up = Icons.Rounded.KeyboardArrowUp,
    left = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
    right = Icons.AutoMirrored.Rounded.KeyboardArrowRight
)

val LocalAppIcons = staticCompositionLocalOf { filledIcons() }

@Composable
fun ProvideAppIcons(style: Int, content: @Composable () -> Unit) {
    val icons = remember(style) { iconsFor(style) }
    CompositionLocalProvider(LocalAppIcons provides icons, content = content)
}
