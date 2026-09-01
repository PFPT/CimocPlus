package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Calendar

class MiniClockText : AppCompatTextView {

    private var mCalendar: Calendar? = null
    private var mAttached = false

    private val mTicker = object : Runnable {
        override fun run() {
            mCalendar!!.timeInMillis = System.currentTimeMillis()
            text = DateFormat.format(FORMAT_24_HOUR, mCalendar)
            val now = SystemClock.uptimeMillis()
            val next = now + (1000 - now % 1000)
            handler.postAtTime(this, next)
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initClock()
    }

    private fun initClock() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mAttached) {
            mAttached = true
            mTicker.run()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mAttached) {
            handler.removeCallbacks(mTicker)
            mAttached = false
        }
    }

    companion object {
        val FORMAT_24_HOUR: CharSequence = "HH:mm"
    }
}
