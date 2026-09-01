package com.haleydu.cimoc.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.cache.common.CacheKey
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.PipelineDraweeController
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.BasePostprocessor
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import java.io.File
import java.util.UUID

object FrescoUtils {

    private const val PHOTO_FRESCO = "frescocache"

    @JvmStatic
    fun httpImageUri(cover: String?): Uri? {
        if (cover == null) {
            return null
        }
        var value = cover.trim()
        val space = value.indexOf(' ')
        if (space > 0) {
            value = value.substring(0, space)
        }
        val comma = value.indexOf(',')
        if (comma > 0) {
            value = value.substring(0, comma)
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return null
        }
        return try {
            val uri = Uri.parse(value)
            val scheme = uri.scheme ?: return null
            if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) {
                null
            } else {
                uri
            }
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun loadUrl(
        url: String,
        draweeView: SimpleDraweeView,
        processor: BasePostprocessor?,
        width: Int,
        height: Int,
        listener: ControllerListener<in ImageInfo>?
    ) {
        load(Uri.parse(url), draweeView, processor, width, height, listener)
    }

    @JvmStatic
    fun loadFile(
        file: String,
        draweeView: SimpleDraweeView,
        processor: BasePostprocessor?,
        width: Int,
        height: Int,
        listener: ControllerListener<in ImageInfo>?
    ) {
        load(getFileUri(file), draweeView, processor, width, height, listener)
    }

    @JvmStatic
    fun loadFile(
        file: File,
        draweeView: SimpleDraweeView,
        processor: BasePostprocessor?,
        width: Int,
        height: Int,
        listener: ControllerListener<in ImageInfo>?
    ) {
        load(getFileUri(file), draweeView, processor, width, height, listener)
    }

    @JvmStatic
    fun loadRes(
        resId: Int,
        draweeView: SimpleDraweeView,
        processor: BasePostprocessor?,
        width: Int,
        height: Int,
        listener: ControllerListener<in ImageInfo>?
    ) {
        load(getResUri(resId), draweeView, processor, width, height, listener)
    }

    @JvmStatic
    fun load(
        uri: Uri,
        draweeView: SimpleDraweeView,
        processor: BasePostprocessor?,
        width: Int,
        height: Int,
        listener: ControllerListener<in ImageInfo>?
    ) {
        var resizeOptions: ResizeOptions? = null
        if (width > 0 && height > 0) {
            resizeOptions = ResizeOptions(width, height)
        }
        val request = ImageRequestBuilder.newBuilderWithSource(uri)
            .setPostprocessor(processor)
            .setResizeOptions(resizeOptions)
            .setProgressiveRenderingEnabled(true)
            .setRotationOptions(RotationOptions.autoRotate())
            .build()
        val controller = Fresco.newDraweeControllerBuilder()
            .setImageRequest(request)
            .setControllerListener(listener)
            .setOldController(draweeView.controller)
            .setAutoPlayAnimations(true)
            .build() as PipelineDraweeController
        draweeView.controller = controller
    }

    @JvmStatic
    fun getFileUri(file: File): Uri {
        return Uri.fromFile(file)
    }

    @JvmStatic
    fun getFileUri(filePath: String): Uri {
        return Uri.fromFile(File(filePath))
    }

    @JvmStatic
    fun getResUri(resId: Int): Uri {
        return Uri.parse("res://xxyy/$resId")
    }

    @JvmStatic
    fun setCircle(draweeView: SimpleDraweeView, bgColor: Int) {
        val roundingParams = RoundingParams.asCircle()
        roundingParams.overlayColor = bgColor
        draweeView.hierarchy.roundingParams = roundingParams
    }

    @JvmStatic
    fun prefetchToDiskCache(pipeline: ImagePipeline?, url: String?, callerContext: Any?) {
        if (pipeline == null || TextUtils.isEmpty(url)) {
            return
        }
        try {
            val request = ImageRequest.fromUri(Uri.parse(url)) ?: return
            pipeline.prefetchToDiskCache(request, callerContext)
        } catch (ignored: Exception) {
        }
    }

    @JvmStatic
    fun pause() {
        Fresco.getImagePipeline().pause()
    }

    @JvmStatic
    fun resume() {
        Fresco.getImagePipeline().resume()
    }

    @JvmStatic
    fun init(context: Context, cacheSizeInM: Int) {
        val diskCacheConfig = DiskCacheConfig.newBuilder(context)
            .setMaxCacheSize((cacheSizeInM * 1024 * 1024).toLong())
            .setBaseDirectoryName(PHOTO_FRESCO)
            .setBaseDirectoryPathSupplier { context.cacheDir }
            .build()
        val imageCacheStatsTracker = MyImageCacheStatsTracker()
        val config = ImagePipelineConfig.newBuilder(context)
            .setMainDiskCacheConfig(diskCacheConfig)
            .setImageCacheStatsTracker(imageCacheStatsTracker)
            .setDownsampleEnabled(true)
            .setBitmapsConfig(Bitmap.Config.RGB_565)
            .build()
        Fresco.initialize(context, config)
    }

    @JvmStatic
    fun clearDiskCache() {
        Fresco.getImagePipeline().clearDiskCaches()
    }

    @JvmStatic
    fun clearCacheByUrl(url: String) {
        val imagePipeline = Fresco.getImagePipeline()
        val uri = Uri.parse(url)
        imagePipeline.evictFromDiskCache(uri)
    }

    @JvmStatic
    @Throws(NullPointerException::class)
    fun getFileFromDiskCache(url: String): File? {
        var localFile: File? = null
        if (!TextUtils.isEmpty(url)) {
            val cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(ImageRequest.fromUri(url), null)
            if (ImagePipelineFactory.getInstance().mainFileCache.hasKey(cacheKey)) {
                val resource = ImagePipelineFactory.getInstance().mainFileCache.getResource(cacheKey)
                localFile = (resource as FileBinaryResource).file
            } else if (ImagePipelineFactory.getInstance().smallImageFileCache.hasKey(cacheKey)) {
                val resource = ImagePipelineFactory.getInstance().smallImageFileCache.getResource(cacheKey)
                localFile = (resource as FileBinaryResource).file
            }
        }
        return localFile
    }

    @JvmStatic
    fun copyCacheFile(url: String, dir: File, fileName: String): Boolean {
        val path = File(dir, fileName)
        return copyCacheFile(url, path)
    }

    @JvmStatic
    fun copyCacheFile(url: String, path: File?): Boolean {
        if (path == null) {
            return false
        }
        val file = getFileFromDiskCache(url) ?: return false
        if (path.isDirectory) {
            throw RuntimeException(path.toString() + "is a directory,you should call copyCacheFileToDir(String url,File dir)")
        }
        return file.renameTo(path)
    }

    @JvmStatic
    fun copyCacheFileToDir(url: String, dir: File?): File? {
        if (dir == null) {
            return null
        }
        if (!dir.isDirectory) {
            throw RuntimeException(dir.toString() + "is not a directory,you should call copyCacheFile(String url,File path)")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        var fileName = URLUtil.guessFileName(url, "", "")
        if (TextUtils.isEmpty(fileName)) {
            fileName = UUID.randomUUID().toString()
        }
        val newFile = File(dir, fileName)
        val isSuccess = copyCacheFile(url, newFile)
        return if (isSuccess) newFile else null
    }

    @JvmStatic
    fun isCached(url: String): Boolean {
        val imageRequest = ImageRequest.fromUri(url)
        val cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(imageRequest, null)
        return ImagePipelineFactory.getInstance().mainFileCache.hasKey(cacheKey)
    }

    @JvmStatic
    fun download(url: String, context: Context, dir: File, listener: DownloadListener) {
        val imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url)).build()
        val imagePipeline = Fresco.getImagePipeline()
        val dataSource = imagePipeline.prefetchToDiskCache(imageRequest, context, Priority.HIGH)
        dataSource.subscribe(object : BaseDataSubscriber<Void>() {
            override fun onNewResultImpl(dataSource: DataSource<Void>) {
                val file = copyCacheFileToDir(url, dir)
                clearCacheByUrl(url)
                if (file == null || !file.exists()) {
                    listener.onFail()
                } else {
                    listener.onSuccess(file)
                }
            }

            override fun onProgressUpdate(dataSource: DataSource<Void>) {
                super.onProgressUpdate(dataSource)
                listener.onProgress(dataSource.progress)
            }

            override fun onFailureImpl(dataSource: DataSource<Void>) {
                listener.onFail()
            }
        }, CallerThreadExecutor.getInstance())
    }

