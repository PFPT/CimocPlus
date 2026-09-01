package com.haleydu.cimoc.fresco

import android.content.Context
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier
import com.facebook.imagepipeline.core.ImagePipelineFactory
import okhttp3.Headers
import okhttp3.OkHttpClient

object ControllerBuilderSupplierFactory {

    @JvmStatic
    fun get(context: Context, factory: ImagePipelineFactory): PipelineDraweeControllerBuilderSupplier {
        return PipelineDraweeControllerBuilderSupplier(context.applicationContext, factory, null)
    }

    @JvmStatic
    fun get(context: Context, header: Headers?, client: OkHttpClient): PipelineDraweeControllerBuilder {
        val factory = ImagePipelineFactoryBuilder.build(context, header, false, client)
        return PipelineDraweeControllerBuilderSupplier(context.applicationContext, factory, null).get()
    }
}
