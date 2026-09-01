package com.haleydu.cimoc.core

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.regex.Pattern

class Manga {

    class ParseErrorException : Exception()

    class NetworkErrorException : Exception()

    companion object {
        @JvmStatic
        fun applyHeaders(request: Request?, headers: Headers?): Request? {
            if (request == null || headers == null || headers.size == 0) {
                return request
            }
            val builder = request.newBuilder()
            for (i in 0 until headers.size) {
                val name = headers.name(i)
                if (request.header(name) == null) {
                    builder.header(name, headers.value(i))
                }
            }
            return builder.build()
        }

        @JvmStatic
        @Throws(NetworkErrorException::class)
        fun getResponseBody(client: OkHttpClient, request: Request): String {
            return getResponseBody(client, request, true)
        }

        @Throws(NetworkErrorException::class)
        private fun getResponseBody(client: OkHttpClient?, request: Request?, retry: Boolean): String {
            if (client == null || request == null) {
                throw NetworkErrorException()
            }
            var response: Response? = null
            try {
                response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodybytes = response.body!!.bytes()
                    var body = String(bodybytes)
                    val m = Pattern.compile("charset=([\\w\\-]+)").matcher(body)
                    if (m.find()) {
                        body = String(bodybytes, charset(m.group(1)!!))
                    }
                    return body
                } else if (retry) {
                    return getResponseBody(client, nextFallbackRequest(request), false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (retry) {
                    return getResponseBody(client, nextFallbackRequest(request), false)
                }
            } finally {
                response?.close()
            }
            val fallback = nextFallbackRequest(request)
            if (fallback !== request) {
                return getResponseBody(client, fallback, true)
            }
            throw NetworkErrorException()
        }

        private fun nextFallbackRequest(request: Request?): Request? {
            if (request == null) {
                return null
            }
            val fallbacks = request.header("X-Cimoc-Fallback")
            if (fallbacks.isNullOrEmpty()) {
                return request
            }
            val comma = fallbacks.indexOf(',')
            val nextUrl = if (comma < 0) fallbacks else fallbacks.substring(0, comma)
            val rest = if (comma < 0) "" else fallbacks.substring(comma + 1)
            val builder = request.newBuilder().url(nextUrl)
            if (rest.isEmpty()) {
                builder.removeHeader("X-Cimoc-Fallback")
            } else {
                builder.header("X-Cimoc-Fallback", rest)
            }
            return builder.build()
        }
    }
}
