package com.haleydu.cimoc.utils

import okhttp3.Request

object HttpUtils {

    @JvmStatic
    fun getSimpleMobileRequest(url: String): Request {
        return Request.Builder()
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1"
            )
            .url(url)
            .build()
    }
}
