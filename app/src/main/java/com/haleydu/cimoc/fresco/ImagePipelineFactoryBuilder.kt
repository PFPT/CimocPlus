package com.haleydu.cimoc.fresco

import android.content.Context
import android.graphics.Bitmap
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImagePipelineFactory
import okhttp3.Headers
import okhttp3.OkHttpClient

object ImagePipelineFactoryBuilder {

    @JvmStatic
    fun build(context: Context, header: Headers?, down: Boolean, client: OkHttpClient?): ImagePipelineFactory {
        val fetcher = if (client != null) OkHttpNetworkFetcher(client, header) else null
        return build(context, down, fetcher)
    }

    @JvmStatic
    fun build(context: Context, down: Boolean, fetcher: OkHttpNetworkFetcher?): ImagePipelineFactory {
        val builder = ImagePipelineConfig.newBuilder(context.applicationContext)
            .setDownsampleEnabled(down)
            .setBitmapsConfig(if (down) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888)
        if (fetcher != null) {
            builder.setNetworkFetcher(fetcher)
        }
        return ImagePipelineFactory(builder.build())
    }
}
