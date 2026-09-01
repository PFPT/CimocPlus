package com.haleydu.cimoc.ui.widget
import android.graphics.Matrix
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class AnimatedScaleRunnable(
    scale: Float,
    x: Float,
    y: Float,
    view: View,
    matrix: Matrix,
    listener: OnScaleDragGestureListener
) : Runnable {

    private val mView = view
    private val mMatrix = matrix
    private val mFocusX = x
    private val mFocusY = y
    private val mStartTime = System.currentTimeMillis()
    private val mScaleStart = ViewUtils.calculateScale(matrix)
    private val mScaleEnd = scale
    private val mListener = listener
    private val mZoomInterpolator = AccelerateDecelerateInterpolator()

    override fun run() {
        val t = interpolate()
        val scale = mScaleStart + t * (mScaleEnd - mScaleStart)
        val deltaScale = scale / ViewUtils.calculateScale(mMatrix)
        mListener.onScale(deltaScale, mFocusX, mFocusY)
        if (t < 1f) {
            ViewUtils.postOnAnimation(mView, this)
        }
    }

    private fun interpolate(): Float {
        var t = 1f * (System.currentTimeMillis() - mStartTime) / 200
        t = t.coerceAtMost(1f)
        t = mZoomInterpolator.getInterpolation(t)
        return t
    }
}
