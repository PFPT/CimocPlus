package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class WebPageExtract(
    val title: String,
    val pageUrl: String,
    val imageUrls: List<String>,
    val nextUrl: String?
)

interface BrowserHandle {
    fun load(url: String)
    fun back(): Boolean
    fun forward()
    fun reload()
    fun canBack(): Boolean
    fun canForward(): Boolean
    fun currentUrl(): String
    fun title(): String
    suspend fun looksLikeComic(): Boolean
    suspend fun extract(): WebPageExtract
}

interface WebExtractSession {
    suspend fun extract(url: String): WebPageExtract
}

const val BrowserUserAgent =
    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

fun browserTarget(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    return "https://m.quark.cn/s?q=${encodeQuery(trimmed)}"
}

@Composable
expect fun rememberBrowserHandle(): BrowserHandle

@Composable
expect fun BrowserWebView(
    handle: BrowserHandle,
    url: String,
    modifier: Modifier,
    onLoading: (Boolean) -> Unit,
    onPage: (url: String, title: String) -> Unit
)

@Composable
expect fun rememberWebExtractSession(): WebExtractSession

private fun encodeQuery(value: String): String {
    val out = StringBuilder()
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt() and 0xFF
        val plain = (c in 'a'.code..'z'.code) ||
            (c in 'A'.code..'Z'.code) ||
            (c in '0'.code..'9'.code) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (plain) {
            out.append(c.toChar())
        } else {
            out.append('%')
            out.append("0123456789ABCDEF"[c shr 4])
            out.append("0123456789ABCDEF"[c and 0x0F])
        }
    }
    return out.toString()
}
