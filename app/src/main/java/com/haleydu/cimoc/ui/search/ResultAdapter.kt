package com.haleydu.cimoc.ui.search
import com.haleydu.cimoc.ui.common.BaseAdapter
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.haleydu.cimoc.App
import com.haleydu.cimoc.databinding.ItemResultBinding
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.ui.search.ResultViewModel

class ResultAdapter(
    context: Context,
    list: MutableList<ResultViewModel.SearchGroup>
) : BaseAdapter<ResultViewModel.SearchGroup>(context, list) {

    private var provider: ControllerBuilderProvider? = null
    private var titleGetter: SourceManager.TitleGetter? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ResultViewHolder(ItemResultBinding.inflate(mInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val group = mDataSet[position]
        val comic = group.primary
        val viewHolder = holder as ResultViewHolder
        viewHolder.binding.resultComicTitle.text = comic.title
        viewHolder.binding.resultComicAuthor.text = comic.author
        viewHolder.binding.resultComicSource.text = sourceLabel(group)
        viewHolder.binding.resultComicUpdate.text = comic.update
        val cover = comic.cover
        if (!cover.isNullOrEmpty() && provider != null) {
            val request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(cover))
                .setResizeOptions(ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                .build()
            viewHolder.binding.resultComicImage.controller =
                provider!!.get(comic.source).setImageRequest(request).build()
        }
    }

    fun setProvider(provider: ControllerBuilderProvider) {
        this.provider = provider
    }

    fun setTitleGetter(getter: SourceManager.TitleGetter) {
        titleGetter = getter
    }

    override fun getItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val density = parent.resources.displayMetrics.density
                val h = (16 * density).toInt()
                val v = (6 * density).toInt()
                outRect.set(h, v, h, v)
            }
        }
    }

    private fun sourceLabel(group: ResultViewModel.SearchGroup): String {
        val getter = titleGetter ?: return ""
        val titles = group.comics.map { getter.getTitle(it.source) }.distinct()
        return if (titles.size <= 3) {
            titles.joinToString(" · ")
        } else {
            titles.take(2).joinToString(" · ") + " · +" + (titles.size - 2)
        }
    }

    private class ResultViewHolder(val binding: ItemResultBinding) : BaseViewHolder(binding.root)
}
