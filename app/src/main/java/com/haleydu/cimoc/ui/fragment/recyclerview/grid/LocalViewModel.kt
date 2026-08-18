package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import android.util.Pair
import androidx.collection.LongSparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.Local
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.manager.TaskManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.saf.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalViewModel @Inject constructor(
    private val comicManager: ComicManager,
    private val taskManager: TaskManager
) : ViewModel() {

    private val _comics = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val comics: SharedFlow<List<Any>> = _comics

    private val _scanSuccess = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val scanSuccess: SharedFlow<List<Any>> = _scanSuccess

    private val _deleteSuccess = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<Long> = _deleteSuccess

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MiniComic.listOf(comicManager.listLocal())
                }
                _comics.emit(list)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    fun loadComic(id: Long): Comic = comicManager.load(id)

    fun scan(doc: DocumentFile) {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MiniComic.listOf(processScanResult(Local.scan(doc)))
                }
                _scanSuccess.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    fun deleteComic(id: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    comicManager.runInTx {
                        taskManager.deleteByComicId(id)
                        comicManager.deleteByKey(id)
                    }
                }
                _deleteSuccess.emit(id)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    private fun buildHash(): Pair<Map<String, Comic>, Set<String>> {
        val array = LongSparseArray<MutableList<String>>()
        val map = HashMap<String, Comic>()
        val set = HashSet<String>()
        for (task in taskManager.list()) {
            var list = array.get(task.key)
            if (list == null) {
                list = ArrayList()
                array.put(task.key, list)
            }
            list.add(task.path)
        }
        for (comic in comicManager.listLocal()) {
            map[comic.cid] = comic
            set.addAll(array.get(comic.id, ArrayList()))
        }
        return Pair.create(map, set)
    }

    private fun processScanResult(pairs: List<Pair<Comic, ArrayList<Task>>>): List<Comic> {
        return comicManager.callInTx {
            val hash = buildHash()
            val result = ArrayList<Comic>()
            for (pr in pairs) {
                val comic = hash.first[pr.first.cid]
                if (comic != null) {
                    for (task in pr.second) {
                        task.key = comic.id
                        if (!hash.second.contains(task.path)) {
                            taskManager.insert(task)
                        }
                    }
                } else {
                    comicManager.insert(pr.first)
                    for (task in pr.second) {
                        task.key = pr.first.id
                        taskManager.insert(task)
                    }
                    result.add(pr.first)
                }
            }
            result
        }
    }
}
