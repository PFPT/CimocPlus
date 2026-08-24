package com.haleydu.cimoc.ui.explore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ItemExploreLoadFooterBinding

class ExploreLoadAdapter : RecyclerView.Adapter<ExploreLoadAdapter.Holder>() {

    companion object {
        const val VIEW_TYPE = 2026082002
    }

    enum class State {
        HIDDEN, LOADING, END
    }

    var state: State = State.HIDDEN
        set(value) {
            if (field == value) return
            val oldCount = itemCount
            field = value
            val newCount = itemCount
            when {
                oldCount == 0 && newCount == 1 -> notifyItemInserted(0)
                oldCount == 1 && newCount == 0 -> notifyItemRemoved(0)
                oldCount == 1 && newCount == 1 -> notifyItemChanged(0)
            }
        }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE
    }

    override fun getItemCount(): Int {
        return if (state == State.HIDDEN) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemExploreLoadFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        when (state) {
            State.LOADING -> {
                holder.binding.exploreLoadProgress.visibility = View.VISIBLE
                holder.binding.exploreLoadText.visibility = View.VISIBLE
                holder.binding.exploreLoadText.setText(R.string.explore_loading_more)
            }
            State.END -> {
                holder.binding.exploreLoadProgress.visibility = View.GONE
                holder.binding.exploreLoadText.visibility = View.VISIBLE
                holder.binding.exploreLoadText.setText(R.string.explore_no_more)
            }
            State.HIDDEN -> {
                holder.binding.exploreLoadProgress.visibility = View.GONE
                holder.binding.exploreLoadText.visibility = View.GONE
            }
        }
    }

    class Holder(val binding: ItemExploreLoadFooterBinding) : RecyclerView.ViewHolder(binding.root)
}
