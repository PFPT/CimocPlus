package com.haleydu.cimoc.component

import androidx.annotation.ColorRes

interface ThemeResponsive {
    fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int)
}
