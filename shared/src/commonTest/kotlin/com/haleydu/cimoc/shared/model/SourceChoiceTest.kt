package com.haleydu.cimoc.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceChoiceTest {
    @Test
    fun searchListDropsWebSources() {
        val sources = listOf(
            SourceItem(type = 1, title = "built", enable = true),
            SourceItem(type = 10001, title = "site", enable = true, web = true),
            SourceItem(type = 2, title = "off", enable = false)
        )
        assertEquals(listOf(1), sourcesForSearch(sources).map { it.type })
    }

    @Test
    fun defaultSelectionSkipsInvalid() {
        val sources = sourcesForSearch(
            listOf(
                SourceItem(type = 1, title = "ok", enable = true),
                SourceItem(type = 2, title = "bad", enable = true, invalid = true),
                SourceItem(type = 10001, title = "site", enable = true, web = true)
            )
        )
        assertEquals(setOf(1), selectedSearchTypes(sources, emptySet()))
    }

    @Test
    fun previousSelectionKeepsEnabledSources() {
        val sources = listOf(
            SourceItem(type = 1, title = "ok", enable = true),
            SourceItem(type = 2, title = "bad", enable = true, invalid = true)
        )
        assertEquals(setOf(2), selectedSearchTypes(sources, setOf(2, 9)))
    }
}
