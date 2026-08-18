package com.haleydu.cimoc.ui.reader

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import com.haleydu.cimoc.manager.PreferenceManager

object ReaderColorFilter {

    @JvmStatic
    fun apply(view: View, preference: PreferenceManager) {
        val r = preference.getInt(PreferenceManager.PREF_READER_COLOR_RED, 100) / 100f
        val g = preference.getInt(PreferenceManager.PREF_READER_COLOR_GREEN, 100) / 100f
        val b = preference.getInt(PreferenceManager.PREF_READER_COLOR_BLUE, 100) / 100f
        val invert = preference.getBoolean(PreferenceManager.PREF_READER_COLOR_INVERT, false)
        val gray = preference.getBoolean(PreferenceManager.PREF_READER_COLOR_GRAY, false)
        val identity = r == 1f && g == 1f && b == 1f && !invert && !gray
        if (identity) {
            view.setLayerType(View.LAYER_TYPE_NONE, null)
            return
        }
        val matrix = ColorMatrix()
        matrix.setScale(r, g, b, 1f)
        if (gray) {
            val saturation = ColorMatrix()
            saturation.setSaturation(0f)
            matrix.postConcat(saturation)
        }
        if (invert) {
            val invertMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(invertMatrix)
        }
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
}
