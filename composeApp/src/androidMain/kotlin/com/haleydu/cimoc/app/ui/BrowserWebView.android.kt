package com.haleydu.cimoc.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

private const val SCROLL_JS = """
(function() {
  var h = Math.max(
    document.body ? document.body.scrollHeight : 0,
    document.documentElement ? document.documentElement.scrollHeight : 0
  );
  var y = window.scrollY || window.pageYOffset || 0;
  var step = window.innerHeight || 800;
  var nextY = Math.min(y + step, h);
  window.scrollTo(0, nextY);
  return {y: nextY, h: h, done: h === 0 || nextY >= h - 8};
})();
"""

private const val EXTRACT_JS = """
(function() {
  function abs(u) {
    if (!u) return null;
    try { return new URL(u, location.href).href; } catch (e) { return null; }
  }
  function pick(img) {
    return img.currentSrc || img.getAttribute('data-src') || img.getAttribute('data-original') || img.getAttribute('data-lazy-src') || img.getAttribute('data-echo') || img.src || '';
  }
  function looksLikeImage(url) {
    return /\.(jpg|jpeg|png|webp|gif|bmp)(\?|#|$)/i.test(url);
  }
  var seen = {};
  var urls = [];
  var nodes = document.querySelectorAll('img');
  for (var i = 0; i < nodes.length; i++) {
    var img = nodes[i];
    var href = abs(pick(img));
    if (!href || href.indexOf('data:') === 0 || seen[href]) continue;
    var w = img.naturalWidth || img.width || 0;
    var h = img.naturalHeight || img.height || 0;
    if ((w > 0 && w < 200) || (h > 0 && h < 200)) continue;
    if (w === 0 && h === 0 && !looksLikeImage(href)) continue;
    seen[href] = 1;
    urls.push(href);
  }
  var next = null;
  var rel = document.querySelector('a[rel="next"]');
  if (rel && rel.href) next = abs(rel.href);
  if (!next) {
    var links = document.querySelectorAll('a');
    var re = /下一章|下一话|下一頁|下一页|下一集|下章/;
    for (var j = 0; j < links.length; j++) {
      var text = (links[j].innerText || '') + ' ' + (links[j].title || '');
      if (re.test(text)) {
        next = abs(links[j].href);
        if (next) break;
      }
    }
  }
  var page = location.href;
  if (next === page) next = null;
  return {title: document.title || '', pageUrl: page, urls: urls, next: next};
})();
"""

private class AndroidBrowserHandle : BrowserHandle {
    private var webView: WebView? = null
    private var pending: String? = null

    fun attach(view: WebView) {
        webView = view
        pending?.let {
            pending = null
            view.loadUrl(it)
        }
    }

    fun detach(view: WebView) {
        if (webView === view) webView = null
    }

    override fun load(url: String) {
        val view = webView
        if (view == null) pending = url else view.loadUrl(url)
    }

    override fun back(): Boolean {
        val view = webView ?: return false
        if (!view.canGoBack()) return false
        view.goBack()
        return true
    }

    override fun forward() {
        val view = webView ?: return
        if (view.canGoForward()) view.goForward()
    }

    override fun reload() {
        webView?.reload()
    }

    override fun canBack(): Boolean = webView?.canGoBack() == true

    override fun canForward(): Boolean = webView?.canGoForward() == true

    override fun currentUrl(): String = webView?.url ?: pending.orEmpty()

    override fun title(): String = webView?.title.orEmpty()

    override suspend fun extract(): WebPageExtract {
        val view = webView ?: return WebPageExtract("", "", emptyList(), null)
        return view.collectPage()
    }
}

private class AndroidWebExtractSession : WebExtractSession {
    var webView: WebView? = null

    override suspend fun extract(url: String): WebPageExtract {
        val view = webView ?: return WebPageExtract("", url, emptyList(), null)
        val done = CompletableDeferred<Unit>()
        var started = false
        view.webViewClient = client(
            onStarted = { started = true },
            onFinished = {
                if (started && !done.isCompleted) done.complete(Unit)
            }
        )
        view.loadUrl(url)
        withTimeoutOrNull(20_000) { done.await() }
        delay(350)
        return view.collectPage()
    }
}

@Composable
actual fun rememberBrowserHandle(): BrowserHandle = remember { AndroidBrowserHandle() }

@Composable
actual fun BrowserWebView(
    handle: BrowserHandle,
    url: String,
    modifier: Modifier,
    onLoading: (Boolean) -> Unit,
    onPage: (url: String, title: String) -> Unit
) {
    val bridge = handle as AndroidBrowserHandle
    LaunchedEffect(url) {
        if (url.isNotEmpty()) bridge.load(url)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).also { web ->
                web.applyBrowserSettings()
                web.webViewClient = client(
                    onStarted = { page ->
                        onLoading(true)
                        onPage(page, web.title.orEmpty())
                    },
                    onFinished = { page ->
                        onLoading(false)
                        onPage(page, web.title.orEmpty())
                    }
                )
                bridge.attach(web)
            }
        },
        onRelease = { web ->
            bridge.detach(web)
            web.destroy()
        }
    )
}

@Composable
actual fun rememberWebExtractSession(): WebExtractSession {
    val session = remember { AndroidWebExtractSession() }
    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0f),
        factory = { context ->
            WebView(context).also { web ->
                web.applyBrowserSettings()
                session.webView = web
            }
        },
        onRelease = { web ->
            session.webView = null
            web.destroy()
        }
    )
    return session
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.applyBrowserSettings() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    settings.userAgentString = BrowserUserAgent
    settings.mediaPlaybackRequiresUserGesture = true
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
}

private fun client(
    onStarted: (String) -> Unit,
    onFinished: (String) -> Unit
): WebViewClient {
    return object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme
            return scheme != "http" && scheme != "https"
        }

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return !url.startsWith("http://") && !url.startsWith("https://")
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (!url.isNullOrEmpty()) onStarted(url)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!url.isNullOrEmpty()) onFinished(url)
        }
    }
}

private suspend fun WebView.eval(script: String): String? = suspendCancellableCoroutine { cont ->
    evaluateJavascript(script) { value ->
        if (cont.isActive) cont.resume(value)
    }
}

private suspend fun WebView.collectPage(): WebPageExtract {
    var guard = 0
    while (guard < 40) {
        val obj = parseJsObject(eval(SCROLL_JS)) ?: break
        if (obj.optBoolean("done")) break
        delay(180)
        guard++
    }
    delay(300)
    val obj = parseJsObject(eval(EXTRACT_JS))
    eval("window.scrollTo(0,0);")
    if (obj == null) return WebPageExtract("", url.orEmpty(), emptyList(), null)
    val urls = buildList {
        val array = obj.optJSONArray("urls") ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optString(index)
            if (item.isNotEmpty()) add(item)
        }
    }
    val next = obj.optString("next").takeIf { it.isNotEmpty() && it != "null" }
    return WebPageExtract(
        title = obj.optString("title"),
        pageUrl = obj.optString("pageUrl").ifEmpty { url.orEmpty() },
        imageUrls = urls,
        nextUrl = next
    )
}

private fun parseJsObject(raw: String?): JSONObject? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return runCatching { JSONObject(raw) }.getOrNull()
}
