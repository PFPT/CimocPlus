package com.haleydu.cimoc.ui.reader
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.haleydu.cimoc.ui.widget.AnimatedScaleRunnable
import com.haleydu.cimoc.ui.widget.FlingRunnable
import com.haleydu.cimoc.ui.widget.OnScaleDragGestureListener
import com.haleydu.cimoc.ui.widget.OnTapGestureListener
import com.haleydu.cimoc.ui.widget.RetryDraweeView
import com.haleydu.cimoc.ui.widget.ScaleDragDetector
import com.haleydu.cimoc.ui.widget.ViewUtils

class PhotoDraweeView : RetryDraweeView, OnScaleDragGestureListener,
    FlingRunnable.OnFlingRunningListener, GestureDetector.OnDoubleTapListener {

    private val mDisplayRect = RectF()
    private val mMatrix = Matrix()
    private lateinit var mScaleDragDetector: ScaleDragDetector
    private lateinit var mGestureDetector: GestureDetector
    private var mTapGestureListener: OnTapGestureListener? = null
    private var mBlockParentIntercept = false
    private var mAlwaysBlockParent = false
    private var mScrollEdge = EDGE_BOTH
    private var mScrollMode = MODE_HORIZONTAL
    private var mScaleFactor = 2.0f
    private var isDoubleTap = true
    private var mCurrentFlingRunnable: FlingRunnable? = null

    constructor(context: Context, hierarchy: GenericDraweeHierarchy) : super(context, hierarchy) {
        init()
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelFling()
    }

    protected fun init() {
        hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER
        mScaleDragDetector = ScaleDragDetector(context, this)
        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                mTapGestureListener?.onLongPress(e.rawX, e.rawY)
            }
        })
        mGestureDetector.setOnDoubleTapListener(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val parent = parent
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                cancelFling()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        val wasScaling = mScaleDragDetector.isScaling()
        val wasDragging = mScaleDragDetector.isDragging()
        mScaleDragDetector.onTouchEvent(event)
        val noScale = !wasScaling && !mScaleDragDetector.isScaling()
        val noDrag = !wasDragging && !mScaleDragDetector.isDragging()
        mBlockParentIntercept = noScale && noDrag
        mGestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val count = canvas.save()
        canvas.concat(mMatrix)
        super.onDraw(canvas)
        canvas.restoreToCount(count)
    }

    fun update(id: Long?) {
        val tag = tag
        if (tag == null || tag != id) {
            setTag(id)
            resetMatrix()
        }
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

    override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        if (ViewUtils.calculateScale(mMatrix) < MAX_SCALE || scaleFactor < 1.0f) {
            mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            checkMatrixAndInvalidate()
        }
    }

    override fun onScaleEnd() {
        if (ViewUtils.calculateScale(mMatrix) < MIN_SCALE) {
            val rect = checkAndGetDisplayRect()
            post(AnimatedScaleRunnable(MIN_SCALE, rect.centerX(), rect.centerY(), this, mMatrix, this))
        }
    }

    override fun onDrag(dx: Float, dy: Float) {
        if (!mScaleDragDetector.isScaling()) {
            mMatrix.postTranslate(dx, dy)
            checkMatrixAndInvalidate()
            val parent = parent ?: return
            if (mAlwaysBlockParent) {
                parent.requestDisallowInterceptTouchEvent(mScrollEdge != EDGE_BOTH)
                return
            }
            if (!mScaleDragDetector.isScaling() && !mBlockParentIntercept) {
                when (mScrollMode) {
                    MODE_HORIZONTAL -> {
                        if (mScrollEdge == EDGE_BOTH || (mScrollEdge == EDGE_LEFT && dx >= 1f) ||
                            (mScrollEdge == EDGE_RIGHT && dx <= -1f)
                        ) {
                            parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    MODE_VERTICAL -> {
                        if (mScrollEdge == EDGE_BOTH || (mScrollEdge == EDGE_TOP && dy >= 1f) ||
                            (mScrollEdge == EDGE_BOTTOM && dy <= -1f)
                        ) {
                            parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            } else {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
    }

    override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        mCurrentFlingRunnable = FlingRunnable(context, this, this)
        mCurrentFlingRunnable!!.fling(
            checkAndGetDisplayRect(), ViewUtils.getViewWidth(this),
            ViewUtils.getViewHeight(this), velocityX.toInt(), velocityY.toInt()
        )
        post(mCurrentFlingRunnable)
    }

    override fun onFlingRunning(dx: Int, dy: Int) {
        mMatrix.postTranslate(dx.toFloat(), dy.toFloat())
        invalidate()
    }

    private fun setScale(scale: Float, focalX: Float, focalY: Float) {
        if (scale < MIN_SCALE || scale > MAX_SCALE) {
            return
        }
        post(AnimatedScaleRunnable(scale, focalX, focalY, this, mMatrix, this))
    }

    fun setScaleFactor(factor: Float) {
        mScaleFactor = factor
    }

    fun setDoubleTap(enable: Boolean) {
        isDoubleTap = enable
    }

    fun setAlwaysBlockParent(block: Boolean) {
        mAlwaysBlockParent = block
    }

    fun setScrollMode(mode: Int) {
        mScrollMode = mode
    }

    fun setTapListenerListener(listener: OnTapGestureListener?) {
        mTapGestureListener = listener
    }

    fun checkAndGetDisplayRect(): RectF {
        when (mScrollMode) {
            MODE_HORIZONTAL -> checkHorizontalBounds()
            MODE_VERTICAL -> checkVerticalBounds()
        }
        return getDisplayRect()
    }

    private fun getDisplayRect(): RectF {
        hierarchy.getActualImageBounds(mDisplayRect)
        mMatrix.mapRect(mDisplayRect)
        return mDisplayRect
    }

    fun checkMatrixAndInvalidate() {
        when (mScrollMode) {
            MODE_HORIZONTAL -> {
                if (checkHorizontalBounds()) {
                    invalidate()
                }
            }
            MODE_VERTICAL -> {
                if (checkVerticalBounds()) {
                    invalidate()
                }
            }
        }
    }

    fun checkVerticalBounds(): Boolean {
        val rect = getDisplayRect()
        val height = rect.height()
        val width = rect.width()
        val viewHeight = ViewUtils.getViewHeight(this)
        val viewWidth = ViewUtils.getViewWidth(this)
        var deltaX = 0.0f
        var deltaY = 0.0f
        if (height <= viewHeight) {
            deltaY = (viewHeight - height) / 2 - rect.top
            mScrollEdge = EDGE_BOTH
        } else if (rect.top > 0) {
            deltaY = -rect.top
            mScrollEdge = EDGE_TOP
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom
            mScrollEdge = EDGE_BOTTOM
        } else {
            mScrollEdge = EDGE_NONE
        }
        if (width <= viewWidth.toFloat()) {
            deltaX = (viewWidth - width) / 2 - rect.left
        } else if (rect.left > 0.0f) {
            deltaX = -rect.left
        } else if (rect.right < viewWidth.toFloat()) {
            deltaX = viewWidth - rect.right
        }
        mMatrix.postTranslate(deltaX, deltaY)
        return true
    }

    fun checkHorizontalBounds(): Boolean {
        val rect = getDisplayRect()
        val height = rect.height()
        val width = rect.width()
        val viewHeight = ViewUtils.getViewHeight(this)
        val viewWidth = ViewUtils.getViewWidth(this)
        var deltaX = 0.0f
        var deltaY = 0.0f
        if (height <= viewHeight.toFloat()) {
            deltaY = (viewHeight - height) / 2 - rect.top
        } else if (rect.top > 0.0f) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight.toFloat()) {
            deltaY = viewHeight - rect.bottom
        }
        if (width <= viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left
            mScrollEdge = EDGE_BOTH
        } else if (rect.left > 0) {
            deltaX = -rect.left
            mScrollEdge = EDGE_LEFT
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
            mScrollEdge = EDGE_RIGHT
        } else {
            mScrollEdge = EDGE_NONE
        }
        mMatrix.postTranslate(deltaX, deltaY)
        return true
    }

    private fun resetMatrix() {
        mMatrix.reset()
        when (mScrollMode) {
            MODE_HORIZONTAL -> checkHorizontalBounds()
            MODE_VERTICAL -> checkVerticalBounds()
        }
        invalidate()
    }

    private fun cancelFling() {
        mCurrentFlingRunnable?.cancelFling()
        mCurrentFlingRunnable = null
    }

    companion object {
        const val MIN_SCALE = 1.0f
        const val MAX_SCALE = 3.0f
        const val MODE_VERTICAL = 0
        const val MODE_HORIZONTAL = 1
        private const val EDGE_NONE = -1
        private const val EDGE_LEFT = 0
        private const val EDGE_RIGHT = 1
        private const val EDGE_TOP = 0
        private const val EDGE_BOTTOM = 1
        private const val EDGE_BOTH = 2
    }
}
