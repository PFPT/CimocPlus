package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import org.junit.Assert.assertEquals
import org.junit.Test

class ChuiXueFixtureTest {
    @Test
    fun detailFixtureKeepsTitleAndChapters() {
        val html = javaClass.classLoader!!.getResource("fixtures/chuixue-detail.html")!!.readText()
        val parser = ChuiXue(null)
        val comic = parser.parseInfo(html, Comic(1, "cid"))
        assertEquals("吹雪漫画", comic.title)
        val chapters = parser.parseChapter(html, comic, 7L)
        assertEquals(2, chapters.size)
        assertEquals("第一话", chapters[0].title)
        assertEquals("1", chapters[0].path)
        assertEquals("第二话", chapters[1].title)
        assertEquals("2", chapters[1].path)
    }

    @Test
    fun pageFixtureKeepsImageUrls() {
        val html = javaClass.classLoader!!.getResource("fixtures/chuixue-pages.html")!!.readText()
        val parser = ChuiXue(null)
        val chapter = Chapter(11L, 7L, "第一话", "1")
        val images = parser.parseImages(html, chapter)
        assertEquals(2, images.size)
        assertEquals("http://chuixue1.tianshigege.com/a.jpg", images[0].urls?.firstOrNull())
        assertEquals("http://chuixue1.tianshigege.com/b.jpg", images[1].urls?.firstOrNull())
    }
}
