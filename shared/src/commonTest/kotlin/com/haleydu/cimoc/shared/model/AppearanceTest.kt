package com.haleydu.cimoc.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceTest {
    @Test
    fun themeIndexStaysInRange() {
        assertEquals(0, Appearance.theme(-4))
        assertEquals(0, Appearance.theme(0))
        assertEquals(4, Appearance.theme(4))
        assertEquals(4, Appearance.theme(12))
        assertEquals("青墨", Appearance.themeTitle(0))
        assertEquals("素纸", Appearance.themeTitle(9))
    }

    @Test
    fun iconStyleIndexStaysInRange() {
        assertEquals(0, Appearance.iconStyle(-1))
        assertEquals(2, Appearance.iconStyle(2))
        assertEquals(4, Appearance.iconStyle(8))
        assertEquals("填充", Appearance.iconTitle(0))
        assertEquals("印章", Appearance.iconTitle(40))
    }
}
