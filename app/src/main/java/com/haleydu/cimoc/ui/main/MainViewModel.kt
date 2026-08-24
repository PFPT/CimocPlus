package com.haleydu.cimoc.ui.main
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.Update
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.SourceRuleManager
import com.haleydu.cimoc.model.MiniComic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val comicManager: ComicManager,
    private val sourceManager: SourceManager,
    private val sourceConfigManager: SourceConfigManager,
    private val sourceRuleManager: SourceRuleManager,
    private val httpClient: OkHttpClient
) : ViewModel() {

    data class LastComic(
        val id: Long,
        val source: Int,
        val cid: String,
        val title: String,
        val cover: String
    )

    sealed class UpdateEvent {
        object Ready : UpdateEvent()
        data class GiteeReady(
            val versionName: String,
            val content: String,
            val url: String,
            val versionCode: Int,
            val md5: String
        ) : UpdateEvent()
    }

    private val _last = MutableSharedFlow<LastComic>(extraBufferCapacity = 1)
    val last: SharedFlow<LastComic> = _last

    private val _lastFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lastFail: SharedFlow<Unit> = _lastFail

    private val _update = MutableSharedFlow<UpdateEvent>(extraBufferCapacity = 1)
    val update: SharedFlow<UpdateEvent> = _update

    fun headerGetter(): SourceManager.HeaderGetter = sourceManager.HeaderGetter()

    fun checkLocal(id: Long): Boolean {
        val comic = comicManager.load(id)
        return comic != null && comic.local
    }

    fun loadLast() {
        viewModelScope.launch {
            try {
                val comic = withContext(Dispatchers.IO) { comicManager.loadLast() }
                if (comic != null) {
                    _last.emit(LastComic(comic.id, comic.source, comic.cid, comic.title, comic.cover))
                }
            } catch (e: Exception) {
                _lastFail.emit(Unit)
            }
        }
    }

    fun checkUpdate(version: String) {
        viewModelScope.launch {
            try {
                val s = withContext(Dispatchers.IO) { Update.check(httpClient) }
                if (version.indexOf(s) == -1 && version.indexOf("t") == -1) {
                    _update.emit(UpdateEvent.Ready)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun checkGiteeUpdate(appVersionCode: Int) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { Update.checkGitee(httpClient) }
                val obj = JSONObject(json)
                val versionName = obj.getString(APP_VERSIONNAME)
                val serverAppVersionCode = obj.getString(APP_VERSIONCODE).toInt()
                val content = obj.getString(APP_CONTENT)
                val md5 = obj.getString(APP_MD5)
                val url = obj.getString(APP_URL)
                if (appVersionCode < serverAppVersionCode) {
                    _update.emit(UpdateEvent.GiteeReady(versionName, content, url, serverAppVersionCode, md5))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSourceBaseUrl() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sourceConfigManager.fetchRemote()
                sourceConfigManager.applyToDatabase()
                sourceRuleManager.refreshRemote()
                sourceManager.clearParserCache()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val APP_VERSIONNAME = "versionName"
        private const val APP_VERSIONCODE = "versionCode"
        private const val APP_CONTENT = "content"
        private const val APP_MD5 = "md5"
        private const val APP_URL = "url"
    }
}
