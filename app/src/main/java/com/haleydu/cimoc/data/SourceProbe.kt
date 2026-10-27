package com.haleydu.cimoc.data

import com.haleydu.cimoc.source.BaoZiMH
import com.haleydu.cimoc.source.CopyMH
import com.haleydu.cimoc.source.DM5
import com.haleydu.cimoc.source.Dmzj
import com.haleydu.cimoc.source.DmzjFix
import com.haleydu.cimoc.source.HotManga
import com.haleydu.cimoc.source.IKanman
import com.haleydu.cimoc.source.JMTT
import com.haleydu.cimoc.source.MH160
import com.haleydu.cimoc.source.McCmsParser
import com.haleydu.cimoc.source.Tencent
import com.haleydu.cimoc.source.YKMH
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class SourceProbe @Inject constructor(
    private val sourceConfigManager: SourceConfigManager,
    private val sourceManager: SourceManager,
    private val sourceHealthManager: SourceHealthManager,
    private val httpClient: OkHttpClient
) {

    @Volatile
    private var started = false

    fun probeAsync() {
        if (started) return
        started = true
        thread(name = "source-probe", isDaemon = true) {
            val client = httpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
            for (type in P0 + P1) {
                val url = probeUrl(type)
                if (url.isEmpty()) {
                    sourceHealthManager.recordFailure(type, SourceHealthManager.KIND_NETWORK)
                    continue
                }
                if (head(client, url)) {
                    sourceHealthManager.recordSuccess(type, 0)
                } else {
                    sourceHealthManager.recordFailure(type, SourceHealthManager.KIND_NETWORK)
                }
            }
        }
    }

    fun homepage(type: Int): String = probeUrl(type)

    private fun probeUrl(type: Int): String {
        val parser = runCatching { sourceManager.getParser(type) }.getOrNull()
        val fromParser = parser?.getUrl("") ?: ""
        if (fromParser.startsWith("http")) {
            return fromParser
        }
        val base = sourceConfigManager.getConfig(type)?.baseUrl.orEmpty()
        if (base.startsWith("http")) return base
        return sourceConfigManager.getUrl(SourceConfigManager.genericMeta(type)?.first.orEmpty(), "")
    }

    private fun head(client: OkHttpClient, url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.code in 200..499
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        private val P0 = intArrayOf(
            CopyMH.TYPE,
            IKanman.TYPE,
            Tencent.TYPE,
            BaoZiMH.TYPE,
            HotManga.TYPE,
            JMTT.TYPE,
            McCmsParser.TYPE_DUMANWUORG,
            Dmzj.TYPE,
            DmzjFix.TYPE
        )

        private val P1 = intArrayOf(
            DM5.TYPE,
            MH160.TYPE,
            YKMH.TYPE
        )
    }
}
