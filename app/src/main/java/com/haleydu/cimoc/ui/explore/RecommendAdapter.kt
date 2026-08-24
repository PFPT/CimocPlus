package com.haleydu.cimoc.ui.explore
import com.haleydu.cimoc.ui.common.GridAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.databinding.ItemExploreRecommendBinding
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.MiniComic

class RecommendAdapter(
    context: Context,
    private val onComicClick: (MiniComic) -> Unit
) : RecyclerView.Adapter<RecommendAdapter.Holder>() {

    companion object {
        const val VIEW_TYPE = 2026082001
        private const val MAX_COUNT = 8
        private const val CARD_WIDTH_DP = 108
    }

    private val cardAdapter = GridAdapter(context, cardWidthDp = CARD_WIDTH_DP).apply {
        setOnItemClickListener { _, position ->
            onComicClick(comicAt(position))
        }
    }

    private var visible = false

    fun setProvider(provider: ControllerBuilderProvider) {
        cardAdapter.setProvider(provider)
    }

    fun setTitleGetter(getter: SourceManager.TitleGetter) {
        cardAdapter.setTitleGetter(getter)
    }

    fun setComics(list: List<MiniComic>) {
        val recs = list.take(MAX_COUNT)
        val current = cardAdapter.dateSet
        val same = recs.size == current.size && recs.indices.all { i ->
            current[i].source == recs[i].source && current[i].cid == recs[i].cid
        }
        if (!same) {
            cardAdapter.submitList(recs)
        }
        val show = recs.isNotEmpty()
        if (show == visible) {
            return
        }
        visible = show
        if (show) {
            notifyItemInserted(0)
        } else {
            notifyItemRemoved(0)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE
    }

    override fun getItemCount(): Int {
        return if (visible) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExploreRecommendBinding.inflate(inflater, parent, false)
        binding.exploreRecommendList.layoutManager =
            LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
        binding.exploreRecommendList.setHasFixedSize(true)
        binding.exploreRecommendList.isNestedScrollingEnabled = false
        binding.exploreRecommendList.itemAnimator = null
        binding.exploreRecommendList.addItemDecoration(cardAdapter.getItemDecoration())
        binding.exploreRecommendList.adapter = cardAdapter
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (holder.binding.exploreRecommendList.adapter !== cardAdapter) {
            holder.binding.exploreRecommendList.adapter = cardAdapter
        }
    }

    class Holder(val binding: ItemExploreRecommendBinding) : RecyclerView.ViewHolder(binding.root)
}
