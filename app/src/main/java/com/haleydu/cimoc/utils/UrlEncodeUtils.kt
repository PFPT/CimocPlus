package com.haleydu.cimoc.utils

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

object UrlEncodeUtils {

    @JvmStatic
    fun urlEncodeChinese(url: String): String {
        var result = url
        try {
            val matcher = Pattern.compile("[\\u4e00-\\u9fa5]").matcher(result)
            var tmp = ""
            while (matcher.find()) {
                tmp = matcher.group()
                result = result.replace(tmp.toRegex(), URLEncoder.encode(tmp, "UTF-8"))
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return result
    }
}
