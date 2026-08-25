package com.haleydu.cimoc.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.ui.reader.ChapterListHolder
import com.haleydu.cimoc.ui.reader.ReaderActivity
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import com.haleydu.cimoc.utils.interpretationUtils
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class DetailFragment : BaseFragment(), BaseAdapter.OnItemClickListener, BaseAdapter.OnItemLongClickListener {

    private val vm: DetailViewModel by viewModels()
    private var binding: ActivityDetailBinding? = null
    private lateinit var detailAdapter: DetailAdapter
    private var imagePipelineFactory: ImagePipelineFactory? = null
    private var autoBackup = false
    private var backupCount = 0
    private var comicTitle = ""
    private var comicIntro = ""

    @Inject
    lateinit var httpClient: OkHttpClient

    private val downloadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        showProgressDialog()
        val list = ChapterListHolder.take()
            ?: result.data?.let {
                IntentCompat.getParcelableArrayListExtra(it, Extra.EXTRA_CHAPTER, Chapter::class.java)
            }
            ?: return@registerForActivityResult
        vm.addTask(detailAdapter.dateSet, list)
    }

    override fun getLayoutRes(): Int = R.layout.activity_detail

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = ActivityDetailBinding.inflate(inflater, container, false)
        binding = view
        bindViews(view.root)
        mPreference = com.haleydu.cimoc.di.AppEntryPoint::class.java.let {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                requireActivity().applicationContext,
                it
            ).preferenceManager()
        }
        initViewModel()
        initView()
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding?.detailCollapsing?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.custom_toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        val host: MenuHost = requireActivity()
        host.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return onDetailMenu(menuItem)
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun initView() {
        val bind = binding ?: return
        detailAdapter = DetailAdapter(requireContext(), ArrayList())
        detailAdapter.setOnItemClickListener(this)
        detailAdapter.setOnItemLongClickListener(this)
        bind.detailRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        bind.detailRecyclerView.setHasFixedSize(false)
        bind.detailRecyclerView.itemAnimator = null
        bind.detailRecyclerView.addItemDecoration(detailAdapter.itemDecoration)
        bind.detailRecyclerView.adapter = detailAdapter
        bind.detailFabRead.setOnClickListener { onReadClick() }
        bind.detailFabFavorite.setOnClickListener { onFavoriteClick() }
        bind.detailCover.transitionName = "comic_cover"
        bind.detailCover.setOnLongClickListener {
            showIntro()
            true
        }
        bind.detailIntro.setOnClickListener { showIntro() }
    }

    override fun initData() {
        autoBackup = mPreference.getBoolean(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true)
        backupCount = mPreference.getInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, 0)
        val args = arguments
        val id = args?.let {
            if (it.containsKey(Extra.EXTRA_ID)) it.getLong(Extra.EXTRA_ID, -1L) else it.getLong("id", -1L)
        } ?: -1L
        val source = args.intArg(Extra.EXTRA_SOURCE, "source")
        val cid = args?.getString(Extra.EXTRA_CID) ?: args?.getString("cid")
        val title = args?.getString(Extra.EXTRA_TITLE)
        val cover = args?.getString(Extra.EXTRA_COVER)
        val author = args?.getString(Extra.EXTRA_AUTHOR)
        if (id == -1L && cid.isNullOrEmpty()) {
            hideProgressBar()
            HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.common_parse_error))
            return
        }
        vm.events.collectOnStart(viewLifecycleOwner) { event ->
            when (event) {
                is DetailViewModel.Event.PreLoad -> onPreLoadSuccess(event.list, event.comic)
                is DetailViewModel.Event.ComicLoaded -> onComicLoadSuccess(event.comic)
                is DetailViewModel.Event.ChapterLoaded -> onChapterLoadSuccess(event.list)
                is DetailViewModel.Event.ParseError -> {
                    hideProgressBar()
                    HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.common_parse_error))
                }
                is DetailViewModel.Event.NetworkError -> {
                    hideProgressBar()
                    HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.common_network_error))
                }
                is DetailViewModel.Event.TaskAddSuccess -> onTaskAddSuccess(event.list)
                is DetailViewModel.Event.TaskAddFail -> {
                    hideProgressDialog()
                    HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.detail_download_queue_fail))
                }
                is DetailViewModel.Event.LastChange -> detailAdapter.setLast(event.last)
            }
        }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE).collectOnStart(viewLifecycleOwner) { vm.refreshFromUpdate() }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE_INFO).collectOnStart(viewLifecycleOwner) { event ->
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

    override fun onDestroyView() {
        imagePipelineFactory?.imagePipeline?.clearMemoryCaches()
        binding = null
        super.onDestroyView()
    }

    override fun onItemClick(view: View, position: Int) {
        val chapter = detailAdapter.getItem(position) ?: return
        startReader(chapter.path)
    }

    override fun onItemLongClick(view: View, position: Int): Boolean = false

    private fun onDetailMenu(item: MenuItem): Boolean {
        val comic = vm.comic ?: return false
        when (item.itemId) {
            R.id.detail_download -> {
                if (detailAdapter.dateSet.isNotEmpty()) {
                    val intent = ChapterActivity.createIntent(requireActivity(), ArrayList(detailAdapter.dateSet))
                    downloadLauncher.launch(intent)
                }
            }
            R.id.detail_tag -> {
                if (comic.favorite != null) {
                    startActivity(TagEditorActivity.createIntent(requireActivity(), comic.id))
                } else {
                    HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.detail_tag_favorite))
                }
            }
            R.id.detail_search_title -> searchKeyword(comic.title)
            R.id.detail_search_author -> searchKeyword(comic.author)
            R.id.detail_share_url -> {
                val url = comic.url
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, url)
                startActivity(Intent.createChooser(intent, url))
            }
            R.id.detail_reverse_list -> detailAdapter.reverse()
            else -> return false
        }
        return true
    }

    private fun onReadClick() {
        if (detailAdapter.dateSet.isEmpty()) return
        val path = vm.comic?.last ?: detailAdapter.getItem(detailAdapter.dateSet.size - 1).path
        startReader(path)
    }

    private fun onFavoriteClick() {
        val comic = vm.comic ?: return
        val bind = binding ?: return
        if (comic.favorite != null) {
            vm.unfavoriteComic()
            bind.detailFabFavorite.setImageResource(R.drawable.ic_favorite_border_white_24dp)
            HintUtils.showSnackbar(bind.detailLayout, getString(R.string.detail_unfavorite))
        } else {
            vm.favoriteComic()
            bind.detailFabFavorite.setImageResource(R.drawable.ic_favorite_white_24dp)
            HintUtils.showSnackbar(bind.detailLayout, getString(R.string.detail_favorite))
        }
    }

    private fun startReader(path: String) {
        val id = vm.updateLast(path)
        detailAdapter.setLast(path)
        val mode = mPreference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE)
        startActivity(ReaderActivity.createIntent(requireActivity(), id, ArrayList(detailAdapter.dateSet), mode))
    }

    private fun onTaskAddSuccess(list: ArrayList<Task>) {
        val intent = DownloadService.createIntent(requireActivity(), list)
        DownloadService.start(requireActivity(), intent)
        val paths = list.map { it.path }.toHashSet()
        for (chapter in detailAdapter.dateSet) {
            if (paths.contains(chapter.path)) chapter.download = true
        }
        HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.detail_download_queue_success))
        hideProgressDialog()
    }

    private fun onComicLoadSuccess(comic: Comic) {
        bindComic(comic)
    }

    private fun onChapterLoadSuccess(list: List<Chapter>) {
        hideProgressBar()
        detailAdapter.clear()
        detailAdapter.addAll(list)
    }

    private fun onPreLoadSuccess(list: List<Chapter>, comic: Comic) {
        hideProgressBar()
        val chapters = if (interpretationUtils.isReverseOrder(comic)) list.reversed() else list
        detailAdapter.addAll(chapters)
        bindComic(comic)
    }

    private fun bindComic(comic: Comic) {
        val bind = binding ?: return
        comicTitle = comic.title.orEmpty()
        comicIntro = comic.intro.orEmpty()
        bind.detailTitle.text = comic.title
        (requireActivity() as AppCompatActivity).supportActionBar?.title = comic.title
        bind.detailAuthor.text = comic.author
        bind.detailIntro.text = comic.intro
        if (comic.finish != null) {
            bind.detailStatus.setText(
                if (comic.finish) R.string.comic_status_finish else R.string.comic_status_continue
            )
        }
        if (!comic.update.isNullOrEmpty()) {
            bind.detailUpdate.text = getString(R.string.detail_last_update, comic.update)
        }
        detailAdapter.setLast(comic.last)
        if (comic.title != null && comic.cover != null) {
            imagePipelineFactory = ImagePipelineFactoryBuilder.build(requireContext(), vm.parserHeader(), false, httpClient)
            val supplier = ControllerBuilderSupplierFactory.get(requireContext(), imagePipelineFactory)
            bind.detailCover.controller = supplier.get().setUri(comic.cover).build()
            bind.detailFabFavorite.setImageResource(
                if (comic.favorite != null) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
            )
            bind.detailFabFavorite.visibility = View.VISIBLE
            val hasHistory = !comic.last.isNullOrEmpty()
            bind.detailFabRead.setImageResource(
                if (hasHistory) R.drawable.ic_history_white_24dp else R.drawable.ic_play_arrow_white_24dp
            )
            bind.detailFabRead.visibility = View.VISIBLE
        }
    }

    private fun searchKeyword(keyword: String?) {
        if (StringUtils.isEmpty(keyword)) {
            HintUtils.showSnackbar(binding?.detailLayout, getString(R.string.common_keyword_empty))
            return
        }
        val sources = vm.enabledSourceTypes()
        val activity = activity
        if (activity is MainActivity) {
            activity.openResult(keyword, sources, true, ResultActivity.LAUNCH_MODE_SEARCH)
        } else {
            startActivity(
                ResultActivity.createIntent(
                    requireActivity(),
                    keyword,
                    true,
                    sources,
                    ResultActivity.LAUNCH_MODE_SEARCH
                )
            )
        }
    }

    private fun showIntro() {
        if (comicIntro.isEmpty() && comicTitle.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle(comicTitle)
            .setMessage(comicIntro)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    companion object {
        fun newInstance(
            id: Long?,
            source: Int,
            cid: String?,
            title: String? = null,
            cover: String? = null,
            author: String? = null
        ): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = bundleOf(
                Extra.EXTRA_ID to (id ?: -1L),
                Extra.EXTRA_SOURCE to source,
                Extra.EXTRA_CID to cid,
                Extra.EXTRA_TITLE to title,
                Extra.EXTRA_COVER to cover,
                Extra.EXTRA_AUTHOR to author
            )
            return fragment
        }

        private fun Bundle?.intArg(key: String, fallbackKey: String): Int {
            this ?: return -1
            return when {
                containsKey(key) -> getInt(key, -1)
                containsKey(fallbackKey) -> getInt(fallbackKey, -1)
                else -> -1
            }
        }
    }
}
