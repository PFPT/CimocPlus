package com.haleydu.cimoc.ui.explore
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.R
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.SourceRuleManager
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.script.JsConsole
import com.haleydu.cimoc.script.JsHttpClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SourceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
    private val sourceRuleManager: SourceRuleManager,
    private val sourceConfigManager: SourceConfigManager,
    private val sourceHealthManager: SourceHealthManager,
    private val httpClient: JsHttpClient,
    private val jsConsole: JsConsole
) : ViewModel() {

    private val _sources = MutableSharedFlow<List<Source>>(extraBufferCapacity = 1)
    val sources: SharedFlow<List<Source>> = _sources

    private val _invalidTypes = MutableStateFlow<Set<Int>>(emptySet())
    val invalidTypes: StateFlow<Set<Int>> = _invalidTypes

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    private val _importSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importSuccess: SharedFlow<Unit> = _importSuccess

    private val _importFail = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val importFail: SharedFlow<String> = _importFail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { sourceManager.list() }
                _invalidTypes.value = sourceHealthManager.invalidTypes()
                _sources.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    fun update(source: Source) {
        sourceManager.update(source)
    }

    fun importScript(input: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { importInternal(input) }
                _importSuccess.emit(Unit)
                val list = withContext(Dispatchers.IO) { sourceManager.list() }
                _invalidTypes.value = sourceHealthManager.invalidTypes()
                _sources.emit(list)
            } catch (e: Exception) {
                _importFail.emit(failMessage(e))
            }
        }
    }

    private fun importInternal(input: String) {
        val text = input.trim()
        if (text.isEmpty()) {
            throw IllegalArgumentException(context.getString(R.string.source_import_fail_empty))
        }
        val remoteUrl = if (isRemoteUrl(text)) toRawUrl(text) else null
        val content = if (remoteUrl != null) {
            val body = httpClient.get(remoteUrl)
            if (body.isBlank()) {
                throw IllegalArgumentException(context.getString(R.string.source_import_fail_empty))
            }
            if (looksLikeHtml(body)) {
                throw IllegalArgumentException(context.getString(R.string.source_import_fail_html))
            }
            body
        } else {
            text
        }
        if (sourceConfigManager.importJson(content)) {
            sourceManager.clearParserCache()
            return
        }
        if (looksLikeJson(content)) {
            throw IllegalArgumentException(context.getString(R.string.source_import_fail_format))
        }
        sourceRuleManager.importScript(content, remoteUrl)
    }

    private fun failMessage(e: Exception): String {
        val message = e.message?.takeIf { it.isNotBlank() }
        return when {
            e is IllegalArgumentException && message != null -> message
            e is IOException -> context.getString(R.string.source_import_fail_http, message ?: "")
            message != null -> context.getString(R.string.source_import_fail_detail, message)
            else -> context.getString(R.string.source_import_fail)
        }
    }

    private fun isRemoteUrl(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    private fun toRawUrl(url: String): String {
        val match = GITHUB_BLOB.matchEntire(url.trim()) ?: return url.trim()
        return "https://raw.githubusercontent.com/${match.groupValues[2]}/${match.groupValues[3]}/${match.groupValues[4]}"
    }

    private fun looksLikeHtml(text: String): Boolean {
        return text.trimStart().startsWith("<")
    }

    private fun looksLikeJson(text: String): Boolean {
        val start = text.trimStart()
        return start.startsWith("{") || start.startsWith("[")
    }

    fun logs(): String = jsConsole.dump()

    fun hasCategory(type: Int): Boolean = sourceManager.getParser(type).category != null

    companion object {
        private val GITHUB_BLOB = Regex(
            """^https?://(?:www\.)?github\.com/([^/]+)/([^/]+)/blob/(.+)$""",
            RegexOption.IGNORE_CASE
        )
    }
}
