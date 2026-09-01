package com.haleydu.cimoc.source

import android.net.Uri
import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.parser.Category.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.CATEGORY_READER
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.Category.CATEGORY_YEAR
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.LinkedList
import java.util.regex.Pattern

open class GenericHtmlParser(source: Source?, config: SourceConfig) : MangaParser() {

    protected val mConfig: SourceConfig = config
    private val mType: Int = if (source == null) config.type else source.type

    init {
        init(source ?: config.toSource(true), if (config.hasCategory()) Category() else null)
        mTitle = config.title
    }

    override fun initUrlFilterList() {
        try {
            val host = Uri.parse(mConfig.baseUrl).host
            if (host != null) {
                filter.add(UrlFilter(host))
            }
        } catch (_: Exception) {
        }
    }

    @Throws(Exception::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val path = mConfig.search ?: ""
        if (!mConfig.searchKey.isNullOrEmpty()) {
            if (page != 1) {
                return null
            }
            val body = FormBody.Builder().add(mConfig.searchKey, keyword).build()
            val builder = requestBuilder(absUrl(path)) ?: return null
            return builder.post(body).build()
        }
        val url = if (path.contains("%s") && path.contains("%d")) {
            mConfig.baseUrl + StringUtils.format(path, encoded, page)
        } else if (path.contains("%s")) {
            if (page != 1) {
                return null
            }
            mConfig.baseUrl + StringUtils.format(path, encoded)
        } else {
            if (page != 1) {
                return null
            }
            mConfig.baseUrl + path + encoded
        }
        return buildGet(url)
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        val type = mType
        val config = mConfig
        val nodes = try {
            body.list(config.searchInfoList)
        } catch (_: Exception) {
            LinkedList()
        }
        return object : NodeIterator(nodes) {
            override fun parse(node: Node): Comic? {
                val cid = extractPath(attrOrHref(node, config.searchInfoCid))
                if (cid.isNullOrEmpty()) {
                    return null
                }
                val title = textOrAttr(node, config.searchInfoTitle)
                val cover = coverOf(node, config.searchInfoCover, null)
                val author = textOrAttr(node, config.searchInfoAuthor)
                val update = textOrAttr(node, config.searchInfoUpdate)
                return Comic(type, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return absUrl(cid)
    }

    override fun getInfoRequest(cid: String): Request? {
        return buildGet(absUrl(cid))
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = firstText(body, mConfig.parseInfoTitle)
        val cover = coverOf(body, mConfig.parseInfoCover, html)
        val intro = firstText(body, mConfig.parseInfoIntro)
        val author = firstText(body, mConfig.parseInfoAuthor)
        val update = firstText(body, mConfig.parseInfoUpdate)
        val finish = isFinish(firstText(body, mConfig.parseInfoStatus))
        comic.setInfo(
            emptyTo(title, comic.title),
            emptyTo(cover, comic.cover),
            update, intro, author, finish
        )
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var index = 0
        index = addChapters(list, body, mConfig.parseChapterList1, sourceComic, index)
        index = addChapters(list, body, mConfig.parseChapterList2, sourceComic, index)
        addChapters(list, body, mConfig.parseChapterList3, sourceComic, index)
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request? {
        return buildGet(absUrl(path))
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        if (isRegex(mConfig.parseImageList)) {
            parseRegexImages(html, chapter, list)
        } else {
            parseCssImages(html, chapter, list)
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request? {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return firstText(Node(html), mConfig.parseInfoUpdate)
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "Referer", mConfig.baseUrl + "/",
            "User-Agent", USER_AGENT
        )
    }

    protected fun requestBuilder(url: String?): Request.Builder? {
        if (url.isNullOrEmpty()) {
            return null
        }
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        val builder = Request.Builder().url(httpUrl)
        val headers = getHeader()
        for (i in 0 until headers.size) {
            builder.header(headers.name(i), headers.value(i))
        }
        return builder
    }

    protected fun buildGet(url: String?): Request? {
        val builder = requestBuilder(url)
        return builder?.build()
    }

    override fun getCategoryRequest(format: String, page: Int): Request {
        val url = StringUtils.format(format, page)
        return buildGet(url) ?: super.getCategoryRequest(format, page)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        if (html == null) {
            return list
        }
        val trim = html.trim()
        if (trim.startsWith("{") || trim.startsWith("[")) {
            return parseJsonCategory(trim)
        }
        val body = Node(html)
        var selector = mConfig.parseCategoryInfoList
        if (selector.isNullOrEmpty() || isJsonKey(selector)) {
            selector = mConfig.searchInfoList
        }
        val nodes = try {
            body.list(selector)
        } catch (_: Exception) {
            return list
        }
        for (node in nodes) {
            val comic = parseCategoryNode(node)
            if (comic != null) {
                list.add(comic)
            }
        }
        return list
    }

    private fun addChapters(
        list: MutableList<Chapter>,
        body: Node,
        selector: String?,
        sourceComic: Long,
        startIndex: Int
    ): Int {
        if (selector.isNullOrEmpty()) {
            return startIndex
        }
        val nodes = try {
            body.list(selector)
        } catch (_: Exception) {
            return startIndex
        }
        var index = startIndex
        for (node in nodes) {
            var title = if (mConfig.parseChapterTitle.isEmpty()) {
                node.text()
            } else {
                textOrAttr(node, mConfig.parseChapterTitle)
            }
            val href = if (mConfig.parseChapterPath.isEmpty()) {
                firstHref(node)
            } else {
                attrOrHref(node, mConfig.parseChapterPath)
            }
            val path = extractPath(href)
            if (path.isNullOrEmpty()) {
                continue
            }
            if (title.isNullOrEmpty()) {
                title = path
            }
            try {
                list.add(Chapter((sourceComic.toString() + "000" + index).toLong(), sourceComic, title, path))
                index++
            } catch (_: Exception) {
            }
        }
        return index
    }

    private fun parseCssImages(html: String, chapter: Chapter?, list: MutableList<ImageUrl>) {
        var selector = mConfig.parseImageList
        if (selector.isNullOrEmpty()) {
            selector = mConfig.parseImageUrl
        }
        if (selector.isNullOrEmpty()) {
            return
        }
        var i = 0
        val nodes = try {
            Node(html).list(selector)
        } catch (_: Exception) {
            return
        }
        val comicChapter = chapter?.id ?: return
        for (node in nodes) {
            var url = imageAttr(node, mConfig.parseImageUrl)
            url = absImage(url)
            if (url == null || isFault(url)) {
                continue
            }
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, ++i, url, false))
        }
    }

    private fun parseRegexImages(html: String, chapter: Chapter?, list: MutableList<ImageUrl>) {
        val comicChapter = chapter?.id ?: return
        val matcher = Pattern.compile(mConfig.parseImageList).matcher(html)
        var i = 0
        while (matcher.find()) {
            var captured = if (matcher.groupCount() >= 1) matcher.group(1) else matcher.group()
            if (captured == null) {
                continue
            }
            captured = captured.trim()
            if (captured.startsWith("[") || captured.startsWith("{")) {
                try {
                    val array = if (captured.startsWith("[")) JSONArray(captured) else JSONArray("[$captured]")
                    for (n in 0 until array.length()) {
                        val url = absImage(array.optString(n))
                        if (url == null || isFault(url)) {
                            continue
                        }
                        val id = (comicChapter.toString() + "000" + i).toLong()
                        list.add(ImageUrl(id, comicChapter, ++i, url, false))
                    }
                    return
                } catch (_: Exception) {
                }
            }
            val parts = captured.split(",|;".toRegex()).toTypedArray()
            for (part in parts) {
                val url = absImage(part.replace("\"", "").replace("'", "").trim())
                if (url == null || isFault(url)) {
                    continue
                }
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, ++i, url, false))
            }
        }
    }

    private fun isRegex(value: String?): Boolean {
        return value != null && (value.contains("(.*") || value.contains("(.+") || value.contains("=\\") || value.contains("='"))
    }

    protected fun absUrl(path: String?): String {
        var p = path
        if (p.isNullOrEmpty()) {
            return mConfig.baseUrl
        }
        if (p.startsWith("http://") || p.startsWith("https://")) {
            return p
        }
        if (p.startsWith("//")) {
            return "https:$p"
        }
        if (!p.startsWith("/")) {
            p = "/$p"
        }
        return mConfig.baseUrl + p
    }

    protected fun absImage(url: String?): String? {
        var u = url ?: return null
        u = u.trim()
        if (u.isEmpty()) {
            return null
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u
        }
        if (u.startsWith("//")) {
            return "https:$u"
        }
        if (u.startsWith("/")) {
            return mConfig.baseUrl + u
        }
        if (!mConfig.imageServerUrl.isNullOrEmpty()) {
            val server = mConfig.imageServerUrl
            if (!server.endsWith("/") && !u.startsWith("/")) {
                return "$server/$u"
            }
            return server + u
        }
        return mConfig.baseUrl + "/" + u
    }

    protected fun extractPath(href: String?): String? {
        var h = href ?: return null
        h = h.trim()
        if (h.isEmpty() || h.startsWith("javascript:")) {
            return null
        }
        if (h.startsWith(mConfig.baseUrl)) {
            h = h.substring(mConfig.baseUrl.length)
        } else if (h.startsWith("http://") || h.startsWith("https://") || h.startsWith("//")) {
            try {
                val uri = Uri.parse(if (h.startsWith("//")) "https:$h" else h)
                val path = uri.encodedPath
                val query = uri.encodedQuery
                h = path ?: ""
                if (!query.isNullOrEmpty()) {
                    h = "$h?$query"
                }
            } catch (_: Exception) {
            }
        }
        val hash = h.indexOf('#')
        if (hash >= 0) {
            h = h.substring(0, hash)
        }
        if (h.isEmpty()) {
            return null
        }
        return h
    }

    private fun attrOrHref(node: Node, selector: String?): String? {
        if (selector.isNullOrEmpty()) {
            return firstHref(node)
        }
        var href = node.href(selector)
        if (href.isNullOrEmpty()) {
            href = node.attr(selector, "href")
        }
        if (href.isNullOrEmpty()) {
            href = node.attr("href")
        }
        return href
    }

    private fun firstHref(node: Node): String? {
        var href = node.href("a")
        if (href.isNullOrEmpty()) {
            href = node.href()
        }
        return href
    }

    private fun textOrAttr(node: Node, selector: String?): String? {
        if (selector.isNullOrEmpty()) {
            return node.text()
        }
        var text = node.text(selector)
        if (text.isNullOrEmpty()) {
            text = node.attr(selector, "title")
        }
        return text
    }

    private fun firstText(node: Node, selector: String?): String {
        if (selector.isNullOrEmpty()) {
            return ""
        }
        val text = node.text(selector)
        return text ?: ""
    }

    private fun coverOf(node: Node, selector: String?, html: String?): String? {
        if (isRegex(selector)) {
            val found = regexFirst(html ?: outerHtml(node), selector)
            if (!found.isNullOrEmpty()) {
                return absImage(found)
            }
        }
        if (selector.isNullOrEmpty()) {
            return absImage(imageAttr(node, ""))
        }
        var child = node
        try {
            if (node.get() != null && node.get().select(selector).first() != null) {
                child = Node(node.get().select(selector).first())
            }
        } catch (_: Exception) {
        }
        return absImage(imageAttr(child, ""))
    }

    private fun regexFirst(html: String?, pattern: String?): String? {
        if (html == null || pattern.isNullOrEmpty()) {
            return null
        }
        try {
            val matcher = Pattern.compile(pattern).matcher(html)
            if (matcher.find()) {
                return if (matcher.groupCount() >= 1) matcher.group(1) else matcher.group()
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun outerHtml(node: Node): String {
        return try {
            if (node.get() == null) "" else node.get().outerHtml()
        } catch (_: Exception) {
            ""
        }
    }

    private fun imageAttr(node: Node, attrHint: String?): String? {
        val attrs = if (attrHint != null && attrHint.isNotEmpty() && !attrHint.contains(" ") && !attrHint.contains(">") && !attrHint.contains(".")) {
            arrayOf(attrHint, "data-original", "data-src", "data-echo", "srcset", "src")
        } else {
            arrayOf("data-original", "data-src", "data-echo", "srcset", "src")
        }
        for (attr in attrs) {
            var value = node.attr(attr)
            if (value != null && value.isNotEmpty() && !value.startsWith("data:")) {
                if (attr == "srcset") {
                    val space = value.indexOf(' ')
                    if (space > 0) {
                        value = value.substring(0, space)
                    }
                    val comma = value.indexOf(',')
                    if (comma > 0) {
                        value = value.substring(0, comma)
                    }
                }
                return value
            }
        }
        return node.src()
    }

    private fun isFault(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return true
        }
        if (!mConfig.parseImageFaultImage.isNullOrEmpty() && url.contains(mConfig.parseImageFaultImage)) {
            return true
        }
        val lower = url.lowercase()
        return lower.contains("icon_loading") || lower.contains("placeholder")
    }

    private fun emptyTo(value: String?, fallback: String?): String? {
        return if (value.isNullOrEmpty()) fallback else value
    }

    private fun parseCategoryNode(node: Node): Comic? {
        var cid = extractPath(attrOrHref(node, mConfig.parseCategoryInfoCid))
        if (cid.isNullOrEmpty() && mConfig.parseCategoryInfoCidPre.isNotEmpty()) {
            val text = textOrAttr(node, mConfig.parseCategoryInfoCid)
            if (!text.isNullOrEmpty()) {
                cid = mConfig.parseCategoryInfoCidPre + text
            }
        }
        if (cid.isNullOrEmpty()) {
            return null
        }
        if (mConfig.parseCategoryInfoCidPre.isNotEmpty() && !cid.startsWith("http") && !cid.startsWith(mConfig.parseCategoryInfoCidPre)) {
            cid = mConfig.parseCategoryInfoCidPre + cid
        }
        val title = textOrAttr(node, mConfig.parseCategoryInfoTitle)
        val cover = coverOf(node, mConfig.parseCategoryInfoCover, null)
        val author = textOrAttr(node, mConfig.parseCategoryInfoAuthor)
        val update = textOrAttr(node, mConfig.parseCategoryInfoUpdate)
        return Comic(mType, cid, title, cover, update, author)
    }

    private fun parseJsonCategory(html: String): List<Comic> {
        val list = LinkedList<Comic>()
        try {
            val root = if (html.startsWith("[")) JSONObject().put("list", JSONArray(html)) else JSONObject(html)
            val key = if (isJsonKey(mConfig.parseCategoryInfoList)) mConfig.parseCategoryInfoList else "list"
            var array = findJsonArray(root, key)
            if (array == null) {
                array = findJsonArray(root, "books")
            }
            if (array == null) {
                array = findJsonArray(root, "list")
            }
            if (array == null) {
                return list
            }
            val cidKey = if (isJsonKey(mConfig.parseCategoryInfoCid)) mConfig.parseCategoryInfoCid else "id"
            val titleKey = if (isJsonKey(mConfig.parseCategoryInfoTitle)) mConfig.parseCategoryInfoTitle else "name"
            val coverKey = if (isJsonKey(mConfig.parseCategoryInfoCover)) mConfig.parseCategoryInfoCover else "cover"
            val authorKey = if (isJsonKey(mConfig.parseCategoryInfoAuthor)) mConfig.parseCategoryInfoAuthor else "author"
            val updateKey = if (isJsonKey(mConfig.parseCategoryInfoUpdate)) mConfig.parseCategoryInfoUpdate else "update"
            val pre = mConfig.parseCategoryInfoCidPre
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                var cid = obj.optString(cidKey)
                if (cid.isNullOrEmpty()) {
                    continue
                }
                if (pre.isNotEmpty() && !cid.startsWith("http") && !cid.startsWith(pre)) {
                    cid = pre + cid
                }
                val title = obj.optString(titleKey)
                val cover = absImage(obj.optString(coverKey))
                val author = obj.optString(authorKey)
                val update = obj.optString(updateKey)
                list.add(Comic(mType, cid, title, cover, update, author))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun findJsonArray(root: JSONObject?, key: String?): JSONArray? {
        if (root == null || key.isNullOrEmpty()) {
            return null
        }
        val direct = root.optJSONArray(key)
        if (direct != null) {
            return direct
        }
        val data = root.optJSONObject("data")
        if (data != null) {
            val nested = data.optJSONArray(key)
            if (nested != null) {
                return nested
            }
        }
        val results = root.optJSONObject("results")
        if (results != null) {
            return results.optJSONArray(key)
        }
        return null
    }

    private fun isJsonKey(value: String?): Boolean {
        return value != null && value.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = mConfig.parseCategoryPath
            path = path.replace("page=%s", "page=%d")
            path = path.replace("page/%s", "page/%d")
            path = fillPercentS(path, *args)
            if (!path.contains("%d")) {
                path = path + if (path.contains("?")) "&page=%d" else "?page=%d"
            }
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return path
            }
            if (!path.startsWith("/")) {
                path = "/$path"
            }
            return mConfig.baseUrl + path
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "all"))
            return list
        }

        override fun hasArea(): Boolean {
            return countPercentS(mConfig.parseCategoryPath) >= 2
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "all"))
            return list
        }

