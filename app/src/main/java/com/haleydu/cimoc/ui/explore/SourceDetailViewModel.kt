package com.haleydu.cimoc.ui.explore
import androidx.lifecycle.ViewModel
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.SourceManager
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
