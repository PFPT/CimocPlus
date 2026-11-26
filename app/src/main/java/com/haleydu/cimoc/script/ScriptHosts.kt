package com.haleydu.cimoc.script

object ScriptHosts {
    private val hostPattern = Regex("""var\s+HOST\s*=\s*["']([^"']+)["']""")

    fun hostOf(script: String): String? {
        return hostPattern.find(script)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun isSiteScript(script: String): Boolean {
        if (hostOf(script) == null) return false
        return !script.contains("function search") && !script.contains("function getDetail")
    }

    fun looksLikeScript(text: String): Boolean {
        val body = text.trim()
        if (body.isEmpty() || body.startsWith("<") || body.startsWith("{")) return false
        return body.contains("function search") ||
            body.contains("function getDetail") ||
            body.contains("function getChapterList")
    }

    fun normalizeHost(host: String): String {
        return host.trim().lowercase().removePrefix("www.")
    }
}