        override fun hasProgress(): Boolean {
            return countPercentS(mConfig.parseCategoryPath) >= 3
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "all"))
            return list
        }

        override fun hasOrder(): Boolean {
            return countPercentS(mConfig.parseCategoryPath) >= 4
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "all"))
            return list
        }

        private fun countPercentS(path: String?): Int {
            if (path == null) {
                return 0
            }
            val normalized = path.replace("page=%s", "page=%d").replace("page/%s", "page/%d")
            var count = 0
            var i = 0
            while (i < normalized.length - 1) {
                if (normalized[i] == '%' && normalized[i + 1] == 's') {
                    count++
                }
                i++
            }
            return count
        }

        private fun fillPercentS(path: String, vararg args: String): String {
            val ordered = arrayOf(
                argAt(args, CATEGORY_SUBJECT),
                argAt(args, CATEGORY_AREA),
                argAt(args, CATEGORY_READER),
                argAt(args, CATEGORY_YEAR),
                argAt(args, CATEGORY_PROGRESS),
                argAt(args, CATEGORY_ORDER)
            )
            val builder = StringBuilder()
            var argIndex = 0
            var i = 0
            while (i < path.length) {
                if (i + 1 < path.length && path[i] == '%' && path[i + 1] == 's') {
                    var value = "all"
                    while (argIndex < ordered.size) {
                        val candidate = ordered[argIndex++]
                        if (!candidate.isNullOrEmpty()) {
                            value = candidate
                            break
                        }
                    }
                    builder.append(value)
                    i += 2
                } else {
                    builder.append(path[i])
                    i++
                }
            }
            return builder.toString()
        }

        private fun argAt(args: Array<out String>?, index: Int): String? {
            if (args == null || index >= args.size) {
                return null
            }
            return args[index]
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
