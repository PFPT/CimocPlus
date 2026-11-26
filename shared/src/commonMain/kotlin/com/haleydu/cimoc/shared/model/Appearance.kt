package com.haleydu.cimoc.shared.model

object Appearance {
    const val THEME_COUNT = 5
    const val ICON_COUNT = 5

    fun theme(id: Int): Int = id.coerceIn(0, THEME_COUNT - 1)

    fun iconStyle(id: Int): Int = id.coerceIn(0, ICON_COUNT - 1)

    fun themeTitle(id: Int): String = when (theme(id)) {
        1 -> "朱砂"
        2 -> "靛蓝"
        3 -> "琥珀"
        4 -> "素纸"
        else -> "青墨"
    }

    fun iconTitle(id: Int): String = when (iconStyle(id)) {
        1 -> "线性"
        2 -> "圆角"
        3 -> "线稿"
        4 -> "印章"
        else -> "填充"
    }
}
