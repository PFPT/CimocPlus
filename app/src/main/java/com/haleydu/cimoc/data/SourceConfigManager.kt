package com.haleydu.cimoc.data

import android.content.Context
import android.util.SparseArray
import com.haleydu.cimoc.db.SourceDao
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.source.CopyMH
import com.haleydu.cimoc.source.Ohmanhua
import com.haleydu.cimoc.source.Tencent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceConfigManager @Inject constructor(
    @ApplicationContext context: Context,
    private val preferenceManager: PreferenceManager,
    private val sourceDao: SourceDao,
    private val httpClient: OkHttpClient
) {

    private var mContext: Context? = null
    private val mConfigByType = SparseArray<SourceConfig>()
    private val mCopyApiHosts = ArrayList<String>()
    private val mCopyWebHosts = ArrayList<String>()
    private var mRootJson: JSONObject? = null
    private var mAssetJson: JSONObject? = null
    private var mCopyVersion = "2025.11.21"

    init {
        init(context)
    }

    fun init(context: Context) {
        mContext = context.applicationContext
        val json = preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, "")
        if (!parse(json)) {
            parse(readAssetJson())
        }
    }

    fun fetchRemote(): Boolean {
        val urls = LinkedHashSet<String>()
        for (url in REMOTE_URLS) {
            urls.add(url)
        }
        val local = parseObject(preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, readAssetJson()))
        if (local != null) {
            addUrl(urls, local.optString("backup_update"))
            addUrl(urls, local.optString("update_backup_my"))
        }
        for (url in urls) {
            val json = download(httpClient, url)
            if (parse(json)) {
                preferenceManager.putString(PREF_SOURCE_BASE_URL_JSON, json)
                return true
            }
        }
        return false
    }

    @Synchronized
    fun applyToDatabase() {
        val dao = sourceDao
        for (i in 0 until mConfigByType.size()) {
            val config = mConfigByType.valueAt(i)
            upsert(dao, config.toSource(true))
        }
        upsert(dao, CopyMH.getDefaultSource())
        upsert(dao, Ohmanhua.getDefaultSource())
        upsert(dao, Tencent.getDefaultSource())
    }

    @Synchronized
    fun listGenericSources(): List<Source> {
        val list = ArrayList<Source>()
        for (i in 0 until mConfigByType.size()) {
            list.add(mConfigByType.valueAt(i).toSource(true))
        }
        return list
    }

    @Synchronized
    fun getConfig(type: Int): SourceConfig? = mConfigByType.get(type)

    @Synchronized
    fun requireConfig(type: Int): SourceConfig {
        getConfig(type)?.let { return it }
        val meta = genericMeta(type)
        val key = meta?.first ?: "SOURCE_$type"
        val title = meta?.second ?: "Source $type"
        val roots = listOfNotNull(mRootJson, assetRoot())
        for (root in roots) {
            val item = root.optJSONObject(key) ?: aliasOf(root, key)
            if (item != null) {
                val config = SourceConfig(key, type, title, item)
                mConfigByType.put(type, config)
                return config
            }
        }
        val json = JSONObject()
        val base = getUrl(key, "")
        if (base.isNotEmpty()) {
            json.put("baseUrl", base)
        }
        json.put("search", "/search?q=%s")
        json.put("searchInfoList", "a")
        val fallback = SourceConfig(key, type, title, json)
        mConfigByType.put(type, fallback)
        return fallback
    }

    @Synchronized
    fun getCopyApiHosts(): List<String> {
        val hosts = ArrayList<String>()
        for (host in COPY_API_PREFERRED) {
            addUnique(hosts, host)
        }
        for (host in mCopyApiHosts) {
            addUnique(hosts, host)
        }
        for (host in COPY_WEB_FALLBACKS) {
            addUnique(hosts, host)
        }
        return hosts
    }

    @Synchronized
    fun getCopyWebHosts(): List<String> {
        val hosts = ArrayList<String>()
        for (host in COPY_API_PREFERRED) {
            addUnique(hosts, host)
        }
        for (host in mCopyWebHosts) {
            addUnique(hosts, host)
        }
        for (host in COPY_WEB_FALLBACKS) {
            addUnique(hosts, host)
        }
        return hosts
    }

    @Synchronized
    fun getHotMangaHosts(): List<String> {
        val hosts = ArrayList<String>()
        addUnique(hosts, getUrl("HOTMANGA", ""))
        addUnique(hosts, getUrl("HOTMANGASERVER", ""))
        for (host in HOTMANGA_FALLBACKS) {
            addUnique(hosts, host)
        }
        return hosts
    }

    fun getCopyApiBase(): String {
        val hosts = getCopyApiHosts()
        return if (hosts.isEmpty()) "https://mapi.copy2000.site" else hosts[0]
    }

    fun getCopyWebBase(): String {
        val hosts = getCopyWebHosts()
        return if (hosts.isEmpty()) "https://www.copy3000.com" else hosts[0]
    }

    @Synchronized
    fun getCopyVersion(): String {
        return if (mCopyVersion.isNullOrEmpty()) "2025.11.21" else mCopyVersion
    }

    @Synchronized
    fun getUrl(key: String, fallback: String): String {
        return firstHttp(rawValue(key, "baseUrl"), fallback)
    }

    @Synchronized
    fun firstUrl(fallback: String, vararg keys: String): String {
        val urls = listUrls(fallback, *keys)
        return if (urls.isEmpty()) fallback else urls[0]
    }

    @Synchronized
    fun listUrls(fallback: String?, vararg keys: String?): List<String> {
        val hosts = ArrayList<String>()
        if (keys != null) {
            for (key in keys) {
                if (key == null) continue
                addUnique(hosts, firstHttp(rawValue(key, "baseUrl"), ""))
                addUnique(hosts, firstHttp(rawValue(key, "serverUrl"), ""))
                addUnique(hosts, firstHttp(rawValue(key, "apiUrl"), ""))
            }
        }
        addUnique(hosts, fallback)
        return hosts
    }

    fun hostPathRequest(path: String, fallbackHost: String, vararg keys: String): Request {
        val hosts = listUrls(fallbackHost, *keys)
        val host = if (hosts.isEmpty()) fallbackHost else hosts[0]
        val builder = Request.Builder().url(host + path)
        applyHostFallbacks(builder, path, hosts)
        return builder.build()
    }

    fun applyHostFallbacks(builder: Request.Builder?, path: String, hosts: List<String>?): Request.Builder? {
        if (builder == null || hosts == null || hosts.size < 2) {
            return builder
        }
        val fallback = StringBuilder()
        for (i in 1 until hosts.size) {
            if (fallback.isNotEmpty()) {
                fallback.append(',')
            }
            fallback.append(hosts[i]).append(path)
        }
        if (fallback.isNotEmpty()) {
            builder.header("X-Cimoc-Fallback", fallback.toString())
        }
        return builder
    }

    @Synchronized
    fun getField(key: String, field: String, fallback: String): String {
        return firstHttp(rawValue(key, field), fallback)
    }

    @Synchronized
    fun getRawField(key: String, field: String, fallback: String): String {
        val value = rawValue(key, field)
        if (value.isNullOrEmpty()) {
            return fallback
        }
        return value
    }

    fun isSourceBaseUrlJson(json: String?): Boolean {
        val `object` = parseObject(json) ?: return false
        if (`object`.has("CopyManHua") || `object`.has("BAOZIMH") || `object`.has("BaoziManHua")
            || `object`.has("COPYMH") || `object`.has("COPYMHSERVER")
        ) {
            return true
        }
        for (key in GENERIC_ITEMS.keys) {
            if (`object`.has(key)) {
                return true
            }
        }
        if (`object`.has("YiManHua") || `object`.has("BaiManGu") || `object`.has("PuFeiManHua")
            || `object`.has("BoLeFu") || `object`.has("DuManHua")
        ) {
            return true
        }
        val keys = `object`.keys()
        while (keys.hasNext()) {
            val item = `object`.optJSONObject(keys.next())
            if (item != null && item.has("baseUrl")) {
                return true
            }
        }
        return false
    }

    @Synchronized
    fun importJson(json: String): Boolean {
        if (!isSourceBaseUrlJson(json)) {
            return false
        }
        if (!parse(json)) {
            val previous = preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, "")
            if (!parse(previous)) {
                parse(readAssetJson())
            }
            return false
        }
        preferenceManager.putString(PREF_SOURCE_BASE_URL_JSON, json)
        applyToDatabase()
        return true
    }

    @Synchronized
    private fun parse(json: String?): Boolean {
        val `object` = parseObject(json) ?: return false
        mRootJson = `object`
        mConfigByType.clear()
        mCopyApiHosts.clear()
        mCopyWebHosts.clear()
        mCopyVersion = "2025.11.21"
        for (entry in GENERIC_ITEMS.entries) {
            val item = `object`.optJSONObject(entry.key)
            val meta = entry.value
            var config = if (item == null) null else SourceConfig(entry.key, meta.type, meta.title, item)
            if (config == null || !config.isComplete()) {
                val alias = aliasOf(`object`, entry.key)
                if (alias != null) {
                    val fallback = SourceConfig(entry.key, meta.type, meta.title, alias)
                    if (fallback.isComplete()) {
                        config = fallback
                    }
                }
            }
            if (config == null || !config.isComplete()) {
                val assetItem = assetRoot().optJSONObject(entry.key)
                if (assetItem != null) {
                    val fromAsset = SourceConfig(entry.key, meta.type, meta.title, assetItem)
                    if (fromAsset.isComplete()) {
                        config = fromAsset
                    }
                }
            }
            if (config != null && config.isComplete()) {
                mConfigByType.put(meta.type, config)
            }
        }
        addExtraConfigs(`object`)
        parseCopyHosts(`object`.optJSONObject("CopyManHua"))
        addHost(mCopyWebHosts, `object`.optString("COPYMH"))
        addHost(mCopyApiHosts, `object`.optString("COPYMHSERVER"))
        val hhaazz = `object`.optString("HHAAZZ")
        val sw = `object`.optString("HHAAZZ_SW")
        if (!hhaazz.isNullOrEmpty()) {
            preferenceManager.putString(PreferenceManager.PREF_HHAAZZ_BASEURL, hhaazz)
        }
        if (!sw.isNullOrEmpty()) {
            preferenceManager.putString(PreferenceManager.PREF_HHAAZZ_SW, sw)
        }
        return mConfigByType.size() > 0 || `object`.has("CopyManHua") || `object`.has("BAOZIMH")
    }

    private fun addExtraConfigs(`object`: JSONObject) {
        val keys = `object`.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (GENERIC_ITEMS.containsKey(key) || skipExtraKey(key)) {
                continue
            }
            val item = `object`.optJSONObject(key) ?: continue
            var title = item.optString("title", "").trim()
            if (title.isEmpty()) {
                title = key
            }
            var type = extraTypeOf(key)
            while (mConfigByType.get(type) != null) {
                type++
            }
            val config = SourceConfig(key, type, title, item)
            if (config.isComplete()) {
                mConfigByType.put(type, config)
            }
        }
    }

    private fun aliasOf(`object`: JSONObject, key: String): JSONObject? {
        return when (key) {
            "BAOZIMH" -> `object`.optJSONObject("BaoziManHua")
            "YIMMH" -> `object`.optJSONObject("YiManHua")
            "BMG" -> `object`.optJSONObject("BaiManGu")
            "PUFEIMH" -> `object`.optJSONObject("PuFeiManHua")
            "BOLEFU" -> `object`.optJSONObject("BoLeFu")
            "DUMH" -> `object`.optJSONObject("DuManHua")
            "HAOMAN6" -> `object`.optJSONObject("HaoManSix")
            "HAOMAN8" -> `object`.optJSONObject("HaoManEight")
            else -> null
        }
    }

    private fun parseCopyHosts(`object`: JSONObject?) {
        if (`object` == null) {
            return
        }
        addHost(mCopyWebHosts, `object`.optString("baseUrl"))
        addHost(mCopyWebHosts, `object`.optString("serverUrl"))
        addHost(mCopyApiHosts, `object`.optString("serverUrl2"))
        addHost(mCopyApiHosts, `object`.optString("serverUrl3"))
        addHost(mCopyApiHosts, `object`.optString("serverUrl"))
        addHost(mCopyWebHosts, `object`.optString("serverUrl4"))
        val version = `object`.optString("serverCodeversion", "")
        if (version != null && version.trim().isNotEmpty()) {
            mCopyVersion = version.trim()
        }
    }

    private fun rawValue(key: String?, field: String): String {
        val root = mRootJson
        if (root == null || key == null) {
            return ""
        }
        val direct = root.optString(key, "")
        if (direct != null && direct.trim().startsWith("http")) {
            return direct.trim()
        }
        val `object` = root.optJSONObject(key)
        if (`object` != null) {
            return `object`.optString(field, "").trim()
        }
        return ""
    }

    private fun addUnique(hosts: MutableList<String>, host: String?) {
        if (host.isNullOrEmpty() || !host.startsWith("http") || hosts.contains(host)) {
            return
        }
        hosts.add(host)
    }

    private fun addHost(hosts: MutableList<String>, raw: String?) {
        if (raw == null) {
            return
        }
        var host = raw.trim()
        if (host.isEmpty() || host.contains("/api/")) {
            if (host.startsWith("http") && host.contains("/api/")) {
                val idx = host.indexOf("/api/")
                host = host.substring(0, idx)
            } else if (host.isEmpty() || !host.startsWith("http")) {
                return
            }
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length - 1)
        }
        if (host.startsWith("http") && !hosts.contains(host)) {
            hosts.add(host)
        }
    }

    private fun upsert(dao: SourceDao, source: Source) {
        val existing = dao.load(source.type)
        if (existing == null) {
            dao.insert(source)
            return
        }
        if (source.title != existing.title) {
            existing.title = source.title
            dao.update(existing)
        }
    }

    private fun assetRoot(): JSONObject {
        if (mAssetJson == null) {
            mAssetJson = parseObject(readAssetJson())
            if (mAssetJson == null) {
                mAssetJson = JSONObject()
            }
        }
        return mAssetJson!!
    }

    private fun readAssetJson(): String {
        val context = mContext ?: return ""
        var reader: BufferedReader? = null
        return try {
            val input = context.assets.open("sourceBaseUrl.json")
            reader = BufferedReader(InputStreamReader(input, "UTF-8"))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line).append('\n')
            }
            builder.toString()
        } catch (_: Exception) {
            ""
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun download(client: OkHttpClient, url: String): String? {
        var response: Response? = null
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", CATALOG_UA)
                .header("Accept", "application/json,text/plain,*/*")
                .build()
            response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val body = response.body!!.string()
                if (body != null && body.trim().startsWith("{")) {
                    return body
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            response?.close()
        }
    }

    private fun parseObject(json: String?): JSONObject? {
        if (json == null || json.trim().isEmpty()) {
            return null
        }
        return try {
            JSONObject(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun addUrl(urls: MutableSet<String>, url: String?) {
        if (url != null && url.startsWith("http")) {
            urls.add(url)
        }
    }

    private class Item(val type: Int, val title: String)

    companion object {
        const val PREF_SOURCE_BASE_URL_JSON = "pref_source_base_url_json"

        private const val CATALOG_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val REMOTE_URLS = arrayOf(
            "https://gitee.com/Haleydu/update/raw/master/sourceBaseUrl.json",
            "https://raw.gitcode.com/Haleydutest/cupdate/raw/main/sourceBaseUrl.json",
            "https://miuscapp.com/cimoc/sourceBaseUrl.json",
            "http://www.cimoc.top/cimoc/sourc/master/raw/sourceBaseUrl.json",
            "https://gitcode.net/Haleydutest/cupdate/-/raw/master/sourceBaseUrl.json",
            "https://raw.githubusercontent.com/haleydu-test/cimocUpdate/main/sourceBaseUrl.json"
        )

        private val COPY_API_PREFERRED = arrayOf(
            "https://mapi.copy2000.site",
            "https://api.copy2000.online",
            "https://www.copy3000.com"
        )

        private val COPY_WEB_FALLBACKS = arrayOf(
            "https://www.2026copy.com",
            "https://www.copy3000.com",
            "https://www.mangacopy.com",
            "https://www.copy20.com",
            "https://www.2025copy.com"
        )

        private val HOTMANGA_FALLBACKS = arrayOf(
            "https://www.relamanhua.com",
            "https://www.manga2020.com",
            "https://mapi.hotmangasg.com:12001",
            "https://www.relam.cn",
            "https://www.relamanhua.xyz"
        )

        fun genericMeta(type: Int): Pair<String, String>? {
            for (entry in GENERIC_ITEMS.entries) {
                if (entry.value.type == type) {
                    return entry.key to entry.value.title
                }
            }
            return null
        }

        private val GENERIC_ITEMS: MutableMap<String, Item> = LinkedHashMap()

        init {
            GENERIC_ITEMS["BAOZIMH"] = Item(200, "包子漫画")
            GENERIC_ITEMS["YIMMH"] = Item(201, "忆漫画")
            GENERIC_ITEMS["DUMH"] = Item(202, "独漫画")
            GENERIC_ITEMS["FEIXUEMH"] = Item(203, "飞雪漫画")
            GENERIC_ITEMS["MANHUAWU"] = Item(204, "漫画屋")
            GENERIC_ITEMS["YEMANMH"] = Item(205, "野蛮漫画")
            GENERIC_ITEMS["HAOMAN8"] = Item(206, "好漫8")
            GENERIC_ITEMS["HAOMAN6"] = Item(207, "好漫6")
            GENERIC_ITEMS["MITUI"] = Item(208, "米推漫画")
            GENERIC_ITEMS["MH360"] = Item(209, "360漫画")
            GENERIC_ITEMS["BMG"] = Item(210, "百漫谷")
            GENERIC_ITEMS["BAINIANMH"] = Item(211, "百年漫画")
            GENERIC_ITEMS["QIMIAO"] = Item(212, "奇妙漫画")
            GENERIC_ITEMS["PUFEIMH"] = Item(213, "扑飞漫画")
            GENERIC_ITEMS["XINXINMH"] = Item(214, "新新漫画")
            GENERIC_ITEMS["BODONGMH"] = Item(215, "波动漫画")
            GENERIC_ITEMS["IQIYI"] = Item(216, "爱奇艺漫画")
            GENERIC_ITEMS["BOLEFU"] = Item(217, "博乐福")
            GENERIC_ITEMS["QIXIMH"] = Item(218, "七夕漫画")
            GENERIC_ITEMS["ZERO"] = Item(219, "Zero")
            GENERIC_ITEMS["DUMANWUORG"] = Item(220, "读漫屋")
            GENERIC_ITEMS["TTKANMANHUA"] = Item(221, "天天看")
            GENERIC_ITEMS["HAODUOMAN"] = Item(222, "好多漫")
            GENERIC_ITEMS["MANHUAYU"] = Item(223, "漫画鱼")
            GENERIC_ITEMS["YYMANHUA"] = Item(224, "优优漫画")
            GENERIC_ITEMS["LAIMH"] = Item(225, "来漫画")
            GENERIC_ITEMS["SHOUJIMH"] = Item(226, "手机漫画")
            GENERIC_ITEMS["VOMIC"] = Item(227, "vomic漫")
            GENERIC_ITEMS["DASHUMANHUA"] = Item(228, "大树漫画")
            GENERIC_ITEMS["MANMANMANHUA"] = Item(229, "漫漫漫画")
            GENERIC_ITEMS["YOUMANHUAWU"] = Item(230, "漫画屋")
            GENERIC_ITEMS["KUAIKANMANHUA"] = Item(231, "快看漫画")
            GENERIC_ITEMS["GUAZIMANHUA"] = Item(232, "瓜子漫画")
            GENERIC_ITEMS["JIUERMH"] = Item(233, "92漫画")
        }

        private fun skipExtraKey(key: String): Boolean {
            return "BaoziManHua" == key
                || "CopyManHua" == key
                || "YiManHua" == key
                || "BaiManGu" == key
                || "PuFeiManHua" == key
                || "BoLeFu" == key
                || "DuManHua" == key
                || "MH50" == key
                || "CoCoManHua" == key
                || "ManHuaCat" == key
        }

        private fun extraTypeOf(key: String): Int {
            val hash = key.hashCode() and 0x7fffffff
            return 300 + (hash % 9000)
        }

        private fun firstHttp(value: String?, fallback: String): String {
            if (value != null) {
                var host = value.trim()
                if (host.startsWith("http")) {
                    if (host.endsWith("/")) {
                        host = host.substring(0, host.length - 1)
                    }
                    return host
                }
            }
            return fallback
        }
    }
}
