package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.graphics.RectF
import android.view.View
import android.widget.OverScroller

class FlingRunnable(
    context: Context,
    listener: OnFlingRunningListener,
    view: View
) : Runnable {

    interface OnFlingRunningListener {
        fun onFlingRunning(dx: Int, dy: Int)
    }

    private val mView = view
    private val mListener = listener
    private val mScroller = OverScroller(context)
    private var mCurrentX = 0
    private var mCurrentY = 0

    fun cancelFling() {
        mScroller.abortAnimation()
    }

    fun fling(rect: RectF, viewWidth: Int, viewHeight: Int, velocityX: Int, velocityY: Int) {
        val startX = Math.round(-rect.left)
        val minX: Int
        val maxX: Int
        val minY: Int
        val maxY: Int

        if (viewWidth < rect.width()) {
            minX = 0
            maxX = Math.round(rect.width() - viewWidth)
        } else {
            minX = startX
            maxX = startX
        }

        val startY = Math.round(-rect.top)
        if (viewHeight < rect.height()) {
            minY = 0
            maxY = Math.round(rect.height() - viewHeight)
        } else {
            minY = startY
            maxY = startY
        }

        mCurrentX = startX
        mCurrentY = startY

        if (startX != maxX || startY != maxY) {
            mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
        }
    }

    override fun run() {
        if (mScroller.isFinished) {
            return
        }
        if (mScroller.computeScrollOffset()) {
            val newX = mScroller.currX
            val newY = mScroller.currY
            mListener.onFlingRunning(mCurrentX - newX, mCurrentY - newY)
            mCurrentX = newX
            mCurrentY = newY
            ViewUtils.postOnAnimation(mView, this)
        }
    }
}
