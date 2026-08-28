package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.dispose
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Size
import com.davemorrissey.labs.subscaleview.ImageSource
import com.haleydu.cimoc.R
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.widget.OnTapGestureListener
import okhttp3.Headers

class ReaderAdapter(context: Context, list: MutableList<ImageUrl>) : BaseAdapter<ImageUrl>(context, list) {

    companion object {
        const val READER_PAGE = 0
        const val READER_STREAM = 1
        private const val TYPE_LOADING = 2016101214
        private const val TYPE_IMAGE = 2016101215
    }

    private var imageLoader: ImageLoader? = null
    private var headers: Headers? = null
    private var tapGestureListener: OnTapGestureListener? = null
    private var lazyLoadListener: OnLazyLoadListener? = null
    private var reader = READER_PAGE
    private var isVertical = false
    private var isPaging = false
    private var isPagingReverse = false
    private var isWhiteEdge = false
    private var isBanTurn = false
    private var isDoubleTap = true
    private var isCloseAutoResizeImage = false
    private var isStreamTile = false
    private var scaleFactor = 2.0f

    override fun getItemViewType(position: Int): Int {
        return if (mDataSet[position].isLazy) TYPE_LOADING else TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = when {
            viewType != TYPE_IMAGE -> R.layout.item_loading
            reader == READER_PAGE -> R.layout.item_picture
            else -> R.layout.item_picture_stream
        }
        return ImageHolder(mInflater.inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val imageUrl = mDataSet[position]
        val imageHolder = holder as ImageHolder
        imageHolder.dispose()
        if (imageUrl.isLazy) {
            if (!imageUrl.isLoading && lazyLoadListener != null) {
                imageUrl.isLoading = true
                lazyLoadListener?.onLoad(imageUrl)
            }
            return
        }
        bindImage(imageHolder, imageUrl)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? ImageHolder)?.dispose()
    }

    private fun bindImage(holder: ImageHolder, imageUrl: ImageUrl) {
        val urls = imageUrl.urls
            ?.map { it?.trim().orEmpty() }
            ?.filter { it.isNotEmpty() }
            ?: return
        if (urls.isEmpty()) {
            return
        }
        holder.showError(false)
        loadImage(holder, imageUrl, urls, 0)
    }

    private fun loadImage(holder: ImageHolder, imageUrl: ImageUrl, urls: List<String>, index: Int) {
        val loader = imageLoader ?: return
        if (index >= urls.size) {
            imageUrl.isSuccess = false
            holder.showError(true)
            holder.onRetry = {
                if (imageUrl.isSuccess) {
                    false
                } else {
                    holder.showError(false)
                    loadImage(holder, imageUrl, urls, 0)
                    true
                }
            }
            return
        }
        val url = normalizeUrl(urls[index])
        val transformation = MangaTransformation(
            imageUrl,
            isPaging,
            isPagingReverse,
            isWhiteEdge,
            isStreamTile
        )
        val requestBuilder = ImageRequest.Builder(mContext)
            .data(url)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .transformations(transformation)
        applyHeaders(requestBuilder)
        holder.onRetry = {
            if (imageUrl.isSuccess) {
                false
            } else {
                holder.showError(false)
                loadImage(holder, imageUrl, urls, 0)
                true
            }
        }

        if (reader == READER_PAGE) {
            val pageView = holder.imageView as? ReaderPageImageView ?: return
            pageView.tapListener = tapGestureListener
            pageView.alwaysBlockParent = isBanTurn
            pageView.setDoubleTapZoomScale(scaleFactor)
            pageView.isQuickScaleEnabled = isDoubleTap
            requestBuilder.target(
                onSuccess = { result ->
                    if (!isBound(holder, imageUrl)) {
                        return@target
                    }
                    imageUrl.isSuccess = true
                    holder.showError(false)
                    val bitmap = drawableToBitmap(result)
                    if (bitmap != null && !bitmap.isRecycled) {
                        pageView.setImage(ImageSource.bitmap(bitmap))
                    }
                    pageView.tag = imageUrl.id
                },
                onError = {
                    if (!isBound(holder, imageUrl)) {
                        return@target
                    }
                    loadImage(holder, imageUrl, urls, index + 1)
                }
            )
            holder.disposable?.dispose()
            holder.disposable = loader.enqueue(requestBuilder.build())
        } else {
            val imageView = holder.imageView as? ImageView ?: return
            bindStreamTap(imageView)
            requestBuilder.size(Size.ORIGINAL)
            val request = requestBuilder
                .target(
                    onSuccess = { result ->
                        if (!isBound(holder, imageUrl)) {
                            return@target
                        }
                        imageUrl.isSuccess = true
                        holder.showError(false)
                        imageView.setImageDrawable(result)
                        val bitmap = drawableToBitmap(result)
                        if (bitmap != null && bitmap.height > 0) {
                            applyStreamWrapContent(holder, imageView)
                        }
                    },
                    onError = {
                        if (!isBound(holder, imageUrl)) {
                            return@target
                        }
                        loadImage(holder, imageUrl, urls, index + 1)
                    }
                )
                .build()
            holder.disposable?.dispose()
            holder.disposable = loader.enqueue(request)
        }
    }

    private fun isBound(holder: ImageHolder, imageUrl: ImageUrl): Boolean {
        val position = holder.bindingAdapterPosition
        return position != RecyclerView.NO_POSITION &&
            position < mDataSet.size &&
            mDataSet[position] === imageUrl
    }

