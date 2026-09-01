package com.haleydu.cimoc.soup

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

class MDocument(html: String) {
    private val document: Document = Jsoup.parse(html)

    fun text(cssQuery: String): String? {
        return try {
            val elements: Elements = document.select(cssQuery)
            return java.lang.String.valueOf(elements.first())
        } catch (e: Exception) {
            null
        }
    }
}
