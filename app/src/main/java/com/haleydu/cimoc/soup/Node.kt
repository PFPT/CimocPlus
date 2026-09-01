package com.haleydu.cimoc.soup

import com.haleydu.cimoc.utils.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.LinkedList

class Node {
    private val element: Element

    constructor(html: String) {
        this.element = Jsoup.parse(html).body()
    }

    constructor(element: Element?) {
        this.element = element!!
    }

    fun id(id: String): Node {
        return Node(element.getElementById(id))
    }

    fun getParent(cssQuery: String): Node {
        return Node(get().select(cssQuery).first().parent())
    }

    fun getChild(cssQuery: String): Node {
        return Node(get().select(cssQuery).first())
    }

    fun getLastChild(cssQuery: String): Node {
        return Node(get().select(cssQuery).last())
    }

    fun list(cssQuery: String): List<Node> {
        val list = LinkedList<Node>()
        val elements: Elements = element.select(cssQuery)
        for (e in elements) {
            list.add(Node(e))
        }
        return list
    }

    fun get(): Element {
        return element
    }

    fun text(): String? {
        return try {
            element.text().trim()
        } catch (e: Exception) {
            null
        }
    }

    fun text(cssQuery: String): String? {
        return try {
            element.select(cssQuery).first().text().trim()
        } catch (e: Exception) {
            null
        }
    }

    fun textWithSubstring(cssQuery: String, start: Int, end: Int): String? {
        return StringUtils.substring(text(cssQuery), start, end)
    }

    fun textWithSubstring(cssQuery: String, start: Int): String? {
        return textWithSubstring(cssQuery, start, -1)
    }

    fun hrefWithLastSplit(): String {
        val string = href()
        val arr = string!!.split("/".toRegex()).toTypedArray()
        return arr[arr.size - 1]
    }

    fun textWithSplit(cssQuery: String, regex: String, index: Int): String? {
        return StringUtils.split(text(cssQuery), regex, index)
    }

    fun attr(attr: String): String? {
        return try {
            element.attr(attr).trim()
        } catch (e: Exception) {
            null
        }
    }

    fun attr(cssQuery: String, attr: String): String? {
        return try {
            element.select(cssQuery).first().attr(attr).trim()
        } catch (e: Exception) {
            null
        }
    }

    fun attrWithSubString(attr: String, start: Int, end: Int): String? {
        return StringUtils.substring(attr(attr), start, end)
    }

    fun attrWithSubString(attr: String, start: Int): String? {
        return attrWithSubString(attr, start, -1)
    }

    fun attrWithSubString(cssQuery: String, attr: String, start: Int, end: Int): String? {
        return StringUtils.substring(attr(cssQuery, attr), start, end)
    }

    fun attrWithSubString(cssQuery: String, attr: String, start: Int): String? {
        return attrWithSubString(cssQuery, attr, start, -1)
    }

    fun attrWithSplit(attr: String, regex: String, index: Int): String? {
        return StringUtils.split(attr(attr), regex, index)
    }

    fun attrWithSplit(cssQuery: String, attr: String, regex: String, index: Int): String? {
        return StringUtils.split(attr(cssQuery, attr), regex, index)
    }

    fun src(): String? {
        return attr("src")
    }

    fun src(cssQuery: String): String? {
        return attr(cssQuery, "src")
    }

    fun dataUrl(cssQuery: String): String? {
        return attr(cssQuery, "data-url")
    }

    fun href(): String? {
        return attr("href")
    }

    fun href(cssQuery: String): String? {
        return attr(cssQuery, "href")
    }

    fun hrefWithSubString(start: Int, end: Int): String? {
        return attrWithSubString("href", start, end)
    }

    fun hrefWithSubString(start: Int): String? {
        return hrefWithSubString(start, -1)
    }

    fun hrefWithSubString(cssQuery: String, start: Int, end: Int): String? {
        return attrWithSubString(cssQuery, "href", start, end)
    }

    fun hrefWithSubString(cssQuery: String, start: Int): String? {
        return hrefWithSubString(cssQuery, start, -1)
    }

    fun hrefWithSplit(index: Int): String? {
        return splitHref(href(), index)
    }

    fun hrefWithSplit(cssQuery: String, index: Int): String? {
        return splitHref(href(cssQuery), index)
    }

    companion object {
        @JvmStatic
        fun splitHref(str: String?, index: Int): String? {
            if (str == null) {
                return null
            }
            var value = str.replaceFirst(".*\\..*?/".toRegex(), "")
            value = value.replace("[/\\.=\\?]".toRegex(), " ")
            value = value.trim()
            return StringUtils.split(value, "\\s+", index)
        }
    }
}
