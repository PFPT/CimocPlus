package com.haleydu.cimoc.ui.detail
import com.haleydu.cimoc.ui.common.BackActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityDetailBinding
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.fresco.ControllerBuilderSupplierFactory
import com.haleydu.cimoc.fresco.ImagePipelineFactoryBuilder
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.detail.DetailAdapter
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.reader.ChapterListHolder
import com.haleydu.cimoc.ui.reader.ReaderActivity
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class DetailActivity : BackActivity(), BaseAdapter.OnItemClickListener, BaseAdapter.OnItemLongClickListener {

    private val vm: DetailViewModel by viewModels()
    private lateinit var binding: ActivityDetailBinding
    private lateinit var detailAdapter: DetailAdapter
    private var imagePipelineFactory: ImagePipelineFactory? = null
    private var autoBackup = false
    private var backupCount = 0
    private var comicTitle: String = ""
    private var comicIntro: String = ""
    private var boundChapters: List<Chapter>? = null
    private var boundCover: String? = null
    private var loggedViewItem = false

    @Inject
    lateinit var httpClient: OkHttpClient

    private val downloadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        showProgressDialog()
        val list = ChapterListHolder.take()
            ?: result.data?.let {
                IntentCompat.getParcelableArrayListExtra(it, Extra.EXTRA_CHAPTER, Chapter::class.java)
            }
            ?: return@registerForActivityResult
        vm.addTask(detailAdapter.dateSet, list)
    }

    override fun inflateContentView(): View {
        binding = ActivityDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_detail
    }

    override fun getLayoutView(): View {
        return binding.detailLayout
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.detail)
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    override fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailAppBar) { v, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, 0)
            val recycler = binding.detailRecyclerView
            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                if (isNavTranslation) recycler.paddingBottom else bars.bottom
            )
            insets
        }
    }

    override fun initView() {
        super.initView()
        detailAdapter = DetailAdapter(this, ArrayList())
        detailAdapter.setOnItemClickListener(this)
        detailAdapter.setOnItemLongClickListener(this)
        binding.detailRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.detailRecyclerView.setHasFixedSize(false)
        binding.detailRecyclerView.itemAnimator = null
        binding.detailRecyclerView.addItemDecoration(detailAdapter.itemDecoration)
        binding.detailRecyclerView.adapter = detailAdapter
        binding.detailCover.setOnLongClickListener {
            showIntro()
            true
        }
        binding.detailIntro.setOnClickListener { showIntro() }
        binding.detailAppBar.addOnOffsetChangedListener { appBar, offset ->
            val range = appBar.totalScrollRange
            val collapsed = range > 0 && kotlin.math.abs(offset) >= range * 0.7f
            mToolbar?.title = if (collapsed && comicTitle.isNotEmpty()) comicTitle else getString(R.string.detail)
        }
    }

    override fun initData() {
        autoBackup = mPreference.getBoolean(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true)
        backupCount = mPreference.getInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, 0)
        val id = intent.getLongExtra(Extra.EXTRA_ID, -1)
        val source = intent.getIntExtra(Extra.EXTRA_SOURCE, -1)
        val cid = intent.getStringExtra(Extra.EXTRA_CID)
        val title = intent.getStringExtra(Extra.EXTRA_TITLE)
        val cover = intent.getStringExtra(Extra.EXTRA_COVER)
        val author = intent.getStringExtra(Extra.EXTRA_AUTHOR)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.refreshFromUpdate()
                vm.uiState.collect { onUiState(it) }
            }
        }
        vm.events.collectOnStart(this) { event ->
            when (event) {
                is DetailViewModel.Event.ParseError -> onParseError()
                is DetailViewModel.Event.NetworkError -> onNetworkError()
                is DetailViewModel.Event.NetworkLoadSuccess -> onNetworkLoadSuccess()
                is DetailViewModel.Event.TaskAddSuccess -> onTaskAddSuccess(event.list)
                is DetailViewModel.Event.TaskAddFail -> onTaskAddFail()
            }
        }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE).collectOnStart(this) { vm.refreshFromUpdate() }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE_INFO).collectOnStart(this) { event ->
            (event.data as? Comic)?.let { vm.applyUpdateInfo(it) }
        }
        vm.load(id, source, cid, title, cover, author)
    }

    override fun onPause() {
        super.onPause()
        if (autoBackup) {
            mPreference.putInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, backupCount)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        boundChapters = null
        boundCover = null
        imagePipelineFactory?.imagePipeline?.clearMemoryCaches()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        val comic = vm.comic
        menu.findItem(R.id.detail_favorite)?.setIcon(
            if (comic?.favorite != null) R.drawable.ic_favorite_white_24dp
            else R.drawable.ic_favorite_border_white_24dp
        )
        val hasHistory = !comic?.last.isNullOrEmpty()
        menu.findItem(R.id.detail_read)?.setIcon(
            if (hasHistory) R.drawable.ic_history_white_24dp else R.drawable.ic_play_arrow_white_24dp
        )
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (isProgressBarShown) return super.onOptionsItemSelected(item)
        val comic = vm.comic ?: return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.detail_download -> {
                if (detailAdapter.dateSet.isNotEmpty()) {
                    val intent = ChapterActivity.createIntent(this, ArrayList(detailAdapter.dateSet))
                    downloadLauncher.launch(intent)
                }
            }
            R.id.detail_tag -> {
                if (comic.favorite != null) {
                    startActivity(TagEditorActivity.createIntent(this, comic.id))
                } else {
                    showSnackbar(R.string.detail_tag_favorite)
                }
            }
            R.id.detail_search_title -> searchKeyword(comic.title, "byTitle", comic.source)
            R.id.detail_search_author -> searchKeyword(comic.author, null, null)
            R.id.detail_share_url -> {
                val url = comic.url
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, url)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(Intent.createChooser(intent, url))
                if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
                    val bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.CONTENT, url)
                    bundle.putInt(FirebaseAnalytics.Param.SOURCE, comic.source)
                    FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SHARE, bundle)
                }
            }
            R.id.detail_reverse_list -> vm.toggleReverse()
            R.id.detail_favorite -> onFavoriteClick()
            R.id.detail_read -> onReadClick()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val chapter = detailAdapter.getItem(position) ?: return
        startReader(chapter.path)
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        return false
    }

    private fun onReadClick() {
        if (detailAdapter.dateSet.isEmpty()) return
        val path = vm.comic?.last ?: run {
            val index = if (vm.isAscend) 0 else detailAdapter.dateSet.size - 1
            detailAdapter.getItem(index).path
        }
        startReader(path)
    }

    private fun onFavoriteClick() {
        val comic = vm.comic ?: return
        if (comic.favorite != null) {
            vm.unfavoriteComic()
            increment()
            invalidateOptionsMenu()
            showSnackbar(R.string.detail_unfavorite)
        } else {
            vm.favoriteComic()
            increment()
            invalidateOptionsMenu()
            showSnackbar(R.string.detail_favorite)
        }
    }

    private fun startReader(path: String) {
        val id = vm.updateLast(path)
        detailAdapter.setLast(path)
        val mode = mPreference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE)
        startActivity(ReaderActivity.createIntent(this, id, ArrayList(detailAdapter.dateSet), mode))
    }

    private fun onTaskAddSuccess(list: ArrayList<Task>) {
        val intent = DownloadService.createIntent(this, list)
        DownloadService.start(this, intent)
        val paths = list.map { it.path }.toHashSet()
        for (chapter in detailAdapter.dateSet) {
            if (paths.contains(chapter.path)) {
                chapter.download = true
            }
        }
        showSnackbar(R.string.detail_download_queue_success)
        hideProgressDialog()
    }

    private fun onTaskAddFail() {
        hideProgressDialog()
        showSnackbar(R.string.detail_download_queue_fail)
    }

    private fun onUiState(state: DetailViewModel.UiState) {
        state.comic?.let { bindComic(it) }
        if (boundChapters != state.chapters) {
            boundChapters = state.chapters
            detailAdapter.setData(state.chapters)
        }
        detailAdapter.setLast(state.last)
        if (state.chaptersReady) {
            hideProgressBar()
        }
    }

    private fun onNetworkLoadSuccess() {
        hideProgressBar()
        logViewItem(true)
    }

    private fun logViewItem(success: Boolean) {
        if (loggedViewItem || !mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            return
        }
        loggedViewItem = true
        val comic = vm.comic
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT, comic?.title)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Title")
        bundle.putInt(FirebaseAnalytics.Param.SOURCE, comic?.source ?: -1)
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, success)
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
    }

    private fun bindComic(comic: Comic) {
        comicTitle = comic.title.orEmpty()
        comicIntro = comic.intro.orEmpty()
        binding.detailTitle.text = comic.title
        binding.detailAuthor.text = comic.author
        binding.detailIntro.text = comic.intro
        if (comic.finish != null) {
            binding.detailStatus.setText(
                if (comic.finish) R.string.comic_status_finish else R.string.comic_status_continue
            )
        }
        if (!comic.update.isNullOrEmpty()) {
            binding.detailUpdate.text = getString(R.string.detail_last_update, comic.update)
        }
        if (comic.title != null && comic.cover != null) {
            if (boundCover != comic.cover) {
                boundCover = comic.cover
                imagePipelineFactory = ImagePipelineFactoryBuilder.build(this, vm.parserHeader(), false, httpClient)
                val supplier = ControllerBuilderSupplierFactory.get(this, imagePipelineFactory)
                binding.detailCover.controller = supplier.get().setUri(comic.cover).build()
            }
            invalidateOptionsMenu()
        }
    }

    private fun onParseError() {
        logViewItem(false)
        hideProgressBar()
        showSnackbar(R.string.common_parse_error)
    }

    private fun onNetworkError() {
        hideProgressBar()
        showSnackbar(R.string.common_network_error)
    }

    private fun searchKeyword(keyword: String?, contentType: String?, source: Int?) {
        if (StringUtils.isEmpty(keyword)) {
            showSnackbar(R.string.common_keyword_empty)
            return
        }
        if (contentType != null && mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CONTENT, keyword)
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            if (source != null) {
                bundle.putInt(FirebaseAnalytics.Param.SOURCE, source)
            }
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
        startActivity(
            ResultActivity.createIntent(
                this,
                keyword,
                true,
                vm.enabledSourceTypes(),
                ResultActivity.LAUNCH_MODE_SEARCH
            )
        )
    }

    private fun showIntro() {
        if (comicIntro.isEmpty() && comicTitle.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(comicTitle)
            .setMessage(comicIntro)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    private fun increment() {
        if (autoBackup && ++backupCount == 10) {
            backupCount = 0
            mPreference.putInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, 0)
            vm.backup()
        }
    }

    companion object {
        const val REQUEST_CODE_DOWNLOAD = 0

        @JvmStatic
        @JvmOverloads
        fun createIntent(
            context: Context,
            id: Long?,
            source: Int,
            cid: String?,
            title: String? = null,
            cover: String? = null,
            author: String? = null
        ): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(Extra.EXTRA_ID, id ?: -1L)
            intent.putExtra(Extra.EXTRA_SOURCE, source)
            intent.putExtra(Extra.EXTRA_CID, cid)
            intent.putExtra(Extra.EXTRA_TITLE, title)
            intent.putExtra(Extra.EXTRA_COVER, cover)
            intent.putExtra(Extra.EXTRA_AUTHOR, author)
            return intent
        }
    }
}
