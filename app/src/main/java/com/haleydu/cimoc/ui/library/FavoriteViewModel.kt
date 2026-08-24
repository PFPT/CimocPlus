package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.manager.PreferenceManager
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.manager.TagRefManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val comicManager: ComicManager,
    private val sourceManager: SourceManager,
    private val tagRefManager: TagRefManager,
    private val preferenceManager: PreferenceManager,
    private val mangaService: MangaService
) : ViewModel() {

    sealed class CheckEvent {
        data class Progress(val comic: MiniComic?, val progress: Int, val max: Int) : CheckEvent()
        object Fail : CheckEvent()
        object Complete : CheckEvent()
    }

    private val _comics = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val comics: SharedFlow<List<Any>> = _comics

    private val _check = MutableSharedFlow<CheckEvent>(extraBufferCapacity = 8)
    val check: SharedFlow<CheckEvent> = _check

    fun load() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                MiniComic.listOf(comicManager.listFavoriteOrdered())
            }
            _comics.emit(list)
        }
    }

    fun loadComic(id: Long): Comic = comicManager.load(id)

    fun cancelAllHighlight() {
        comicManager.cancelHighlight()
    }

    fun unfavoriteComic(id: Long) {
        val comic = comicManager.load(id)
        comic.favorite = null
        tagRefManager.deleteByComic(id)
        comicManager.updateOrDelete(comic)
    }

    fun checkUpdate() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { comicManager.listFavorite() }
                var count = 0
                mangaService.checkUpdate(sourceManager, list).collect { comic ->
                    if (comic != null) {
                        withContext(Dispatchers.IO) { comicManager.update(comic) }
                    }
                    _check.emit(
                        CheckEvent.Progress(
                            comic?.let { MiniComic(it) },
                            ++count,
                            list.size
                        )
                    )
                }
                _check.emit(CheckEvent.Complete)
                logCheck(true, App.getAppContext().getString(R.string.favorite_check_update_done))
            } catch (e: Exception) {
                _check.emit(CheckEvent.Fail)
                logCheck(false, e.toString())
            }
        }
    }

    private fun logCheck(success: Boolean, content: String) {
        if (preferenceManager.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            val context = App.getAppContext()
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CONTENT, content)
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, success)
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, bundle)
        }
    }
}
