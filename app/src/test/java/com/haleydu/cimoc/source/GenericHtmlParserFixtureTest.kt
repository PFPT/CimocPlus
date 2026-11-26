package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.SourceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericHtmlParserFixtureTest {
    @Test
    fun detailFixtureKeepsTitleTagsAndChapters() {
        val html = javaClass.classLoader!!.getResource("fixtures/detail.html")!!.readText()
        val parser = GenericHtmlParser(null, config())
        val comic = parser.parseInfo(html, Comic(7, "cid"))
        assertEquals("Fixture Comic", comic.title)
        assertEquals(listOf("冒险", "热血"), comic.genres)
        val chapters = parser.parseChapter(html, comic, 7L)
        assertEquals(2, chapters.size)
        assertEquals("第一话", chapters[0].title)
        assertEquals("/c/1", chapters[0].path)
        assertEquals("第二话", chapters[1].title)
        assertEquals("/c/2", chapters[1].path)
    }

    @Test
    fun pageFixtureKeepsImageUrls() {
        val html = javaClass.classLoader!!.getResource("fixtures/pages.html")!!.readText()
        val parser = GenericHtmlParser(null, config())
        val chapter = Chapter(11L, 7L, "第一话", "/c/1")
        val images = parser.parseImages(html, chapter)
        assertEquals(2, images.size)
        assertEquals("https://fixture.example/p/1.jpg", images[0].urls?.firstOrNull())
        assertEquals("https://fixture.example/p/2.jpg", images[1].urls?.firstOrNull())
    }

    private fun config(): SourceConfig {
        return SourceConfig(
            "fixture",
            7,
            "Fixture",
            mapOf(
                "baseUrl" to "https://fixture.example",
                "parseInfoTitle" to "h1.title",
                "parseInfoTags" to ".tags a",
                "parseChapterList1" to ".chapters a",
                "parseImageList" to ".pages img"
            )
        )
    }
}
