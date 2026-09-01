package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class ZoomableRecyclerView : RecyclerView, OnScaleDragGestureListener,
    FlingRunnable.OnFlingRunningListener, GestureDetector.OnDoubleTapListener {

    private val mMatrix = Matrix()
    private val mTempRectF = RectF()
    private val mTempRect = Rect()

    private val mScaleDragDetector: ScaleDragDetector
    private val mGestureDetector: GestureDetector
    private var mTapGestureListener: OnTapGestureListener? = null

    private var mScaleFactor = 2.0f
    private var isVertical = true
    private var isDoubleTap = true

    private var mCurrentFlingRunnable: FlingRunnable? = null

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        mScaleDragDetector = ScaleDragDetector(context, this)
        mGestureDetector = GestureDetector(getContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                mTapGestureListener?.onLongPress(e.rawX, e.rawY)
            }
        })
        mGestureDetector.setOnDoubleTapListener(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> cancelFling()
        }
        val wasScaling = mScaleDragDetector.isScaling()
        mScaleDragDetector.onTouchEvent(event)
        if (!wasScaling && !mScaleDragDetector.isScaling()) {
            super.onTouchEvent(event)
        }
        mGestureDetector.onTouchEvent(event)
        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.concat(mMatrix)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        if (ViewUtils.calculateScale(mMatrix) < MAX_SCALE || scaleFactor < 1.0f) {
            mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            checkBounds()
            invalidate()
        }
    }

    override fun onScaleEnd() {
        if (ViewUtils.calculateScale(mMatrix) < MIN_SCALE) {
            checkBounds()
            val rect = getDisplayRect(mMatrix)
            post(AnimatedScaleRunnable(MIN_SCALE, rect.centerX(), rect.centerY(), this, mMatrix, this))
        }
    }

    override fun onDrag(dx: Float, dy: Float) {
        if (isVertical) {
            mMatrix.postTranslate(dx, 0f)
        } else {
            mMatrix.postTranslate(0f, dy)
        }
        checkBounds()
        invalidate()
    }

    override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        checkBounds()
        val rect = getDisplayRect(mMatrix)
        mCurrentFlingRunnable = FlingRunnable(context, this, this)
        mCurrentFlingRunnable!!.fling(
            rect, ViewUtils.getViewWidth(this),
            ViewUtils.getViewHeight(this), velocityX.toInt(), velocityY.toInt()
        )
        post(mCurrentFlingRunnable)
    }

    override fun onFlingRunning(dx: Int, dy: Int) {
        if (isVertical) {
            mMatrix.postTranslate(dx.toFloat(), 0f)
        } else {
            mMatrix.postTranslate(0f, dy.toFloat())
        }
        invalidate()
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val listener = mTapGestureListener
        if (listener != null) {
            listener.onSingleTap(e.rawX, e.rawY)
            return true
        }
        return false
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        if (isDoubleTap) {
            try {
                val scale = ViewUtils.calculateScale(mMatrix)
                val x = event.x
                val y = event.y
                setScale(if (scale < mScaleFactor) mScaleFactor else MIN_SCALE, x, y)
            } catch (e: Exception) {
            }
            return true
        }
        return false
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        return false
    }

    fun setScaleFactor(factor: Float) {
        mScaleFactor = factor
    }

    fun setDoubleTap(enable: Boolean) {
        isDoubleTap = enable
    }

    fun setTapListenerListener(listener: OnTapGestureListener?) {
        mTapGestureListener = listener
    }

    fun setVertical(vertical: Boolean) {
        isVertical = vertical
    }

    private fun setScale(scale: Float, focusX: Float, focusY: Float) {
        if (scale >= MIN_SCALE && scale <= MAX_SCALE) {
            post(AnimatedScaleRunnable(scale, focusX, focusY, this, mMatrix, this))
        }
    }

    private fun cancelFling() {
        mCurrentFlingRunnable?.cancelFling()
        mCurrentFlingRunnable = null
    }

    fun checkBounds() {
        val rect = getDisplayRect(mMatrix)
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0.0f
        var deltaY = 0.0f
        val viewHeight = ViewUtils.getViewHeight(this)
        if (height <= viewHeight) {
            deltaY = (viewHeight - height) / 2 - rect.top
        } else if (rect.top > 0) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom
        }
        val viewWidth = ViewUtils.getViewWidth(this)
        if (width <= viewWidth.toFloat()) {
            deltaX = (viewWidth - width) / 2 - rect.left
        } else if (rect.left > 0.0f) {
            deltaX = -rect.left
        } else if (rect.right < viewWidth.toFloat()) {
            deltaX = viewWidth - rect.right
        }
        mMatrix.postTranslate(deltaX, deltaY)
    }

    private fun getDisplayRect(matrix: Matrix): RectF {
        getLocalVisibleRect(mTempRect)
        mTempRectF.set(mTempRect)
        matrix.mapRect(mTempRectF)
        return mTempRectF
    }

    companion object {
        const val MIN_SCALE = 1.0f
        const val MAX_SCALE = 3.0f
    }
}
