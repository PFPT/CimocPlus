package com.haleydu.cimoc.script

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsJsoup @Inject constructor() {
    fun parse(html: String): JsDocument {
        return JsDocument(Jsoup.parse(html))
    }
}

class JsDocument(private val document: Document) {
    fun select(css: String): Array<JsElement> {
        return document.select(css).map { JsElement(it) }.toTypedArray()
    }

    fun selectFirst(css: String): JsElement? {
        val element = document.selectFirst(css) ?: return null
        return JsElement(element)
    }

    fun text(): String = document.text()

    fun html(): String = document.html()

    fun title(): String = document.title()

    fun attr(name: String): String = document.attr(name)
}

class JsElement(private val element: Element) {
    fun select(css: String): Array<JsElement> {
        return element.select(css).map { JsElement(it) }.toTypedArray()
    }

    fun selectFirst(css: String): JsElement? {
        val child = element.selectFirst(css) ?: return null
        return JsElement(child)
    }

    fun text(): String = element.text()

    fun ownText(): String = element.ownText()

    fun html(): String = element.html()

    fun outerHtml(): String = element.outerHtml()

    fun attr(name: String): String = element.attr(name)
}
