package com.haleydu.cimoc.fresco

import android.content.Context
import android.util.SparseArray
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier
import com.facebook.imagepipeline.core.ImagePipeline
import com.haleydu.cimoc.data.SourceManager
import okhttp3.OkHttpClient

class ControllerBuilderProvider(
    private val mContext: Context,
    private val mHeaderGetter: SourceManager.HeaderGetter,
    private val mCover: Boolean,
    private val mHttpClient: OkHttpClient
) {
    private val mSupplierArray = SparseArray<PipelineDraweeControllerBuilderSupplier>()
    private val mPipelineArray = SparseArray<ImagePipeline>()

    fun get(type: Int): PipelineDraweeControllerBuilder {
        var supplier = mSupplierArray.get(type)
        if (supplier == null) {
            val factory = ImagePipelineFactoryBuilder.build(
                mContext,
                if (type < 0) null else mHeaderGetter.getHeader(type),
                mCover,
                mHttpClient
            )
            supplier = ControllerBuilderSupplierFactory.get(mContext, factory)
            mSupplierArray.put(type, supplier)
            mPipelineArray.put(type, factory.imagePipeline)
        }
        return supplier.get()
    }

    fun pause() {
        var i = 0
        while (i != mPipelineArray.size()) {
            mPipelineArray.valueAt(i).pause()
            ++i
        }
    }

    fun resume() {
        var i = 0
        while (i != mPipelineArray.size()) {
            mPipelineArray.valueAt(i).resume()
            ++i
        }
    }

    fun clear() {
        var i = 0
        while (i != mPipelineArray.size()) {
            mPipelineArray.valueAt(i).clearMemoryCaches()
            ++i
        }
    }
}
