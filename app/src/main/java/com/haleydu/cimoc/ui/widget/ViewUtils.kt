package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.graphics.Matrix
import android.os.Build
import android.view.View
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

object ViewUtils {

    @JvmStatic
    fun dpToPixel(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        var result = ceil(25 * context.resources.displayMetrics.density.toDouble()).toInt()
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun getNavigationBarHeight(context: Context): Int {
        var result = ceil(48 * context.resources.displayMetrics.density.toDouble()).toInt()
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun postOnAnimation(view: View, runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postOnAnimation(runnable)
        } else {
            view.postDelayed(runnable, 16L)
        }
    }

    @JvmStatic
    fun calculateScale(matrix: Matrix): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return sqrt(
            values[Matrix.MSCALE_X].toDouble().pow(2.0) +
                values[Matrix.MSKEW_Y].toDouble().pow(2.0)
        ).toFloat()
    }

    @JvmStatic
    fun getViewWidth(view: View): Int {
        return view.width - view.paddingLeft - view.paddingRight
    }

    @JvmStatic
    fun getViewHeight(view: View): Int {
        return view.height - view.paddingTop - view.paddingBottom
    }
}
