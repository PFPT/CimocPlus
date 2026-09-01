package com.haleydu.cimoc.utils

import android.content.Context
import android.util.TypedValue
import com.haleydu.cimoc.R

object ThemeUtils {

    const val THEME_GUOFENG = 6

    @JvmStatic
    fun getResourceId(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.resourceId
    }

    @JvmStatic
    fun getThemeById(id: Int): Int {
        return R.style.AppThemeGuofeng
    }
}