    class MyImageCacheStatsTracker : ImageCacheStatsTracker {
        override fun onBitmapCachePut(cacheKey: CacheKey) {}
        override fun onBitmapCacheHit(cacheKey: CacheKey) {}
        override fun onBitmapCacheMiss(cacheKey: CacheKey) {}
        override fun onMemoryCachePut(cacheKey: CacheKey) {}
        override fun onMemoryCacheHit(cacheKey: CacheKey) {}
        override fun onMemoryCacheMiss(cacheKey: CacheKey) {}
        override fun onStagingAreaHit(cacheKey: CacheKey) {}
        override fun onStagingAreaMiss(cacheKey: CacheKey) {}
        override fun onDiskCacheHit(cacheKey: CacheKey) {}
        override fun onDiskCacheMiss(cacheKey: CacheKey) {}
        override fun onDiskCacheGetFail(cacheKey: CacheKey) {}
        override fun onDiskCachePut(cacheKey: CacheKey) {}
        override fun registerBitmapMemoryCache(bitmapMemoryCache: MemoryCache<*, *>?) {}
        override fun registerEncodedMemoryCache(encodedMemoryCache: MemoryCache<*, *>?) {}
    }

    interface BitmapListener {
        fun onSuccess(bitmap: Bitmap)
        fun onFail()
    }

    interface DownloadListener {
        fun onSuccess(file: File)
        fun onFail()
        fun onProgress(progress: Float)
    }
}
