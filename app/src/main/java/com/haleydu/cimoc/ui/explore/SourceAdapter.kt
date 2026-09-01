package com.haleydu.cimoc.ui.explore
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.databinding.ItemSourceBinding
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.ui.common.BaseAdapter

class SourceAdapter(context: Context, list: MutableList<Source>) : BaseAdapter<Source>(context, list) {

    private var mOnItemCheckedListener: OnItemCheckedListener? = null
    @ColorInt
    private var color = -1
    private val invalidTypes = HashSet<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SourceHolder(ItemSourceBinding.inflate(mInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val source = mDataSet[position]
        val viewHolder = holder as SourceHolder
        viewHolder.binding.itemSourceTitle.text = source.title
        viewHolder.binding.itemSourceInvalid.visibility =
            if (invalidTypes.contains(source.type)) View.VISIBLE else View.GONE
        viewHolder.binding.itemSourceSwitch.isChecked = source.enable
        viewHolder.binding.itemSourceSwitch.setOnCheckedChangeListener { _, isChecked ->
            mOnItemCheckedListener?.onItemCheckedListener(isChecked, viewHolder.bindingAdapterPosition)
        }
        if (color != -1) {
            val thumbList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)),
                intArrayOf(Color.WHITE, color)
            )
            viewHolder.binding.itemSourceSwitch.thumbTintList = thumbList
            val trackList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)),
                intArrayOf(0x4C000000, (0x00FFFFFF and color or 0x4C000000))
            )
            viewHolder.binding.itemSourceSwitch.trackTintList = trackList
        }
    }

    override fun getItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val density = parent.resources.displayMetrics.density
                val h = (16 * density).toInt()
                val v = (6 * density).toInt()
                outRect.set(h, v, h, v)
            }
        }
    }

    fun setOnItemCheckedListener(listener: OnItemCheckedListener?) {
        mOnItemCheckedListener = listener
    }

    fun setColor(@ColorInt color: Int) {
        this.color = color
    }

    fun setInvalidTypes(types: Set<Int>?) {
        invalidTypes.clear()
        if (types != null) {
            invalidTypes.addAll(types)
        }
        notifyDataSetChanged()
    }

    interface OnItemCheckedListener {
        fun onItemCheckedListener(isChecked: Boolean, position: Int)
    }

    class SourceHolder(val binding: ItemSourceBinding) : BaseViewHolder(binding.root)
}
