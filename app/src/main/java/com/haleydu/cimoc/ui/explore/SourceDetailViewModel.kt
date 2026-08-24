package com.haleydu.cimoc.ui.activity

import androidx.lifecycle.ViewModel
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.manager.SourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SourceDetailViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val comicManager: ComicManager
) : ViewModel() {

    data class Detail(val type: Int, val title: String, val count: Long)

    fun load(type: Int): Detail {
        val source = sourceManager.load(type)
        val count = comicManager.countBySource(type)
        return Detail(type, source.title, count)
    }
}
