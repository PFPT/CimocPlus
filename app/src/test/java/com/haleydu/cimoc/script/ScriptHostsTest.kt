package com.haleydu.cimoc.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptHostsTest {
    @Test
    fun readsHostAndTreatsStubAsSite() {
        val script = "var TITLE = \"example.com\";\nvar HOST = \"Example.com\";\n"
        assertEquals("Example.com", ScriptHosts.hostOf(script))
        assertEquals("example.com", ScriptHosts.titleOf(script))
        assertEquals("cdn.example.com", ScriptHosts.hostOfUrl("https://cdn.example.com/a.js"))
        assertEquals("example.com", ScriptHosts.normalizeHost("WWW.Example.com"))
        assertTrue(ScriptHosts.isSiteScript(script))
    }

    @Test
    fun fullScriptIsNotASiteStub() {
        val script = """
            var HOST = "example.com";
            function search(keyword, page) { return "[]"; }
            function getDetail(cid) { return "{}"; }
        """.trimIndent()
        assertFalse(ScriptHosts.isSiteScript(script))
        assertTrue(ScriptHosts.looksLikeScript(script))
    }

    @Test
    fun htmlAndEmptyAreNotScripts() {
        assertFalse(ScriptHosts.looksLikeScript("<html></html>"))
        assertFalse(ScriptHosts.looksLikeScript(""))
        assertNull(ScriptHosts.hostOf("function search() {}"))
    }
}
