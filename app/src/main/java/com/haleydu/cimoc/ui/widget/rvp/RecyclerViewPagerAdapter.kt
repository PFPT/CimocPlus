package com.haleydu.cimoc.ui.widget.rvp
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewPagerAdapter<VH : RecyclerView.ViewHolder>(
    private val mViewPager: RecyclerViewPager,
    var mAdapter: RecyclerView.Adapter<VH>
) : RecyclerView.Adapter<VH>() {

    init {
        setHasStableIds(mAdapter.hasStableIds())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return mAdapter.onCreateViewHolder(parent, viewType)
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.registerAdapterDataObserver(observer)
        mAdapter.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        mAdapter.unregisterAdapterDataObserver(observer)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        mAdapter.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: VH): Boolean {
        return mAdapter.onFailedToRecycleView(holder)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        mAdapter.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        mAdapter.onViewDetachedFromWindow(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mAdapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mAdapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        mAdapter.onBindViewHolder(holder, position)
        val itemView = holder.itemView
        val lp: ViewGroup.LayoutParams
        if (itemView.layoutParams == null) {
            lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            lp = itemView.layoutParams
            if (mViewPager.layoutManager!!.canScrollHorizontally()) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        itemView.layoutParams = lp
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
        mAdapter.setHasStableIds(hasStableIds)
    }

    override fun getItemCount(): Int {
        return mAdapter.itemCount
    }

    override fun getItemViewType(position: Int): Int {
        return mAdapter.getItemViewType(position)
    }

    override fun getItemId(position: Int): Long {
        return mAdapter.getItemId(position)
    }
}
