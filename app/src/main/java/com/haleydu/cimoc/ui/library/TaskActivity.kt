package com.haleydu.cimoc.ui.library
import com.haleydu.cimoc.ui.common.BackActivity
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivityComposeBinding
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.service.DownloadService.DownloadServiceBinder
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.ItemDialogFragment
import com.haleydu.cimoc.ui.detail.ChapterActivity
import com.haleydu.cimoc.ui.detail.DetailActivity
import com.haleydu.cimoc.ui.reader.ChapterListHolder
import com.haleydu.cimoc.ui.reader.ReaderActivity
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.utils.StringUtils
import com.haleydu.cimoc.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskActivity : BackActivity(), DialogCaller {

    private val vm: TaskViewModel by viewModels()
    private lateinit var binding: ActivityComposeBinding
    private var connection: ServiceConnection? = null
    private var binder: DownloadServiceBinder? = null
    private var taskOrder = false
    private var savedTask: Task? = null

    private var tasks by mutableStateOf(emptyList<Task>(), neverEqualPolicy())
    private var lastPath by mutableStateOf<String?>(null)
    private var showDetailFab by mutableStateOf(true)
    private var showPlayFab by mutableStateOf(true)

    override fun inflateContentView(): View {
        binding = ActivityComposeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        binding.composeView.setContent {
            MaterialTheme {
                TaskScreen(
                    tasks = tasks,
                    lastPath = lastPath,
                    showDetailFab = showDetailFab,
                    showPlayFab = showPlayFab,
                    onTaskClick = { onTaskClick(it) },
                    onTaskLongClick = { onTaskLongClick(it) },
                    onDetailClick = { onDetailClick() },
                    onPlayClick = { onPlayClick() }
                )
            }
        }
    }

    override fun initData() {
        val key = intent.getLongExtra(Extra.EXTRA_ID, -1)
        taskOrder = mPreference.getBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, false)
        vm.loadSuccess.collectOnStart(this) { onTaskLoadSuccess(it.list, it.isLocal) }
        vm.loadFail.collectOnStart(this) { onTaskLoadFail() }
        vm.deleteSuccess.collectOnStart(this) { onTaskDeleteSuccess(it) }
        vm.deleteFail.collectOnStart(this) { onTaskDeleteFail() }
        AppEventBus.observe(AppEvent.EVENT_TASK_STATE_CHANGE).collectOnStart(this) { event ->
            val id = (event.getData(1) as Number).toLong()
            when ((event.getData() as Number).toInt()) {
                Task.STATE_PARSE -> onTaskParse(id)
                Task.STATE_ERROR -> onTaskError(id)
                Task.STATE_PAUSE -> onTaskPause(id)
            }
        }
        AppEventBus.observe(AppEvent.EVENT_TASK_PROCESS).collectOnStart(this) { event ->
            onTaskProcess(
                (event.getData() as Number).toLong(),
                (event.getData(1) as Number).toInt(),
                (event.getData(2) as Number).toInt()
            )
        }
        AppEventBus.observe(AppEvent.EVENT_TASK_INSERT).collectOnStart(this) { event ->
            @Suppress("UNCHECKED_CAST")
            val list = event.getData(1) as List<Task>
            val task = list[0]
            val comic = vm.comic
            if (comic != null && task.key == comic.id) {
                onTaskAdd(list)
            }
        }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE).collectOnStart(this) { event ->
            val comic = vm.comic
            if (comic != null && comic.id != null && comic.id == (event.getData() as Number).toLong()) {
                vm.refreshLast()
                onLastChange(vm.comic?.last)
            }
        }
        vm.load(key, taskOrder)
    }

    override fun onDestroy() {
        super.onDestroy()
        connection?.let { unbindService(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_task, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (tasks.isNotEmpty()) {
            when (item.itemId) {
                R.id.task_history -> {
                    var path = vm.comic?.last
                    if (path == null) {
                        path = tasks[if (taskOrder) 0 else tasks.lastIndex].path
                    }
                    startReader(path, true)
                }
                R.id.task_delete -> {
                    val list = ArrayList<Chapter>(tasks.size)
                    tasks.forEachIndexed { i, task ->
                        val sourceComic = (task.source.toString() + "000" + task.id).toLong()
                        val id = (sourceComic.toString() + "000" + i).toLong()
                        list.add(Chapter(id, sourceComic, task.title, task.path, task.id))
                    }
                    startActivityForResult(ChapterActivity.createIntent(this, list), REQUEST_CODE_DELETE)
                }
                R.id.detail_search_title -> {
                    if (!StringUtils.isEmpty(vm.comic?.title)) {
                        startActivity(
                            ResultActivity.createIntent(
                                this,
                                vm.comic?.title,
                                true,
                                vm.enabledSourceTypes(),
                                ResultActivity.LAUNCH_MODE_SEARCH
                            )
                        )
                    } else {
                        showSnackbar(R.string.common_keyword_empty)
                    }
                }
                R.id.detail_search_author -> {
                    if (!StringUtils.isEmpty(vm.comic?.author)) {
                        startActivity(
                            ResultActivity.createIntent(
                                this,
                                vm.comic?.author,
                                true,
                                vm.enabledSourceTypes(),
                                ResultActivity.LAUNCH_MODE_SEARCH
                            )
                        )
                    } else {
                        showSnackbar(R.string.common_keyword_empty)
                    }
                }
                R.id.task_sort -> {
                    tasks = tasks.reversed()
                    taskOrder = !taskOrder
                    mPreference.putBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, taskOrder)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_REQUEST_OPERATION) {
            when (bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)) {
                OPERATION_READ -> startReader(savedTask?.path, true)
                OPERATION_DELETE -> {
                    val task = savedTask ?: return
                    showProgressDialog()
                    val sourceComic = (task.source.toString() + "000" + task.id).toLong()
                    val id = (sourceComic.toString() + "000" + 0).toLong()
                    val list = listOf(Chapter(id, sourceComic, task.title, task.path, task.id))
                    if (vm.comic?.local != true) {
                        binder?.service?.removeDownload(task.id)
                    }
                    vm.deleteTask(list, tasks.size == 1)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_DELETE) {
            val list = ChapterListHolder.take()
                ?: data?.getParcelableArrayListExtra<Chapter>(Extra.EXTRA_CHAPTER)
            if (list != null && list.isNotEmpty()) {
                showProgressDialog()
                for (chapter in list) {
                    binder?.service?.removeDownload(chapter.tid)
                }
                vm.deleteTask(list, tasks.size == list.size)
            } else {
                showSnackbar(R.string.task_empty)
            }
        }
    }

    private fun onDetailClick() {
        val comic = vm.comic ?: return
        startActivity(DetailActivity.createIntent(this, comic.id, comic.source, comic.cid))
    }

    private fun onPlayClick() {
        tasks.forEach { task ->
            if (task.state == Task.STATE_PAUSE || task.state == Task.STATE_ERROR) {
                task.state = Task.STATE_WAIT
                val taskIntent = DownloadService.createIntent(this, task)
                DownloadService.start(this, taskIntent)
            }
        }
        notifyTasks()
    }

    private fun onTaskClick(task: Task) {
        when (task.state) {
            Task.STATE_FINISH -> startReader(task.path, false)
            Task.STATE_PAUSE, Task.STATE_ERROR -> {
                task.state = Task.STATE_WAIT
                notifyTasks()
                val taskIntent = DownloadService.createIntent(this, task)
                DownloadService.start(this, taskIntent)
            }
            Task.STATE_WAIT -> {
                task.state = Task.STATE_PAUSE
                notifyTasks()
                binder?.service?.removeDownload(task.id)
            }
            Task.STATE_DOING, Task.STATE_PARSE -> binder?.service?.removeDownload(task.id)
        }
    }

    private fun onTaskLongClick(task: Task) {
        savedTask = task
        val item = arrayOf(getString(R.string.task_read), getString(R.string.task_delete))
        ItemDialogFragment.newInstance(R.string.common_operation_select, item, DIALOG_REQUEST_OPERATION)
            .show(supportFragmentManager, null)
    }

    private fun onLastChange(path: String?) {
        lastPath = path
    }

    private fun startReader(path: String?, preview: Boolean) {
        if (path == null) return
        val list = ArrayList<Chapter>()
        tasks.forEachIndexed { i, t ->
            val sourceComic = (t.source.toString() + "000" + t.id).toLong()
            val id = (sourceComic.toString() + i).toLong()
            if (preview && t.progress > 0) {
                list.add(Chapter(id, sourceComic, t.title, t.path, t.progress, true, true, t.id))
            } else if (t.state == Task.STATE_FINISH) {
                list.add(Chapter(id, sourceComic, t.title, t.path, t.max, true, true, t.id))
            }
        }
        lastPath = path
        val id = vm.updateLast(path)
        val mode = mPreference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE)
        startActivity(ReaderActivity.createIntent(this, id, list, mode))
    }

    private fun onTaskDeleteSuccess(list: List<Long>) {
        hideProgressDialog()
        val ids = list.toSet()
        tasks = tasks.filter { it.id !in ids }
        showSnackbar(R.string.common_execute_success)
    }

    private fun onTaskDeleteFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_fail)
    }

    private fun onTaskLoadSuccess(list: List<Task>, local: Boolean) {
        lastPath = vm.comic?.last
        tasks = ArrayList(list)
        if (!local) {
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    binder = service as DownloadServiceBinder
                    binder?.service?.initTask(tasks)
                    hideProgressBar()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                }
            }
            bindService(Intent(this, DownloadService::class.java), connection!!, BIND_AUTO_CREATE)
        } else {
            hideProgressBar()
            showDetailFab = false
        }
    }

    private fun onTaskLoadFail() {
        hideProgressBar()
        showDetailFab = false
        showSnackbar(R.string.task_load_task_fail)
    }

    private fun onTaskAdd(list: List<Task>) {
        tasks = ArrayList(list) + tasks
    }

    private fun onTaskError(id: Long) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        if (task.state != Task.STATE_PAUSE) {
            task.state = Task.STATE_ERROR
            notifyTasks()
        }
    }

    private fun onTaskPause(id: Long) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        task.state = Task.STATE_PAUSE
        notifyTasks()
    }

    private fun onTaskParse(id: Long) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        if (task.state != Task.STATE_PAUSE) {
            task.state = Task.STATE_PARSE
            notifyTasks()
        }
    }

    private fun onTaskProcess(id: Long, progress: Int, max: Int) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        task.max = max
        task.progress = progress
        if (task.state != Task.STATE_PAUSE) {
            task.state = if (max == progress) Task.STATE_FINISH else Task.STATE_DOING
        }
        notifyTasks()
    }

    private fun notifyTasks() {
        tasks = tasks
    }

    override fun getDefaultTitle(): String = getString(R.string.task_list)

    override fun getLayoutView(): View = binding.composeLayout

    override fun getLayoutRes(): Int = R.layout.activity_compose

    companion object {
        private const val REQUEST_CODE_DELETE = 0
        private const val DIALOG_REQUEST_OPERATION = 1
        private const val OPERATION_READ = 0
        private const val OPERATION_DELETE = 1

        @JvmStatic
        fun createIntent(context: Context, id: Long?): Intent {
            val intent = Intent(context, TaskActivity::class.java)
            intent.putExtra(Extra.EXTRA_ID, id)
            return intent
        }
    }
}

