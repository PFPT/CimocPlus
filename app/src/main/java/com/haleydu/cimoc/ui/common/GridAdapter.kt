package com.haleydu.cimoc.ui.common
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.haleydu.cimoc.App
import com.haleydu.cimoc.databinding.ItemGridBinding
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.global.FastClick
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.utils.FrescoUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GridAdapter @JvmOverloads constructor(
    context: Context,
    private val cardWidthDp: Int? = null
) : PagingDataAdapter<MiniComic, GridAdapter.Holder>(DIFF) {

    companion object {
        const val TYPE_GRID = 2016101213

        private val DIFF = object : DiffUtil.ItemCallback<MiniComic>() {
            override fun areItemsTheSame(oldItem: MiniComic, newItem: MiniComic): Boolean {
                return oldItem.source == newItem.source && oldItem.cid == newItem.cid
            }

            override fun areContentsTheSame(oldItem: MiniComic, newItem: MiniComic): Boolean {
                return oldItem.title == newItem.title &&
                    oldItem.cover == newItem.cover &&
                    oldItem.isHighlight == newItem.isHighlight &&
                    oldItem.source == newItem.source
            }
        }
    }

    private val inflater = LayoutInflater.from(context)
    private val items = ArrayList<MiniComic>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var provider: ControllerBuilderProvider? = null
    private var titleGetter: SourceManager.TitleGetter? = null
    private var symbol = false
    private var clickListener: BaseAdapter.OnItemClickListener? = null
    private var longClickListener: BaseAdapter.OnItemLongClickListener? = null

    val dateSet: List<MiniComic>
        get() {
            val snap = snapshot().items.mapNotNull { it }
            return snap.ifEmpty { items.toList() }
        }

    fun submitPaging(data: PagingData<MiniComic>) {
        scope.launch { submitData(data) }
    }

    fun submitList(list: List<MiniComic>) {
        setData(list)
    }

    private fun publish() {
        scope.launch { submitData(PagingData.from(items.toList())) }
    }

    fun comicAt(position: Int): MiniComic {
        return peek(position) ?: items.getOrNull(position)
            ?: throw IndexOutOfBoundsException("position $position")
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemGridBinding.inflate(inflater, parent, false)
        val dp = cardWidthDp
        if (dp != null) {
            val width = (dp * parent.resources.displayMetrics.density).toInt()
            binding.root.layoutParams = RecyclerView.LayoutParams(
                width,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val comic = comicAt(position)
        holder.binding.itemGridTitle.text = comic.title
        holder.binding.itemGridSubtitle.text = titleGetter?.getTitle(comic.source).orEmpty()
        holder.binding.itemGridImage.transitionName = "comic_cover_${comic.source}_${comic.cid}"
        bindCover(holder, comic)
        holder.binding.itemGridSymbol.visibility =
            if (symbol && comic.isHighlight) View.VISIBLE else View.INVISIBLE
        holder.itemView.setOnClickListener { view ->
            val pos = holder.bindingAdapterPosition
            if (clickListener != null && FastClick.isClickValid() && pos != RecyclerView.NO_POSITION) {
                clickListener?.onItemClick(view, pos)
            }
        }
        holder.itemView.setOnLongClickListener { view ->
            val listener = longClickListener ?: return@setOnLongClickListener false
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) {
                false
            } else {
                listener.onItemLongClick(view, pos)
            }
        }
    }

    fun setProvider(provider: ControllerBuilderProvider?) {
        this.provider = provider
    }

    fun setTitleGetter(getter: SourceManager.TitleGetter?) {
        titleGetter = getter
    }

    fun setSymbol(symbol: Boolean) {
        this.symbol = symbol
    }

    fun setOnItemClickListener(listener: BaseAdapter.OnItemClickListener?) {
        clickListener = listener
    }

    fun setOnItemLongClickListener(listener: BaseAdapter.OnItemLongClickListener?) {
        longClickListener = listener
    }

    fun getItemDecoration(): RecyclerView.ItemDecoration {
        return if (cardWidthDp != null) {
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val space = (4 * parent.resources.displayMetrics.density).toInt()
                    outRect.set(space, 0, space, 0)
                }
            }
        } else {
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val offset = parent.width / 90
                    outRect.set(offset, 0, offset, (2.8 * offset).toInt())
                }
            }
        }
    }

    fun setData(collection: Collection<*>) {
        items.clear()
        items.addAll(asComics(collection))
        publish()
    }

    fun add(comic: MiniComic) {
        items.add(comic)
        publish()
    }

    fun add(location: Int, comic: MiniComic) {
        items.add(location.coerceIn(0, items.size), comic)
        publish()
    }

    fun addAll(collection: Collection<*>) {
        addAll(items.size, collection)
    }

    fun addAll(location: Int, collection: Collection<*>) {
        val comics = asComics(collection)
        if (comics.isEmpty()) {
            return
        }
        items.addAll(location.coerceIn(0, items.size), comics)
        publish()
    }

    fun remove(comic: MiniComic): Boolean {
        val index = items.indexOfFirst { sameItem(it, comic) }
        if (index < 0) {
            return false
        }
        items.removeAt(index)
        publish()
        return true
    }

    fun removeItemById(id: Long) {
        val removed = items.removeAll { it.id != null && it.id == id }
        if (removed) {
            publish()
        }
    }

    fun exist(comic: MiniComic): Boolean {
        return items.any { sameItem(it, comic) }
    }

    fun contains(comic: MiniComic): Boolean {
        return exist(comic)
    }

    fun clear() {
        items.clear()
        publish()
    }

    fun findFirstNotHighlight(): Int {
        if (!symbol) {
            return 0
        }
        var count = 0
        for (comic in items) {
            if (!comic.isHighlight) {
                break
            }
            count++
        }
        return count
    }

    fun cancelAllHighlight() {
        var count = 0
        for (comic in items) {
            if (!comic.isHighlight) {
                break
            }
            comic.isHighlight = false
            count++
        }
        if (count > 0) {
            notifyItemRangeChanged(0, count)
        }
    }

    fun moveItemTop(comic: MiniComic) {
        val index = items.indexOfFirst { sameItem(it, comic) }
        if (index < 0) {
            return
        }
        val item = items.removeAt(index)
        items.add(findFirstNotHighlight(), item)
        publish()
    }

    private fun bindCover(holder: Holder, comic: MiniComic) {
        val builderProvider = provider ?: return
        var request: ImageRequest? = null
        try {
            val cover = comic.cover
            val prefs = EntryPointAccessors.fromApplication(
                holder.itemView.context.applicationContext,
                AppEntryPoint::class.java
            ).preferenceManager()
            val wifiOnlyConnect = prefs.getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)
            val wifiOnlyCover = prefs.getBoolean(PreferenceManager.PREF_OTHER_LOADCOVER_ONLY_WIFI, false)
            val wifiEnabled = App.getManager_wifi().isWifiEnabled
            val resize = ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3)
            request = if (!wifiEnabled && (wifiOnlyConnect || wifiOnlyCover)) {
                if (FrescoUtils.isCached(cover)) {
                    ImageRequestBuilder
                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(cover)))
                        .setResizeOptions(resize)
                        .build()
                } else {
                    null
                }
            } else if (FrescoUtils.isCached(cover)) {
                ImageRequestBuilder
                    .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(cover)))
                    .setResizeOptions(resize)
                    .build()
            } else {
                ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(cover))
                    .setResizeOptions(resize)
                    .build()
            }
        } catch (_: Exception) {
        }
        val controller = builderProvider.get(comic.source)
            .setOldController(holder.binding.itemGridImage.controller)
            .setImageRequest(request)
            .build()
        holder.binding.itemGridImage.controller = controller
    }

    private fun replaceItems(list: List<MiniComic>?) {
        items.clear()
        if (list != null) {
            items.addAll(list)
        }
    }

    private fun asComics(collection: Collection<*>): List<MiniComic> {
        return collection.mapNotNull { it as? MiniComic }
    }

    private fun sameItem(a: MiniComic, b: MiniComic): Boolean {
        val idA = a.id
        val idB = b.id
        if (idA != null && idB != null && idA == idB) {
            return true
        }
        return a.source == b.source && a.cid == b.cid
    }

    class Holder(val binding: ItemGridBinding) : RecyclerView.ViewHolder(binding.root)
}
