package com.haleydu.cimoc.source

import android.util.Pair
import com.alibaba.fastjson.JSONArray
import com.google.common.collect.Lists
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_AREA
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
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.HttpUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.net.URLEncoder
import java.util.LinkedList
import java.util.regex.Pattern

class MH50(
    source: Source?,
    private val preferenceManager: PreferenceManager,
    private val sourceConfigManager: SourceConfigManager?
) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun host(): String {
        if (sourceConfigManager == null) {
            return baseUrl
        }
        return sourceConfigManager.getUrl("MH50", baseUrl)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            var url = StringUtils.format(host() + "/search/?keywords=%s&page=%d", keyword, page)
            if (host().contains("maofly")) {
                url = StringUtils.format(host() + "/search.html?q=%s", keyword)
            }
            return HttpUtils.getSimpleMobileRequest(url)
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".UpdateList > .itemBox, .comic-main-section > .row > div")) {
            override fun parse(node: Node): Comic {
                var cid = node.hrefWithSplit(".itemTxt > a", 1)
                var title = node.text(".itemTxt > a")
                var cover = node.src(".itemImg > a > img")
                if (title.isNullOrEmpty()) {
                    cid = node.hrefWithSplit("h2 > a", 1)
                    title = node.text("h2")
                    cover = node.src("img")
                }
                if (cover != null && cover.startsWith("//")) cover = "https:$cover"
                val update = node.text(".itemTxt > p.txtItme:eq(3)")
                var author = node.text(".itemTxt > p.txtItme:eq(1)")
                if (author.isNullOrEmpty()) {
                    author = node.text(".comic-author")
                }
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format(host() + "/manhua/%s/", cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("maofly.com"))
        filter.add(UrlFilter("manhuadai.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format(host() + "/manhua/%s/", cid)
        return HttpUtils.getSimpleMobileRequest(url)
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        var intro = body.text("#full-des")
        if (intro.isNullOrEmpty()) {
            intro = body.text("div.comic-info > p.comic_story, p.comic_story")
        }
        var title = body.text("#comicName")
        if (title.isNullOrEmpty()) {
            title = body.text("td.comic-titles, h1")
        }
        var cover = body.src("#Cover > img")
        if (cover.isNullOrEmpty()) {
            cover = body.src("img.img-round")
        }
        if (cover != null && cover.startsWith("//")) cover = "https:$cover"
        var author = body.text(".Introduct_Sub > .sub_r > .txtItme:eq(0)")
        if (author.isNullOrEmpty()) {
            author = body.text("td.pub-duration")
        }
        var update = body.text(".Introduct_Sub > .sub_r > .txtItme:eq(4)")
        if (update.isNullOrEmpty()) {
            update = body.text("div > table > tbody > tr:eq(10) > td")
        }
        var status = isFinish(body.text(".Introduct_Sub > .sub_r > .txtItme:eq(2) > a:eq(3)"))
        if (!status) {
            status = isFinish(body.text("div.comic-info > ul.tags"))
        }
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var nodes = body.list(".chapter-warp > ul > li > a")
        if (nodes.isEmpty()) {
            nodes = body.list("ol.links-of-books > li > a")
        }
        var i = 0
        for (node in nodes) {
            val title = node.text()
            var path = StringUtils.split(node.href(), "/", 3)
            if (path.isNullOrEmpty()) {
                path = node.href()
            }
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format(host() + "/manhua/%s/%s", cid, path)
        return HttpUtils.getSimpleMobileRequest(url)
    }

    private val server = arrayOf(
        "https://mhcdn.manhuazj.com",
        "https://manga8.mlxsc.com",
        "https://manga9.mlxsc.com",
        "https://img01.eshanyao.com",
        "https://imgdm.eshanyao.com/"
    )

    private fun decrypt(code: String?): String? {
        if (code == null) {
            return null
        }
        val key = preferenceManager.getString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, "KA58ZAQ321oobbG8") ?: "KA58ZAQ321oobbG8"
        val iv = preferenceManager.getString(PreferenceManager.PREFERENCES_MH50_IV_MSG, "A1B2C3DEF1G321o8") ?: "A1B2C3DEF1G321o8"
        return try {
            DecryptionUtils.aesDecrypt(code, key, iv)
        } catch (_: Exception) {
            null
        }
    }

    private fun getImageUrlByKey(key: String, domain: String, chapter: String): String? {
        if (key.startsWith("http://images.dmzj.com")) {
            return try {
                "https://imgdm.eshanyao.com/showImage.php?url=$key"
            } catch (_: Exception) {
                null
            }
        }
        if (Pattern.matches("\\^[a-z]//i", key)) {
            return try {
                domain + "/showImage.php?url=" + URLEncoder.encode("https://images.dmzj.com/$key", "utf-8")
            } catch (_: Exception) {
                null
            }
        }
        if (key.startsWith("http") || key.startsWith("ftp")) {
            return key
        }
        return "$domain/$chapter$key"
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val arrayStringCode = decrypt(StringUtils.match("var chapterImages =\\s*\"(.*?)\";", html, 1))
            ?: return list
        val imageList = JSONArray.parseArray(arrayStringCode)
        val chapterPath = StringUtils.match("var chapterPath = \"([\\s\\S]*?)\";", html, 1)
        val imageListSize = imageList.size
        for (i in 0 until imageListSize) {
            val key = imageList.getString(i)
            var imageUrl = getImageUrlByKey(key, server[0], chapterPath)
            if (imageUrl != null && imageUrl.contains("images.dmzj.com")) {
                imageUrl = imageUrl.replace("%", "%25")
            }
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i + 1, imageUrl ?: "", false))
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).text(".Introduct_Sub > .sub_r > .txtItme:eq(4)")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        try {
            val total = body.attr("#total-page", "value")
            if (!total.isNullOrEmpty() && page > Integer.parseInt(total)) {
                return list
            }
        } catch (_: Exception) {
        }
        val nodes = body.list("#comic-items > li")
        if (nodes.isNotEmpty()) {
            for (node in nodes) {
                val cid = node.hrefWithSplit("a.ImgA", 1)
                val title = node.text("a.txtA")
                var cover = node.src("a.ImgA img")
                if (cover != null && cover.startsWith("//")) cover = "https:$cover"
                val update = node.text(".info")
                list.add(Comic(TYPE, cid, title, cover, update, null))
            }
            return list
        }
        for (node in body.list(".comic-main-section > .row > div, .comic-main-section > div")) {
            var cid = node.hrefWithSplit("h2 > a", 1)
            if (cid.isNullOrEmpty()) {
                cid = node.hrefWithSplit("a", 1)
            }
            var title = node.text("h2")
            if (title.isNullOrEmpty()) {
                title = node.attr("img", "alt")
            }
            var cover = node.src("img")
            if (cover != null && cover.startsWith("//")) cover = "https:$cover"
            val author = node.text(".comic-author, div.comic-creators")
            if (!cid.isNullOrEmpty()) {
                list.add(Comic(TYPE, cid, title, cover, null, author))
            }
        }
        return list
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = part(args, CATEGORY_SUBJECT) + " " +
                part(args, CATEGORY_AREA) + " " +
                part(args, CATEGORY_READER) + " " +
                part(args, CATEGORY_YEAR) + " " +
                part(args, CATEGORY_PROGRESS)
            path = path.trim().replace("\\s+".toRegex(), "-")
            if (path.isEmpty()) {
                return host() + "/list/?page=%d"
            }
            return StringUtils.format(host() + "/list/%s/?page=%%d", path)
        }

        private fun part(args: Array<out String>?, index: Int): String {
            if (args == null || index >= args.size || args[index] == null) {
                return ""
            }
            return args[index]
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("热血", "rexue"))
            list.add(Pair.create("冒险", "maoxian"))
            list.add(Pair.create("玄幻", "xuanhuan"))
            list.add(Pair.create("搞笑", "gaoxiao"))
            list.add(Pair.create("恋爱", "lianai"))
            list.add(Pair.create("宠物", "chongwu"))
            list.add(Pair.create("新作", "xinzuo"))
            list.add(Pair.create("神魔", "shenmo"))
            list.add(Pair.create("竞技", "jingji"))
            list.add(Pair.create("穿越", "chuanyue"))
            list.add(Pair.create("漫改", "mangai"))
            list.add(Pair.create("霸总", "bazong"))
            list.add(Pair.create("都市", "dushi"))
            list.add(Pair.create("武侠", "wuxia"))
            list.add(Pair.create("社会", "shehui"))
            list.add(Pair.create("古风", "gufeng"))
            list.add(Pair.create("恐怖", "kongbu"))
            list.add(Pair.create("东方", "dongfang"))
            list.add(Pair.create("格斗", "gedou"))
            list.add(Pair.create("魔法", "mofa"))
            list.add(Pair.create("轻小说", "qingxiaoshuo"))
            list.add(Pair.create("魔幻", "mohuan"))
            list.add(Pair.create("生活", "shenghuo"))
            list.add(Pair.create("欢乐向", "huanlexiang"))
            list.add(Pair.create("励志", "lizhi"))
            list.add(Pair.create("音乐舞蹈", "yinyuewudao"))
            list.add(Pair.create("科幻", "kehuan"))
            list.add(Pair.create("美食", "meishi"))
            list.add(Pair.create("节操", "jiecao"))
            list.add(Pair.create("神鬼", "shengui"))
            list.add(Pair.create("爱情", "aiqing"))
            list.add(Pair.create("校园", "xiaoyuan"))
            list.add(Pair.create("治愈", "zhiyu"))
            list.add(Pair.create("奇幻", "qihuan"))
            list.add(Pair.create("仙侠", "xianxia"))
            list.add(Pair.create("运动", "yundong"))
            list.add(Pair.create("动作", "dongzuo"))
            list.add(Pair.create("日更", "rigeng"))
            list.add(Pair.create("历史", "lishi"))
            list.add(Pair.create("推理", "tuili"))
            list.add(Pair.create("悬疑", "xuanyi"))
            list.add(Pair.create("修真", "xiuzhen"))
            list.add(Pair.create("游戏", "youxi"))
            list.add(Pair.create("战争", "zhanzheng"))
            list.add(Pair.create("后宫", "hougong"))
            list.add(Pair.create("职场", "zhichang"))
            list.add(Pair.create("四格", "sige"))
            list.add(Pair.create("性转换", "xingzhuanhuan"))
            list.add(Pair.create("伪娘", "weiniang"))
            list.add(Pair.create("颜艺", "yanyi"))
            list.add(Pair.create("真人", "zhenren"))
            list.add(Pair.create("杂志", "zazhi"))
            list.add(Pair.create("侦探", "zhentan"))
            list.add(Pair.create("萌系", "mengxi"))
            list.add(Pair.create("耽美", "danmei"))
            list.add(Pair.create("百合", "baihe"))
            list.add(Pair.create("西方魔幻", "xifangmohuan"))
            list.add(Pair.create("机战", "jizhan"))
            list.add(Pair.create("宅系", "zhaixi"))
            list.add(Pair.create("忍者", "renzhe"))
            list.add(Pair.create("萝莉", "luoli"))
            list.add(Pair.create("异世界", "yishijie"))
            list.add(Pair.create("吸血", "xixie"))
            list.add(Pair.create("其他", "qita"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("日本", "riben"))
            list.add(Pair.create("大陆", "dalu"))
            list.add(Pair.create("香港", "hongkong"))
            list.add(Pair.create("台湾", "taiwan"))
            list.add(Pair.create("欧美", "oumei"))
            list.add(Pair.create("韩国", "hanguo"))
            list.add(Pair.create("其他", "qita"))
            return list
        }

        override fun hasReader(): Boolean {
            return true
        }

        override fun getReader(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("儿童漫画", "ertong"))
            list.add(Pair.create("少年漫画", "shaonian"))
            list.add(Pair.create("少女漫画", "shaonv"))
            list.add(Pair.create("青年漫画", "qingnian"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "lianzai"))
            list.add(Pair.create("完结", "wanjie"))
            return list
        }

        override fun hasYear(): Boolean {
            return true
        }

        override fun getYear(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("2000年前", "2000nianqian"))
            list.add(Pair.create("2001年", "2001nian"))
            list.add(Pair.create("2002年", "2002nian"))
            list.add(Pair.create("2003年", "2003nian"))
            list.add(Pair.create("2004年", "2004nian"))
            list.add(Pair.create("2005年", "2005nian"))
            list.add(Pair.create("2006年", "2006nian"))
            list.add(Pair.create("2007年", "2007nian"))
            list.add(Pair.create("2008年", "2008nian"))
            list.add(Pair.create("2009年", "2009nian"))
            list.add(Pair.create("2010年", "2010nian"))
            list.add(Pair.create("2011年", "2011nian"))
            list.add(Pair.create("2012年", "2012nian"))
            list.add(Pair.create("2013年", "2013nian"))
            list.add(Pair.create("2014年", "2014nian"))
            list.add(Pair.create("2015年", "2015nian"))
            list.add(Pair.create("2016年", "2016nian"))
            list.add(Pair.create("2017年", "2017nian"))
            list.add(Pair.create("2018年", "2018nian"))
            list.add(Pair.create("2019年", "2019nian"))
            list.add(Pair.create("2020年", "2020nian"))
            return list
        }
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36")
    }

    companion object {
        const val TYPE = 80
        const val DEFAULT_TITLE = "漫画堆"
        const val baseUrl = "https://www.maofly.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
