package com.haleydu.cimoc.fresco;

import android.content.Context;
import android.graphics.Bitmap;

import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import okhttp3.Headers;
import okhttp3.OkHttpClient;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class ImagePipelineFactoryBuilder {

    public static ImagePipelineFactory build(Context context, Headers header, boolean down, OkHttpClient client) {
        OkHttpNetworkFetcher fetcher = client != null ? new OkHttpNetworkFetcher(client, header) : null;
        return build(context, down, fetcher);
    }

    public static ImagePipelineFactory build(Context context, boolean down, OkHttpNetworkFetcher fetcher) {
        ImagePipelineConfig.Builder builder =
                ImagePipelineConfig.newBuilder(context.getApplicationContext())
                        .setDownsampleEnabled(down)
                        .setBitmapsConfig(down ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
        if (fetcher != null) {
            builder.setNetworkFetcher(fetcher);
        }
        return new ImagePipelineFactory(builder.build());
    }

}
