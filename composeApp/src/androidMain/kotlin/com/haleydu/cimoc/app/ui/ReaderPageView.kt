package com.haleydu.cimoc.app.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlin.math.abs

internal class ReaderPageView(context: Context) : SubsamplingScaleImageView(context) {
    var onTap: ((Offset, Float, Float) -> Unit)? = null
    var onFling: ((Float, Float) -> Unit)? = null
    var vertical: Boolean = false
    private var downX = 0f
    private var downY = 0f
    private var turned = false
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onTap?.invoke(Offset(e.x, e.y), width.toFloat(), height.toFloat())
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (zoomed()) return false
            turned = true
            onFling?.invoke(velocityX, velocityY)
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1 || zoomed()) {
            return super.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                turned = false
            }
            MotionEvent.ACTION_UP -> {
                gestures.onTouchEvent(event)
                val dx = event.x - downX
                val dy = event.y - downY
                val axis = if (vertical) dy else dx
                val other = if (vertical) dx else dy
                val threshold = (if (vertical) height else width) * 0.18f
                if (!turned && abs(axis) > threshold && abs(axis) > abs(other)) {
                    val velocity = if (axis < 0f) -1f else 1f
                    onFling?.invoke(
                        if (vertical) 0f else velocity,
                        if (vertical) velocity else 0f
                    )
                    turned = true
                }
                return true
            }
        }
        gestures.onTouchEvent(event)
        return true
    }

    private fun zoomed(): Boolean {
        if (!isReady) return false
        val min = minScale
        if (min <= 0f) return false
        return scale > min * 1.08f
    }
}
