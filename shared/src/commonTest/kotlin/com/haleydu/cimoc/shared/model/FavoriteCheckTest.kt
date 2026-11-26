package com.haleydu.cimoc.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteCheckTest {
    @Test
    fun namesTheSourceAndKind() {
        val text = favoriteCheckText(
            FavoriteCheck(
                updated = 1,
                failures = listOf(CheckFailure("吹雪", "NETWORK"), CheckFailure("扑飞", "EMPTY"))
            )
        )
        assertEquals("发现 1 部更新，失败：吹雪（网络），扑飞（空结果）", text)
    }

    @Test
    fun successDropsTheFailureLine() {
        assertEquals("已是最新", favoriteCheckText(FavoriteCheck()))
        assertEquals("发现 2 部更新", favoriteCheckText(FavoriteCheck(updated = 2)))
        assertEquals("解析", checkKindLabel("PARSE"))
    }
}
