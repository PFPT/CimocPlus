package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.util.AttributeSet
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class ReverseSeekBar : DiscreteSeekBar {

    private var isReverse = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun isRtl(): Boolean {
        return isReverse
    }

    fun setReverse(reverse: Boolean) {
        isReverse = reverse
        invalidate()
    }
}
