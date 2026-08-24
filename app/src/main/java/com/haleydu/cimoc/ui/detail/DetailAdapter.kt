package com.haleydu.cimoc.ui.detail
import com.haleydu.cimoc.ui.common.BaseAdapter
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ItemChapterBinding
import com.haleydu.cimoc.model.Chapter

class DetailAdapter(
    context: Context,
    list: MutableList<Chapter>
) : BaseAdapter<Chapter>(context, list) {

    var reversed: Boolean = false
        private set

    private var last: String? = null
    private val textPaint = TextPaint().apply {
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
        isAntiAlias = true
        textSize = 40f
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
    }
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.transparent)
    }

    override fun getItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildLayoutPosition(view)
                if (position == RecyclerView.NO_POSITION) {
                    outRect.set(0, 0, 0, 0)
                    return
                }
                val offset = parent.width / 40
                if (isFirst(position)) {
                    outRect.set(offset, 50, offset, (offset * 1.5).toInt())
                } else {
                    outRect.set(offset, 0, offset, (offset * 1.5).toInt())
                }
            }

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                super.onDraw(c, parent, state)
                val count = parent.childCount
                for (i in 0 until count) {
                    val view = parent.getChildAt(i)
                    val position = parent.getChildLayoutPosition(view)
                    if (position == RecyclerView.NO_POSITION || !isFirst(position)) continue
                    val top = view.top - 60f
                    val bottom = view.top - 30f
                    val rect = Rect(
                        parent.paddingLeft,
                        top.toInt(),
                        parent.width - parent.paddingRight,
                        bottom.toInt()
                    )
                    c.drawRect(rect, paint)
                    val fontMetrics = textPaint.fontMetrics
                    val baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2
                    textPaint.textAlign = Paint.Align.CENTER
                    c.drawText(getGroupName(position), rect.centerX().toFloat(), baseline, textPaint)
                }
            }
        }
    }

    override fun reverse() {
        reversed = !reversed
        super.reverse()
    }

    fun isFirst(position: Int): Boolean {
        if (position < 0 || position >= mDataSet.size) return false
        if (mDataSet[position].sourceGroup.isEmpty()) return false
        if (position == 0) return true
        return mDataSet[position - 1].sourceGroup != mDataSet[position].sourceGroup
    }

    fun getGroupName(position: Int): String {
        return mDataSet[position].sourceGroup
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ChapterHolder(ItemChapterBinding.inflate(mInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val chapter = mDataSet[position]
        val viewHolder = holder as ChapterHolder
        viewHolder.binding.root.text = chapter.title
        viewHolder.binding.root.setDownload(chapter.isComplete)
        viewHolder.binding.root.isSelected = chapter.path != null && chapter.path == last
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager as GridLayoutManager
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (isFirst(position)) manager.spanCount else 1
            }
        }
    }

    fun setLast(value: String?) {
        if (value == null || value == last) return
        val temp = last
        last = value
        for (i in mDataSet.indices) {
            val path = mDataSet[i].path
            if (path == last || path == temp) {
                notifyItemChanged(i)
            }
        }
    }

    private class ChapterHolder(val binding: ItemChapterBinding) : BaseViewHolder(binding.root)
}
