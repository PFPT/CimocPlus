package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.DialogReaderChapterBinding
import com.haleydu.cimoc.model.Chapter

fun interface OnChapterSelect {
    fun onSelect(path: String)
}

class ReaderChapterSheet(
    context: Context,
    chapters: List<Chapter>,
    currentPath: String?,
    onSelect: OnChapterSelect
) : BottomSheetDialog(context, R.style.ReaderBottomSheetDialog) {

    init {
        val binding = DialogReaderChapterBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        val sheetHeight = (context.resources.displayMetrics.heightPixels * 0.6f).toInt()
        binding.readerChapterRecycler.layoutParams.height = sheetHeight
        binding.readerChapterRecycler.layoutManager = LinearLayoutManager(context)
        val currentIndex = chapters.indexOfFirst { it.path == currentPath }
        binding.readerChapterRecycler.adapter = Adapter(chapters, currentPath) { path ->
            dismiss()
            onSelect.onSelect(path)
        }
        if (currentIndex >= 0) {
            binding.readerChapterRecycler.post {
                binding.readerChapterRecycler.scrollToPosition(currentIndex)
            }
        }
        behavior.peekHeight = (context.resources.displayMetrics.heightPixels * 0.6f).toInt()
        behavior.skipCollapsed = true
    }

    private class Adapter(
        private val chapters: List<Chapter>,
        private val currentPath: String?,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<Adapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reader_chapter, parent, false) as TextView
            return Holder(view, view.currentTextColor)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val chapter = chapters[position]
            val selected = chapter.path == currentPath
            holder.title.text = chapter.title
            holder.title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            holder.title.setTextColor(
                if (selected) {
                    ContextCompat.getColor(holder.itemView.context, R.color.colorPrimaryBlue)
                } else {
                    holder.defaultColor
                }
            )
            holder.itemView.setOnClickListener { onSelect(chapter.path) }
        }

        override fun getItemCount(): Int = chapters.size

        class Holder(val title: TextView, val defaultColor: Int) : RecyclerView.ViewHolder(title)
    }
}
