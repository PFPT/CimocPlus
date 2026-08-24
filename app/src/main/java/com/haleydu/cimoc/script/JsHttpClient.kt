package com.haleydu.cimoc.script

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsHttpClient @Inject constructor(
    httpClient: OkHttpClient
) {
    private val client = httpClient.newBuilder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun get(url: String): String = get(url, null)

    fun get(url: String, headersJson: Any?): String {
        validateUrl(url)
        val builder = Request.Builder().url(url)
        applyHeaders(builder, headersJson)
        return execute(builder.build())
    }

    fun post(url: String, body: String): String = post(url, body, null)

    fun post(url: String, body: String, headersJson: Any?): String {
        validateUrl(url)
        val builder = Request.Builder().url(url)
        applyHeaders(builder, headersJson)
        val type = headerValue(headersJson, "Content-Type")
            ?: "application/x-www-form-urlencoded; charset=utf-8"
        builder.post(body.toRequestBody(type.toMediaType()))
        return execute(builder.build())
    }

    fun validateUrl(url: String) {
        val value = url.trim()
        val lower = value.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw IllegalArgumentException("blocked url")
        }
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("http ${response.code}")
            }
            return body
        }
    }

    private fun applyHeaders(builder: Request.Builder, headersJson: Any?) {
        val json = jsonObject(headersJson) ?: return
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            builder.header(key, json.optString(key))
        }
    }

    private fun headerValue(headersJson: Any?, name: String): String? {
        val json = jsonObject(headersJson) ?: return null
        return if (json.has(name)) json.optString(name) else null
    }

    private fun jsonObject(headersJson: Any?): JSONObject? {
        if (headersJson == null) return null
        val text = headersJson.toString()
        if (text.isEmpty() || text == "undefined" || text == "null") return null
        return JSONObject(text)
    }
}
