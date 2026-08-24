package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.haleydu.cimoc.ui.widget.OnTapGestureListener

class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SubsamplingScaleImageView(context, attrs) {

    var tapListener: OnTapGestureListener? = null
    var alwaysBlockParent = false

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                tapListener?.onSingleTap(e.rawX, e.rawY)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                tapListener?.onLongPress(e.rawX, e.rawY)
            }
        }
    )

    init {
        setMinimumScaleType(SCALE_TYPE_CENTER_INSIDE)
        setDoubleTapZoomDuration(200)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (alwaysBlockParent) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        detector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}
