package com.haleydu.cimoc.ui.widget;

import android.content.Context;
import android.graphics.RectF;
import android.view.View;
import android.widget.OverScroller;

public class FlingRunnable implements Runnable {

    private final View mView;
    private final OnFlingRunningListener mListener;
    private final OverScroller mScroller;
    private int mCurrentX, mCurrentY;

    public FlingRunnable(Context context, OnFlingRunningListener listener, View view) {
        mScroller = new OverScroller(context);
        mListener = listener;
        mView = view;
    }

    public void cancelFling() {
        mScroller.abortAnimation();
    }

    public void fling(RectF rect, int viewWidth, int viewHeight, int velocityX, int velocityY) {
        final int startX = Math.round(-rect.left);
        final int minX, maxX, minY, maxY;

        if (viewWidth < rect.width()) {
            minX = 0;
            maxX = Math.round(rect.width() - viewWidth);
        } else {
            minX = maxX = startX;
        }

        final int startY = Math.round(-rect.top);
        if (viewHeight < rect.height()) {
            minY = 0;
            maxY = Math.round(rect.height() - viewHeight);
        } else {
            minY = maxY = startY;
        }

        mCurrentX = startX;
        mCurrentY = startY;

        if (startX != maxX || startY != maxY) {
            mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
        }
    }

    @Override
    public void run() {
        if (mScroller.isFinished()) {
            return;
        }

        if (mScroller.computeScrollOffset()) {
            final int newX = mScroller.getCurrX();
            final int newY = mScroller.getCurrY();
            mListener.onFlingRunning(mCurrentX - newX, mCurrentY - newY);
            mCurrentX = newX;
            mCurrentY = newY;
            ViewUtils.postOnAnimation(mView, this);
        }
    }

    public interface OnFlingRunningListener {
        void onFlingRunning(int dx, int dy);
    }

}
