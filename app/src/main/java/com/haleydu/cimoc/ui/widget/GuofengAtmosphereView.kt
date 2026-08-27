package com.haleydu.cimoc.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.haleydu.cimoc.R

class GuofengAtmosphereView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paper = ContextCompat.getDrawable(context, R.drawable.bg_guofeng_paper)

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paper?.setBounds(0, 0, width, height)
        paper?.draw(canvas)
    }
}
