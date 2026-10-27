package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

private class DesktopBrowserHandle : BrowserHandle {
    override fun load(url: String) = Unit
    override fun back(): Boolean = false
    override fun forward() = Unit
    override fun reload() = Unit
    override fun canBack(): Boolean = false
    override fun canForward(): Boolean = false
    override fun currentUrl(): String = ""
    override fun title(): String = ""
    override suspend fun extract(): WebPageExtract = WebPageExtract("", "", emptyList(), null)
}

@Composable
actual fun rememberBrowserHandle(): BrowserHandle = remember { DesktopBrowserHandle() }

@Composable
actual fun BrowserWebView(
    handle: BrowserHandle,
    url: String,
    modifier: Modifier,
    onLoading: (Boolean) -> Unit,
    onPage: (url: String, title: String) -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("请在手机上使用浏览")
    }
}

@Composable
actual fun rememberWebExtractSession(): WebExtractSession = remember {
    object : WebExtractSession {
        override suspend fun extract(url: String): WebPageExtract {
            return WebPageExtract("", url, emptyList(), null)
        }
    }
}
