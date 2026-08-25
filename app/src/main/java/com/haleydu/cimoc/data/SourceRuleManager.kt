package com.haleydu.cimoc.data
import com.haleydu.cimoc.db.SourceDao
import com.haleydu.cimoc.db.SourceRuleDao
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceRule
import com.haleydu.cimoc.script.JsHttpClient
import com.haleydu.cimoc.script.ScriptRunner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRuleManager @Inject constructor(
    private val sourceDao: SourceDao,
    private val sourceRuleDao: SourceRuleDao,
    private val sourceManager: SourceManager,
    private val scriptRunner: ScriptRunner,
    private val httpClient: JsHttpClient
) {

    fun import(input: String): Int {
        val text = input.trim()
        if (text.isEmpty()) throw IllegalArgumentException("empty")
        val remote = isRemoteUrl(text)
        val script = if (remote) httpClient.get(text) else text
        return importScript(script, if (remote) text else null)
    }

    fun importScript(script: String, remoteUrl: String? = null): Int {
        if (script.isBlank()) throw IllegalArgumentException("empty")
        val meta = scriptRunner.extractMeta(script)
        val type = if (meta.type != null && meta.type >= 0) meta.type else nextType()
        val title = meta.title?.takeIf { it.isNotBlank() } ?: "JS $type"
        val version = meta.version ?: ""
        val existing = sourceRuleDao.load(type)
        val rule = existing ?: SourceRule()
        rule.type = type
        rule.scriptContent = script
        rule.version = version
        rule.remoteUrl = remoteUrl ?: existing?.remoteUrl
        rule.updatedAt = System.currentTimeMillis()
        sourceRuleDao.insertOrReplace(rule)
        var source = sourceDao.load(type)
        if (source == null) {
            sourceDao.insert(Source(null, title, type, true))
        } else if (source.title != title && type >= TYPE_JS_START) {
            source.title = title
            sourceDao.update(source)
        }
        sourceManager.clearParserCache()
        return type
    }

    fun refreshRemote() {
        for (rule in sourceRuleDao.listRemote()) {
            val url = rule.remoteUrl ?: continue
            try {
                httpClient.validateUrl(url)
                val script = httpClient.get(url)
                if (script.isBlank() || script == rule.scriptContent) continue
                val meta = scriptRunner.extractMeta(script)
                val version = meta.version ?: ""
                if (version.isNotEmpty() && version == rule.version) continue
                rule.scriptContent = script
                rule.version = version
                rule.updatedAt = System.currentTimeMillis()
                sourceRuleDao.update(rule)
            } catch (_: Exception) {
            }
        }
        sourceManager.clearParserCache()
    }

    private fun isRemoteUrl(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    private fun nextType(): Int {
        var type = TYPE_JS_START
        while (sourceDao.load(type) != null || sourceRuleDao.load(type) != null) {
            type++
        }
        return type
    }

    companion object {
        const val TYPE_JS_START = 10000
    }
}
