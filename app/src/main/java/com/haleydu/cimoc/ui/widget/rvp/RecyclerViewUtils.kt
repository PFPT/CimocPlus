package com.haleydu.cimoc.ui.widget.rvp
import android.view.View
import androidx.recyclerview.widget.RecyclerView

object RecyclerViewUtils {

    @JvmStatic
    fun getCenterXChild(recyclerView: RecyclerView): View? {
        val childCount = recyclerView.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = recyclerView.getChildAt(i)
                if (isChildInCenterX(recyclerView, child)) {
                    return child
                }
            }
        }
        return null
    }

    @JvmStatic
    fun getCenterXChildPosition(recyclerView: RecyclerView): Int {
        val childCount = recyclerView.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = recyclerView.getChildAt(i)
                if (isChildInCenterX(recyclerView, child)) {
                    return recyclerView.getChildAdapterPosition(child)
                }
            }
        }
        return childCount
    }

    @JvmStatic
    fun getCenterYChild(recyclerView: RecyclerView): View? {
        val childCount = recyclerView.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = recyclerView.getChildAt(i)
                if (isChildInCenterY(recyclerView, child)) {
                    return child
                }
            }
        }
        return null
    }

    @JvmStatic
    fun getCenterYChildPosition(recyclerView: RecyclerView): Int {
        val childCount = recyclerView.childCount
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = recyclerView.getChildAt(i)
                if (isChildInCenterY(recyclerView, child)) {
                    return recyclerView.getChildAdapterPosition(child)
                }
            }
        }
        return childCount
    }

    @JvmStatic
    fun isChildInCenterX(recyclerView: RecyclerView, view: View): Boolean {
        val childCount = recyclerView.childCount
        val lvLocationOnScreen = IntArray(2)
        val vLocationOnScreen = IntArray(2)
        recyclerView.getLocationOnScreen(lvLocationOnScreen)
        val middleX = lvLocationOnScreen[0] + recyclerView.width / 2
        if (childCount > 0) {
            view.getLocationOnScreen(vLocationOnScreen)
            if (vLocationOnScreen[0] <= middleX && vLocationOnScreen[0] + view.width >= middleX) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isChildInCenterY(recyclerView: RecyclerView, view: View): Boolean {
        val childCount = recyclerView.childCount
        val lvLocationOnScreen = IntArray(2)
        val vLocationOnScreen = IntArray(2)
        recyclerView.getLocationOnScreen(lvLocationOnScreen)
        val middleY = lvLocationOnScreen[1] + recyclerView.height / 2
        if (childCount > 0) {
            view.getLocationOnScreen(vLocationOnScreen)
            if (vLocationOnScreen[1] <= middleY && vLocationOnScreen[1] + view.height >= middleY) {
                return true
            }
        }
        return false
    }
}
