package com.haleydu.cimoc.ui.widget
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PreCacheLayoutManager : LinearLayoutManager {

    private var mExtraSpace = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)

    fun setExtraSpace(extraSpace: Int) {
        mExtraSpace = extraSpace
    }

    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        var extra = 0
        if (mExtraSpace > 0) {
            extra = if (orientation == HORIZONTAL) {
                mExtraSpace * width
            } else {
                mExtraSpace * height
            }
        }
        extraLayoutSpace[0] = extra
        extraLayoutSpace[1] = extra
    }
}
