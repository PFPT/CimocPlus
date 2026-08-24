package com.haleydu.cimoc.ui.search
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.firebase.analytics.FirebaseAnalytics
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivityResultBinding
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.search.ResultAdapter
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.ItemDialogFragment
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.ui.detail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class ResultActivity : BackActivity(), BaseAdapter.OnItemClickListener, DialogCaller {

    private val vm: ResultViewModel by viewModels()
    private lateinit var binding: ActivityResultBinding
    private lateinit var adapter: ResultAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var provider: ControllerBuilderProvider? = null
    private var type: Int = -1
    private var lastFilters: List<Int> = emptyList()
    private var pendingGroup: ResultViewModel.SearchGroup? = null

    @Inject
    lateinit var httpClient: OkHttpClient

    override fun initViewModel() {
        val keyword = intent.getStringExtra(Extra.EXTRA_KEYWORD)
        val source = intent.getIntArrayExtra(Extra.EXTRA_SOURCE_LIST)
        val strictSearch = intent.getBooleanExtra(Extra.EXTRA_STRICT, true)
        vm.setup(source, keyword, strictSearch)
    }

    override fun inflateContentView(): View {
        binding = ActivityResultBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        layoutManager = LinearLayoutManager(this)
        adapter = ResultAdapter(this, ArrayList())
        adapter.setOnItemClickListener(this)
        provider = ControllerBuilderProvider(this, vm.headerGetter(), true, httpClient)
        adapter.setProvider(provider!!)
        adapter.setTitleGetter(vm.titleGetter())
        binding.resultRecyclerView.setHasFixedSize(true)
        binding.resultRecyclerView.layoutManager = layoutManager
        binding.resultRecyclerView.addItemDecoration(adapter.itemDecoration)
        binding.resultRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 4 && dy > 0) {
                    load()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> provider?.pause()
                    RecyclerView.SCROLL_STATE_IDLE -> provider?.resume()
                }
            }
        })
        binding.resultRecyclerView.adapter = adapter
        binding.resultFilterChips.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            val chip = binding.resultFilterChips.findViewById<Chip>(checkedId) ?: return@setOnCheckedChangeListener
            val source = chip.tag as? Int ?: ResultViewModel.FILTER_ALL
            vm.setFilter(source)
        }
    }

    override fun initData() {
        type = intent.getIntExtra(Extra.EXTRA_MODE, -1)
        vm.items.collectOnStart(this) { onItems(it) }
        vm.filterSources.collectOnStart(this) { onFilters(it) }
        vm.searchError.collectOnStart(this) { onSearchError() }
        vm.loadFail.collectOnStart(this) { onLoadFail() }
        vm.loadNetworkError.collectOnStart(this) { onLoadNetworkError() }
        vm.loadEmpty.collectOnStart(this) { onLoadEmpty() }
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        provider?.clear()
    }

    override fun onItemClick(view: View, position: Int) {
        val group = adapter.getItem(position)
        val comics = group.comics
        if (comics.size == 1) {
            openDetail(comics[0])
            return
        }
        pendingGroup = group
        val titles = Array(comics.size) { vm.titleGetter().getTitle(comics[it].source) }
        ItemDialogFragment.newInstance(R.string.result_source_pick, titles, DIALOG_SOURCE)
            .show(supportFragmentManager, null)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_SOURCE) {
            val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
            val comic = pendingGroup?.comics?.getOrNull(index) ?: return
            openDetail(comic)
        }
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.result)
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_result
    }

    override fun getLayoutView(): View {
        return binding.resultLayout
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    private fun load() {
        when (type) {
            LAUNCH_MODE_SEARCH -> vm.dispatch(ResultViewModel.SearchIntent.LoadMore)
            LAUNCH_MODE_CATEGORY -> vm.loadCategory()
        }
    }

    private fun onItems(list: List<ResultViewModel.SearchGroup>) {
        if (list.isNotEmpty()) {
            hideProgressBar()
        }
        adapter.setData(list)
    }

    private fun onFilters(types: List<Int>) {
        if (type != LAUNCH_MODE_SEARCH || types.size < 2) {
            binding.resultFilterScroll.visibility = View.GONE
            return
        }
        if (types == lastFilters) return
        lastFilters = types
        binding.resultFilterScroll.visibility = View.VISIBLE
        val chips = binding.resultFilterChips
        chips.removeAllViews()
        chips.addView(filterChip(getString(R.string.result_filter_all), ResultViewModel.FILTER_ALL, true, false))
        for (source in types) {
            chips.addView(filterChip(vm.titleGetter().getTitle(source), source, false, vm.isInvalid(source)))
        }
    }

    private fun filterChip(text: String, source: Int, checked: Boolean, invalid: Boolean): Chip {
        val chip = Chip(this)
        val drawable = ChipDrawable.createFromAttributes(
            this,
            null,
            0,
            R.style.Widget_Material3_Chip_Filter
        )
        chip.setChipDrawable(drawable)
        chip.layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        chip.text = if (invalid) text + " · " + getString(R.string.source_invalid) else text
        chip.alpha = if (invalid) 0.55f else 1f
        chip.isCheckable = true
        chip.isChecked = checked
        chip.tag = source
        chip.id = View.generateViewId()
        return chip
    }

    private fun openDetail(comic: Comic) {
        startActivity(
            DetailActivity.createIntent(
                this,
                null,
                comic.source,
                comic.cid,
                comic.title,
                comic.cover,
                comic.author
            )
        )
    }

    private fun onLoadFail() {
        hideProgressBar()
        showSnackbar(R.string.common_parse_error)
    }

    private fun onLoadNetworkError() {
        hideProgressBar()
        showSnackbar(R.string.common_network_error)
    }

    private fun onLoadEmpty() {
        hideProgressBar()
        showSnackbar(R.string.result_empty)
    }

    private fun onSearchError() {
        hideProgressBar()
        showSnackbar(R.string.result_empty)
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CHARACTER, intent.getStringExtra(Extra.EXTRA_KEYWORD))
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "bySearch")
            bundle.putString(FirebaseAnalytics.Param.CONTENT, getString(R.string.result_empty))
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false)
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }

    companion object {
        const val LAUNCH_MODE_SEARCH = 0
        const val LAUNCH_MODE_CATEGORY = 1
        private const val DIALOG_SOURCE = 0

        @JvmField
        val searchUrls = SparseArray<String>()

        @JvmStatic
        fun createIntent(context: Context, keyword: String?, source: Int, type: Int): Intent {
            return createIntent(context, keyword, intArrayOf(source), type)
        }

        @JvmStatic
        fun createIntent(context: Context, keyword: String?, array: IntArray?, type: Int): Intent {
            val intent = Intent(context, ResultActivity::class.java)
            intent.putExtra(Extra.EXTRA_MODE, type)
            intent.putExtra(Extra.EXTRA_SOURCE_LIST, array)
            intent.putExtra(Extra.EXTRA_KEYWORD, keyword)
            return intent
        }

        @JvmStatic
        fun createIntent(
            context: Context,
            keyword: String?,
            strictSearch: Boolean,
            array: IntArray?,
            type: Int
        ): Intent {
            val intent = createIntent(context, keyword, array, type)
            intent.putExtra(Extra.EXTRA_STRICT, strictSearch)
            return intent
        }
    }
}
