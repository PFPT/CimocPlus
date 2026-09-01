package com.haleydu.cimoc.ui.detail
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.databinding.ItemChapterBinding
import com.haleydu.cimoc.databinding.ItemSelectBinding
import com.haleydu.cimoc.misc.Switcher
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.widget.ChapterButton

class ChapterAdapter(context: Context, list: MutableList<Switcher<Chapter>>) : BaseAdapter<Switcher<Chapter>>(context, list) {

    private var isButtonMode = false

    override fun getItemViewType(position: Int): Int {
        return if (isButtonMode) TYPE_BUTTON else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            ItemHolder(ItemSelectBinding.inflate(mInflater, parent, false))
        } else {
            ButtonHolder(ItemChapterBinding.inflate(mInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val switcher = mDataSet[position]
        if (isButtonMode) {
            val viewHolder = holder as ButtonHolder
            viewHolder.chapterButton.text = switcher.element.title
            if (switcher.element.download) {
                viewHolder.chapterButton.setDownload(true)
                viewHolder.chapterButton.isSelected = false
            } else {
                viewHolder.chapterButton.setDownload(false)
                viewHolder.chapterButton.isSelected = switcher.isEnable
            }
        } else {
            val viewHolder = holder as ItemHolder
            viewHolder.binding.itemSelectTitle.text = switcher.element.title
            viewHolder.binding.itemSelectCheckbox.isEnabled = !switcher.element.download
            viewHolder.binding.itemSelectCheckbox.isChecked = switcher.isEnable
        }
    }

    fun setButtonMode(enable: Boolean) {
        isButtonMode = enable
    }

    override fun getItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val offset = parent.width / 40
                outRect.set(offset, 0, offset, (offset * 1.5).toInt())
            }
        }
    }

    override fun isClickValid(): Boolean {
        return true
    }

    class ItemHolder(val binding: ItemSelectBinding) : BaseViewHolder(binding.root)

    class ButtonHolder(binding: ItemChapterBinding) : BaseViewHolder(binding.root) {
        val chapterButton: ChapterButton = binding.root as ChapterButton
    }

    companion object {
        private const val TYPE_ITEM = 2017030222
        private const val TYPE_BUTTON = 2017030223
    }
}