    private fun applyStreamWrapContent(holder: ImageHolder, imageView: ImageView) {
        val imageLp = imageView.layoutParams ?: return
        val itemLp = holder.itemView.layoutParams
        if (isVertical) {
            imageLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            if (itemLp != null) {
                itemLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        } else {
            imageLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            if (itemLp != null) {
                itemLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        imageView.layoutParams = imageLp
        if (itemLp != null) {
            holder.itemView.layoutParams = itemLp
        }
        imageView.adjustViewBounds = true
        imageView.requestLayout()
        holder.itemView.requestLayout()
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("//")) "https:$url" else url
    }

    private fun applyHeaders(builder: ImageRequest.Builder) {
        val values = headers ?: return
        for (i in 0 until values.size) {
            builder.addHeader(values.name(i), values.value(i))
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap ?: return null
            if (bitmap.isRecycled) return null
            val config = bitmap.config ?: Bitmap.Config.RGB_565
            return bitmap.copy(config, false) ?: bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    fun setImageLoader(loader: ImageLoader) {
        imageLoader = loader
    }

    private fun bindStreamTap(imageView: ImageView) {
        var rawX = 0f
        var rawY = 0f
        imageView.isClickable = true
        imageView.isLongClickable = true
        imageView.setOnTouchListener { _, event ->
            rawX = event.rawX
            rawY = event.rawY
            false
        }
        imageView.setOnClickListener {
            tapGestureListener?.onSingleTap(rawX, rawY)
        }
        imageView.setOnLongClickListener {
            tapGestureListener?.onLongPress(rawX, rawY)
            true
        }
    }

    fun setHeaders(value: Headers?) {
        headers = value
    }

    fun prefetch(urls: List<String>) {
        val loader = imageLoader ?: return
        for (raw in urls) {
            if (raw.isEmpty()) continue
            val builder = ImageRequest.Builder(mContext).data(normalizeUrl(raw))
            applyHeaders(builder)
            loader.enqueue(builder.build())
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun evict(url: String) {
        val loader = imageLoader ?: return
        loader.memoryCache?.keys
            ?.filter { it.key.contains(url) }
            ?.forEach { loader.memoryCache?.remove(it) }
        loader.diskCache?.remove(url)
    }

    fun setTapGestureListener(listener: OnTapGestureListener?) {
        tapGestureListener = listener
    }

    fun setLazyLoadListener(listener: OnLazyLoadListener?) {
        lazyLoadListener = listener
    }

    fun setScaleFactor(factor: Float) {
        scaleFactor = factor
    }

    fun setDoubleTap(enable: Boolean) {
        isDoubleTap = enable
    }

    fun setBanTurn(block: Boolean) {
        isBanTurn = block
    }

    fun setVertical(vertical: Boolean) {
        isVertical = vertical
    }

    fun setPaging(paging: Boolean) {
        isPaging = paging
    }

    fun setPagingReverse(pagingReverse: Boolean) {
        isPagingReverse = pagingReverse
    }

    fun setCloseAutoResizeImage(closeAutoResizeImage: Boolean) {
        isCloseAutoResizeImage = closeAutoResizeImage
    }

    fun setWhiteEdge(whiteEdge: Boolean) {
        isWhiteEdge = whiteEdge
    }

    fun setStreamTile(streamTile: Boolean) {
        isStreamTile = streamTile
    }

    fun setReaderMode(mode: Int) {
        reader = mode
    }

    override fun getItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                if (reader == READER_STREAM) {
                    if (isVertical) outRect.set(0, 10, 0, 10) else outRect.set(10, 0, 10, 0)
                } else {
                    outRect.set(0, 0, 0, 0)
                }
            }
        }
    }

    fun getPositionByNum(current: Int, num: Int, reverse: Boolean): Int {
        var pos = current
        try {
            while (mDataSet[pos].num < num) {
                pos = if (reverse) pos - 1 else pos + 2
            }
        } catch (_: Exception) {
        }
        return pos
    }

    fun getPositionById(id: Long): Int {
        for (i in mDataSet.indices) {
            if (mDataSet[i].id == id) return i
        }
        return -1
    }

    fun update(id: Long, url: String?) {
        for (i in mDataSet.indices) {
            val imageUrl = mDataSet[i]
            if (imageUrl.id == id && imageUrl.isLoading) {
                if (url == null) {
                    imageUrl.isLoading = false
                    return
                }
                imageUrl.url = url
                imageUrl.isLoading = false
                imageUrl.isLazy = false
                notifyItemChanged(i)
                break
            }
        }
    }

    fun interface OnLazyLoadListener {
        fun onLoad(imageUrl: ImageUrl)
    }

    class ImageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: View? = view.findViewById(R.id.reader_image_view)
        private val errorView: TextView? = view.findViewById(R.id.reader_image_error)
        var disposable: Disposable? = null
        var onRetry: () -> Boolean = { false }

        init {
            errorView?.setOnClickListener { retry() }
        }

        fun retry(): Boolean = onRetry.invoke()

        fun showError(show: Boolean) {
            errorView?.visibility = if (show) View.VISIBLE else View.GONE
        }

        fun dispose() {
            disposable?.dispose()
            disposable = null
            val view = imageView
            if (view is ImageView) {
                view.dispose()
            }
        }
    }
}
