package com.haleydu.cimoc.ui.widget.rvp
import android.content.Context
import android.graphics.PointF
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class RecyclerViewPager : RecyclerView {

    var mNeedAdjust = false
    var mFirstLeftWhenDragging = 0
    var mFirstTopWhenDragging = 0
    var mCurView: View? = null
    var mMaxLeftWhenDragging = Integer.MIN_VALUE
    var mMinLeftWhenDragging = Integer.MAX_VALUE
    var mMaxTopWhenDragging = Integer.MIN_VALUE
    var mMinTopWhenDragging = Integer.MAX_VALUE

    private var mViewPagerAdapter: RecyclerViewPagerAdapter<*>? = null
    private var mTouchSpan = 0f
    private var mOnPageChangedListener: OnPageChangedListener? = null
    private val mGlobalLayoutListener = GlobalLayoutListener()
    private var mSmoothScrollTargetPosition = -1
    private var mPositionBeforeScroll = -1
    private var mTriggerOffset = 0.05f
    private var mScrollSpeed: Float? = null
    private var mPositionOnTouchDown = -1
    private var mHasCalledOnPageChanged = true
    private var reverseLayout = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        isNestedScrollingEnabled = false
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        try {
            val fLayoutState = state!!.javaClass.getDeclaredField("mLayoutState")
            fLayoutState.isAccessible = true
            val layoutState = fLayoutState.get(state)
            val fAnchorOffset = layoutState.javaClass.getDeclaredField("mAnchorOffset")
            val fAnchorPosition = layoutState.javaClass.getDeclaredField("mAnchorPosition")
            fAnchorPosition.isAccessible = true
            fAnchorOffset.isAccessible = true
            if (fAnchorOffset.getInt(layoutState) > 0) {
                fAnchorPosition.set(layoutState, fAnchorPosition.getInt(layoutState) - 1)
            } else if (fAnchorOffset.getInt(layoutState) < 0) {
                fAnchorPosition.set(layoutState, fAnchorPosition.getInt(layoutState) + 1)
            }
            fAnchorOffset.setInt(layoutState, 0)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onRestoreInstanceState(state)
    }

    override fun swapAdapter(adapter: Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
        mViewPagerAdapter = ensureRecyclerViewPagerAdapter(adapter)
        super.swapAdapter(mViewPagerAdapter, removeAndRecycleExistingViews)
    }

    override fun getAdapter(): Adapter<*>? {
        return mViewPagerAdapter?.mAdapter
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        mViewPagerAdapter = if (adapter == null) null else ensureRecyclerViewPagerAdapter(adapter)
        super.setAdapter(mViewPagerAdapter)
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        if (layout is LinearLayoutManager) {
            reverseLayout = layout.reverseLayout
        }
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val flinging = super.fling((velocityX * FLING_FACTOR).toInt(), (velocityY * FLING_FACTOR).toInt())
        if (flinging) {
            if (layoutManager!!.canScrollHorizontally()) {
                adjustPositionX(velocityX)
            } else {
                adjustPositionY(velocityY)
            }
        }
        return flinging
    }

    fun startSmoothScroll(position: Int) {
        mSmoothScrollTargetPosition = position
        val linearSmoothScroller = object : LinearSmoothScroller(context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                val manager = layoutManager ?: return null
                return (manager as LinearLayoutManager).computeScrollVectorForPosition(targetPosition)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return context.resources.displayMetrics.density * (mScrollSpeed ?: 1f) / displayMetrics.density
            }
        }
        linearSmoothScroller.targetPosition = position
        if (position == NO_POSITION) {
            return
        }
        layoutManager!!.startSmoothScroll(linearSmoothScroller)
    }

    override fun smoothScrollToPosition(position: Int) {
        if (mPositionBeforeScroll < 0) {
            mPositionBeforeScroll = currentPosition
        }
        startSmoothScroll(position)
        if (mSmoothScrollTargetPosition >= 0 && mSmoothScrollTargetPosition < getItemCount() &&
            mSmoothScrollTargetPosition != mPositionBeforeScroll && mOnPageChangedListener != null
        ) {
            viewTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener)
        }
    }

    override fun scrollToPosition(position: Int) {
        mPositionBeforeScroll = currentPosition
        mSmoothScrollTargetPosition = position
        super.scrollToPosition(position)
        if (mSmoothScrollTargetPosition >= 0 && mSmoothScrollTargetPosition < getItemCount() &&
            mSmoothScrollTargetPosition != mPositionBeforeScroll && mOnPageChangedListener != null
        ) {
            viewTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener)
        }
    }

    private fun getItemCount(): Int {
        return mViewPagerAdapter?.itemCount ?: 0
    }

    val currentPosition: Int
        get() {
            var curPosition = if (layoutManager!!.canScrollHorizontally()) {
                RecyclerViewUtils.getCenterXChildPosition(this)
            } else {
                RecyclerViewUtils.getCenterYChildPosition(this)
            }
            if (curPosition < 0) {
                curPosition = mSmoothScrollTargetPosition
            }
            return curPosition
        }

    protected fun adjustPositionX(velocityX: Int) {
        var vx = velocityX
        if (reverseLayout) vx *= -1
        val childCount = childCount
        if (childCount > 0) {
            val curPosition = RecyclerViewUtils.getCenterXChildPosition(this)
            val childWidth = width - paddingLeft - paddingRight
            val flingCount = max(-1, min(1, getFlingCount(vx, childWidth)))
            var targetPosition = if (flingCount == 0) curPosition else mPositionOnTouchDown + flingCount
            targetPosition = max(targetPosition, 0)
            targetPosition = min(targetPosition, getItemCount() - 1)
            if (targetPosition == curPosition && mPositionOnTouchDown == curPosition) {
                val centerXChild = RecyclerViewUtils.getCenterXChild(this)
                if (centerXChild != null) {
                    if (mTouchSpan > centerXChild.width * mTriggerOffset * mTriggerOffset && targetPosition != 0) {
                        if (!reverseLayout) targetPosition-- else targetPosition++
                    } else if (mTouchSpan < centerXChild.width * -mTriggerOffset && targetPosition != getItemCount() - 1) {
                        if (!reverseLayout) targetPosition++ else targetPosition--
                    }
                }
            }
            startSmoothScroll(safeTargetPosition(targetPosition, getItemCount()))
        }
    }

    protected fun adjustPositionY(velocityY: Int) {
        var vy = velocityY
        if (reverseLayout) vy *= -1
        val childCount = childCount
        if (childCount > 0) {
            val curPosition = RecyclerViewUtils.getCenterYChildPosition(this)
            val childHeight = height - paddingTop - paddingBottom
            val flingCount = max(-1, min(1, getFlingCount(vy, childHeight)))
            var targetPosition = if (flingCount == 0) curPosition else mPositionOnTouchDown + flingCount
            targetPosition = max(targetPosition, 0)
            targetPosition = min(targetPosition, getItemCount() - 1)
            if (targetPosition == curPosition && mPositionOnTouchDown == curPosition) {
                val centerYChild = RecyclerViewUtils.getCenterYChild(this)
                if (centerYChild != null) {
                    if (mTouchSpan > centerYChild.height * mTriggerOffset && targetPosition != 0) {
                        if (!reverseLayout) targetPosition-- else targetPosition++
                    } else if (mTouchSpan < centerYChild.height * -mTriggerOffset && targetPosition != getItemCount() - 1) {
                        if (!reverseLayout) targetPosition++ else targetPosition--
                    }
                }
            }
            startSmoothScroll(safeTargetPosition(targetPosition, getItemCount()))
        }
    }

    fun setScrollSpeed(speed: Float) {
        mScrollSpeed = speed
    }

    fun setTriggerOffset(offset: Float) {
        mTriggerOffset = offset
    }

    fun setOnPageChangedListener(listener: OnPageChangedListener?) {
        mOnPageChangedListener = listener
    }

    fun refreshPosition() {
        val pos = currentPosition
        if (mPositionBeforeScroll != -1) {
            mPositionBeforeScroll += pos
        }
        mPositionOnTouchDown += pos
        mSmoothScrollTargetPosition += pos
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && layoutManager != null) {
            mPositionOnTouchDown = if (layoutManager!!.canScrollHorizontally()) {
                RecyclerViewUtils.getCenterXChildPosition(this)
            } else {
                RecyclerViewUtils.getCenterYChildPosition(this)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_MOVE) {
            val cur = mCurView
            if (cur != null) {
                mMaxLeftWhenDragging = max(cur.left, mMaxLeftWhenDragging)
                mMaxTopWhenDragging = max(cur.top, mMaxTopWhenDragging)
                mMinLeftWhenDragging = min(cur.left, mMinLeftWhenDragging)
                mMinTopWhenDragging = min(cur.top, mMinTopWhenDragging)
            }
        }
        return super.onTouchEvent(e)
    }

    override fun onInterceptHoverEvent(event: MotionEvent): Boolean {
        return try {
            super.onInterceptHoverEvent(event)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == SCROLL_STATE_DRAGGING) {
            mNeedAdjust = true
            mCurView = if (layoutManager!!.canScrollHorizontally()) {
                RecyclerViewUtils.getCenterXChild(this)
            } else {
                RecyclerViewUtils.getCenterYChild(this)
            }
            val cur = mCurView
            if (cur != null) {
                if (mHasCalledOnPageChanged) {
                    mPositionBeforeScroll = getChildLayoutPosition(cur)
                    mHasCalledOnPageChanged = false
                }
                mFirstLeftWhenDragging = cur.left
                mFirstTopWhenDragging = cur.top
            } else {
                mPositionBeforeScroll = -1
            }
            mTouchSpan = 0f
        } else if (state == SCROLL_STATE_SETTLING) {
            mNeedAdjust = false
            val cur = mCurView
            if (cur != null) {
                mTouchSpan = if (layoutManager!!.canScrollHorizontally()) {
                    (cur.left - mFirstLeftWhenDragging).toFloat()
                } else {
                    (cur.top - mFirstTopWhenDragging).toFloat()
                }
            } else {
                mTouchSpan = 0f
            }
            mCurView = null
        } else if (state == SCROLL_STATE_IDLE) {
            if (mNeedAdjust) {
                var targetPosition = if (layoutManager!!.canScrollHorizontally()) {
                    RecyclerViewUtils.getCenterXChildPosition(this)
                } else {
                    RecyclerViewUtils.getCenterYChildPosition(this)
                }
                val cur = mCurView
                if (cur != null) {
                    targetPosition = getChildAdapterPosition(cur)
                    if (layoutManager!!.canScrollHorizontally()) {
                        val spanX = cur.left - mFirstLeftWhenDragging
                        if (spanX > cur.width * mTriggerOffset && cur.left >= mMaxLeftWhenDragging) {
                            if (!reverseLayout) targetPosition-- else targetPosition++
                        } else if (spanX < cur.width * -mTriggerOffset && cur.left <= mMinLeftWhenDragging) {
                            if (!reverseLayout) targetPosition++ else targetPosition--
                        }
                    } else {
                        val spanY = cur.top - mFirstTopWhenDragging
                        if (spanY > cur.height * mTriggerOffset && cur.top >= mMaxTopWhenDragging) {
                            if (!reverseLayout) targetPosition-- else targetPosition++
                        } else if (spanY < cur.height * -mTriggerOffset && cur.top <= mMinTopWhenDragging) {
                            if (!reverseLayout) targetPosition++ else targetPosition--
                        }
                    }
                }
                startSmoothScroll(safeTargetPosition(targetPosition, getItemCount()))
                mCurView = null
            } else if (mSmoothScrollTargetPosition != mPositionBeforeScroll) {
                mOnPageChangedListener?.OnPageChanged(mPositionBeforeScroll, currentPosition)
                mHasCalledOnPageChanged = true
                mPositionBeforeScroll = mSmoothScrollTargetPosition
            }
            mMaxLeftWhenDragging = Integer.MIN_VALUE
            mMinLeftWhenDragging = Integer.MAX_VALUE
            mMaxTopWhenDragging = Integer.MIN_VALUE
            mMinTopWhenDragging = Integer.MAX_VALUE
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun ensureRecyclerViewPagerAdapter(adapter: Adapter<*>?): RecyclerViewPagerAdapter<*> {
        return if (adapter is RecyclerViewPagerAdapter<*>) {
            adapter
        } else {
            RecyclerViewPagerAdapter(this, adapter as Adapter<ViewHolder>)
        }
    }

    private fun getFlingCount(velocity: Int, cellSize: Int): Int {
        if (velocity == 0) {
            return 0
        }
        val sign = if (velocity > 0) 1 else -1
        return (sign * ceil((velocity * sign * FLING_FACTOR / cellSize) - mTriggerOffset)).toInt()
    }

    private fun safeTargetPosition(position: Int, count: Int): Int {
        if (position < 0) {
            return 0
        }
        if (position >= count) {
            return count - 1
        }
        return position
    }

    interface OnPageChangedListener {
        fun OnPageChanged(oldPosition: Int, newPosition: Int)
    }

    private inner class GlobalLayoutListener : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            if (mSmoothScrollTargetPosition >= 0 && mSmoothScrollTargetPosition < getItemCount()) {
                mOnPageChangedListener?.OnPageChanged(mPositionBeforeScroll, currentPosition)
            }
        }
    }

    companion object {
        private const val FLING_FACTOR = 0.10f
    }
}
