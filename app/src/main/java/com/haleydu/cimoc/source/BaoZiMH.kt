package com.haleydu.cimoc.source

import android.util.Log
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import okhttp3.Headers
import org.json.JSONArray
import java.util.LinkedHashSet
import java.util.LinkedList

class BaoZiMH(
    source: Source?,
    config: SourceConfig,
    private val sourceConfigManager: SourceConfigManager
) : GenericHtmlParser(source, config) {

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        var cards: List<Node>?
        try {
            cards = Node(html).list("div.comics-card")
        } catch (_: Exception) {
            cards = null
        }
        if (cards.isNullOrEmpty()) {
            return super.getSearchIterator(html, page)
        }
        return object : NodeIterator(cards) {
            override fun parse(node: Node): Comic? {
                val cid = comicPath(node)
                if (cid.isNullOrEmpty()) {
                    return null
                }
                var title = node.attr("a.comics-card__poster", "title")
                if (title.isNullOrEmpty()) {
                    title = node.text("a.comics-card__title, .comics-card__title, small")
                }
                val cover = cardCover(node)
                val author = node.text("small")
                return Comic(TYPE, cid, title, cover, null, author)
            }
        }
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val parsed = super.parseInfo(html, comic)
        val body = Node(html)
        var title = parsed.title
        if (title.isNullOrEmpty()) {
            title = body.text("h1.comics-detail__title, .comics-detail__title")
        }
        var cover = parsed.cover
        if (cover.isNullOrEmpty() || (!cover.startsWith("http://") && !cover.startsWith("https://"))) {
            val ampCover = ampCover(body)
            if (!ampCover.isNullOrEmpty()) {
                cover = ampCover
            }
        } else {
            cover = absImage(cover)
        }
        var intro = parsed.intro
        if (intro.isNullOrEmpty()) {
            intro = body.text("p.comics-detail__desc, div.comics-detail__info > p")
        }
        var author = parsed.author
        if (author.isNullOrEmpty()) {
            author = body.text("h2.comics-detail__author, .comics-detail__author")
        }
        val finish = parsed.finish == true
        parsed.setInfo(title, cover, parsed.update, intro, author, finish)
        return parsed
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        var list: MutableList<Chapter>?
        try {
            list = super.parseChapter(html, comic, sourceComic).toMutableList()
        } catch (_: Exception) {
            list = null
        }
        if (!list.isNullOrEmpty()) {
            return list
        }
        list = LinkedList()
        val nodes = try {
            Node(html).list(
                "div.comics-chapters a, div#chapter-items a, div#chapters_other_list a, a.comics-chapters"
            )
        } catch (_: Exception) {
            return list
        }
        val seen = LinkedHashSet<String>()
        var index = 0
        for (node in nodes) {
            var href = node.href()
            if (href.isNullOrEmpty()) {
                href = node.href("a")
            }
            val path = extractPath(href)
            if (path.isNullOrEmpty() || !seen.add(path)) {
                continue
            }
            var title = node.text("span")
            if (title.isNullOrEmpty()) {
                title = node.text()
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
        return list
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        try {
            val parsed = super.parseImages(html, chapter)
            if (parsed != null) {
                list.addAll(parsed)
            }
        } catch (e: Exception) {
            Log.e("BaoZiMH", "parseImages", e)
        }
        if (list.isEmpty()) {
            parseAmpImages(html, chapter, list)
        }
        for (image in list) {
            rewriteImage(image)
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "Referer", REFERER,
            "User-Agent", USER_AGENT,
            "Accept", ACCEPT
        )
    }

    override fun getHeader(url: String): Headers {
        return getHeader()
    }

    override fun getHeader(list: List<ImageUrl>): Headers {
        return getHeader()
    }

    private fun comicPath(node: Node): String? {
        var path = extractPath(node.href("a.comics-card__poster"))
        if (isComicPath(path)) {
            return path
        }
        try {
            val links = node.list("a")
            for (link in links) {
                path = extractPath(link.href())
                if (isComicPath(path)) {
                    return path
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun isComicPath(path: String?): Boolean {
        if (path.isNullOrEmpty()) {
            return false
        }
        val lower = path.lowercase()
        return lower.contains("/comic/") || lower.contains("/manga/")
    }

    private fun cardCover(node: Node): String? {
        try {
            val imgs = node.list("amp-img, img")
            for (img in imgs) {
                val url = absImage(firstImageUrl(img))
                if (!url.isNullOrEmpty()) {
                    return url
                }
            }
        } catch (_: Exception) {
        }
        return absImage(firstImageUrl(node))
    }

    private fun ampCover(body: Node): String? {
        try {
            val imgs = body.list(
                "div.comics-detail amp-img, div.l-content amp-img, .comics-detail__poster amp-img, amp-img"
            )
            for (img in imgs) {
                val url = absImage(firstImageUrl(img))
                if (!url.isNullOrEmpty()) {
                    return url
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun parseAmpImages(html: String, chapter: Chapter?, list: MutableList<ImageUrl>) {
        val seen = LinkedHashSet<String>()
        val nodes = try {
            Node(html).list(
                ".comic-contain amp-img, ul.comic-contain li amp-img, .comic-contain img, amp-img.comic-contain__item"
            )
        } catch (_: Exception) {
            return
        }
        val comicChapter = chapter?.id ?: return
        var i = 0
        for (node in nodes) {
            val url = absImage(firstImageUrl(node))
            if (url.isNullOrEmpty() || !seen.add(url)) {
                continue
            }
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, ++i, url, false))
        }
    }

    private fun rewriteImage(image: ImageUrl) {
        try {
            val urls = image.urls
            if (urls == null || urls.isEmpty()) {
                return
            }
            val all = LinkedHashSet<String>()
            for (url in urls) {
                for (variant in fallbackUrls(url)) {
                    if (!variant.isNullOrEmpty()) {
                        all.add(variant)
                    }
                }
            }
            if (all.isNotEmpty()) {
                image.urls = all.toTypedArray()
            }
        } catch (_: Exception) {
        }
    }

    private fun fallbackUrls(url: String?): Array<String> {
        val abs = absImage(url)
        if (abs.isNullOrEmpty()) {
            return arrayOf()
        }
        val variants = LinkedHashSet<String>()
        val rewritten = rewriteCdn(abs)
        if (!rewritten.isNullOrEmpty()) {
            variants.add(rewritten)
        }
        variants.add(abs)
        val def = defaultCdn()
        if (abs.contains(def)) {
            for (suffix in cdnSuffixes()) {
                val index = abs.indexOf(def)
                if (index < 0) {
                    continue
                }
                variants.add(abs.substring(0, index) + suffix + abs.substring(index + def.length))
            }
        }
        return variants.toTypedArray()
    }

    private fun cdnSuffixes(): List<String> {
        val suffixes = ArrayList<String>()
        val raw = sourceConfigManager.getRawField("BAOZIMH", "REQ_DOMAINS", "")
        try {
            val array = JSONArray(raw.replace('\'', '"'))
            for (i in 0 until array.length()) {
                val item = array.optJSONArray(i)
                if (item == null || item.length() == 0) {
                    continue
                }
                val suffix = item.optString(0)
                if (!suffix.isNullOrEmpty()) {
                    suffixes.add(suffix)
                }
            }
        } catch (_: Exception) {
        }
        val def = defaultCdn()
        if (!suffixes.contains(def)) {
            suffixes.add(def)
        }
        return suffixes
    }

    private fun defaultCdn(): String {
        val def = sourceConfigManager.getRawField("BAOZIMH", "DEFAULT_TOP_DOMIAN", DEFAULT_CDN)
        if (def.isNullOrEmpty()) {
            return DEFAULT_CDN
        }
        return def
    }

    private fun firstImageUrl(node: Node): String? {
        val attrs = arrayOf("srcset", "data-src", "data-original", "data-echo", "src")
        for (attr in attrs) {
            var value = node.attr(attr)
            if (value.isNullOrEmpty() || value.startsWith("data:")) {
                continue
            }
            value = value.trim()
            if (attr == "srcset") {
                value = firstSrcsetUrl(value)
                if (value.isNullOrEmpty()) {
                    continue
                }
            }
            return value
        }
        return null
    }

    private fun firstSrcsetUrl(srcset: String): String? {
        val candidates = srcset.split(",")
        if (candidates.isEmpty()) {
            return null
        }
        var first = candidates[0].trim()
        val space = first.indexOf(' ')
        if (space > 0) {
            first = first.substring(0, space)
        }
        return first
    }

    private fun rewriteCdn(url: String?): String? {
        if (url.isNullOrEmpty()) {
            return url
        }
        return try {
            val def = defaultCdn()
            val current = sourceConfigManager.getRawField("BAOZIMH", "CURRENT_TOP_DOMIAN", DEFAULT_CDN)
            if (def.isNullOrEmpty() || current.isNullOrEmpty() || def == current) {
                return url
            }
            val index = url.indexOf(def)
            if (index < 0) {
                return url
            }
            url.substring(0, index) + current + url.substring(index + def.length)
        } catch (_: Exception) {
            url
        }
    }

    companion object {
        const val TYPE = 200
        const val DEFAULT_TITLE = "包子漫画"
        private const val REFERER = "https://www.baozimh.com/"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val ACCEPT = "image/webp,image/apng,image/*,*/*;q=0.8"
        private const val DEFAULT_CDN = ".baozicdn.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