@Composable
private fun TaskScreen(
    tasks: List<Task>,
    lastPath: String?,
    showDetailFab: Boolean,
    showPlayFab: Boolean,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onDetailClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(ContextCompat.getColor(context, ThemeUtils.getResourceId(context, R.attr.colorAccent)))
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showPlayFab) {
                    FloatingActionButton(onClick = onPlayClick, backgroundColor = accent) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_white_24dp),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                if (showPlayFab && showDetailFab) {
                    Spacer(Modifier.height(16.dp))
                }
                if (showDetailFab) {
                    FloatingActionButton(onClick = onDetailClick, backgroundColor = accent) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launch_white_24dp),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(tasks, key = { it.id ?: it.path }) { task ->
                TaskRow(
                    task = task,
                    lastPath = lastPath,
                    accent = accent,
                    onClick = { onTaskClick(task) },
                    onLongClick = { onTaskLongClick(task) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: Task,
    lastPath: String?,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fraction = if (task.max > 0) task.progress.toFloat() / task.max else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .background(Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = task.title,
                color = Color.Black,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(taskStateRes(task.state)),
                    color = accent,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = StringUtils.getProgress(task.progress, task.max),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                color = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(12.dp)
            )
        }
        if (task.path == lastPath) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(accent)
            )
        }
    }
}

private fun taskStateRes(state: Int): Int {
    return when (state) {
        Task.STATE_PARSE -> R.string.task_parse
        Task.STATE_DOING -> R.string.task_doing
        Task.STATE_FINISH -> R.string.task_finish
        Task.STATE_WAIT -> R.string.task_wait
        Task.STATE_ERROR -> R.string.task_error
        else -> R.string.task_pause
    }
}
