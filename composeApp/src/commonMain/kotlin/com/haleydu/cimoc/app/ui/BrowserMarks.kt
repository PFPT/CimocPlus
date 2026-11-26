package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haleydu.cimoc.shared.repo.SettingsRepository

data class BrowserMark(
    val url: String,
    val title: String,
    val time: Long,
    val pinned: Boolean
)

object BrowserMarks {
    private const val KEY = "pref_browser_marks"
    private const val QUERY_KEY = "pref_browser_queries"
    private const val SEP = "\u001f"
    private const val HISTORY_LIMIT = 40
    private const val QUERY_LIMIT = 20

    var revision by mutableStateOf(0)
        private set

    fun load(settings: SettingsRepository): List<BrowserMark> {
        val raw = settings.string(KEY)
        if (raw.isEmpty()) return emptyList()
        return raw.lineSequence().mapNotNull { parse(it) }.toList()
    }

    fun queries(settings: SettingsRepository): List<String> {
        return settings.string(QUERY_KEY)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(QUERY_LIMIT)
            .toList()
    }

    fun rememberQuery(settings: SettingsRepository, raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("http://") || trimmed.startsWith("https://")) return
        val next = (listOf(trimmed) + queries(settings).filter { it != trimmed }).take(QUERY_LIMIT)
        settings.setString(QUERY_KEY, next.joinToString("\n"))
        revision++
    }

    fun remember(settings: SettingsRepository, url: String, title: String) {
        if (!keep(url)) return
        val current = load(settings).toMutableList()
        val index = current.indexOfFirst { it.url == url }
        val previous = current.getOrNull(index)
        val label = clean(title).ifBlank { previous?.title.orEmpty() }
        val pinned = previous?.pinned ?: false
        val head = current.firstOrNull { it.pinned == pinned }
        if (head?.url == url && previous?.title == label) return
        if (index >= 0) current.removeAt(index)
        current.add(0, BrowserMark(url, label, nextStamp(current), pinned))
        save(settings, trim(current))
    }

    fun togglePin(settings: SettingsRepository, url: String, title: String) {
        if (!keep(url)) return
        val current = load(settings).toMutableList()
        val index = current.indexOfFirst { it.url == url }
        val previous = current.getOrNull(index)
        val label = clean(title).ifBlank { previous?.title.orEmpty() }
        if (index >= 0) current.removeAt(index)
        current.add(0, BrowserMark(url, label, nextStamp(current), previous?.pinned != true))
        save(settings, trim(current))
    }

    private fun trim(list: List<BrowserMark>): List<BrowserMark> {
        val pins = list.filter { it.pinned }
        val recent = list.filter { !it.pinned }.take(HISTORY_LIMIT)
        return pins + recent
    }

    private fun save(settings: SettingsRepository, list: List<BrowserMark>) {
        val raw = list.joinToString("\n") { mark ->
            listOf(
                mark.url.replace("\n", ""),
                mark.title,
                mark.time.toString(),
                if (mark.pinned) "1" else "0"
            ).joinToString(SEP)
        }
        settings.setString(KEY, raw)
        revision++
    }

    private fun parse(line: String): BrowserMark? {
        val parts = line.split(SEP)
        if (parts.size < 4 || parts[0].isEmpty()) return null
        return BrowserMark(
            url = parts[0],
            title = parts[1],
            time = parts[2].toLongOrNull() ?: 0L,
            pinned = parts[3] == "1"
        )
    }

    private fun nextStamp(list: List<BrowserMark>): Long {
        val max = list.maxOfOrNull { it.time } ?: 0L
        return max + 1
    }

    private fun keep(url: String): Boolean {
        if (url.isEmpty() || url.startsWith("about:")) return false
        val host = pageHost(url) ?: return false
        return !isSearchHost(host)
    }

    private fun clean(title: String) = title.replace(SEP, " ").replace("\n", " ").trim()
}
