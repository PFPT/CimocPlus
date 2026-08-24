package com.haleydu.cimoc.ui.explore
import com.haleydu.cimoc.ui.common.BaseFragment
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.databinding.FragmentExploreBinding
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category
import com.haleydu.cimoc.ui.detail.DetailActivity
import com.haleydu.cimoc.ui.explore.ExploreViewModel
import com.haleydu.cimoc.ui.search.SearchActivity
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.explore.CategoryAdapter
import com.haleydu.cimoc.ui.explore.ExploreLoadAdapter
import com.haleydu.cimoc.ui.common.GridAdapter
import com.haleydu.cimoc.ui.explore.RecommendAdapter
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment : BaseFragment(), BaseAdapter.OnItemClickListener, ThemeResponsive {

    @Inject
    lateinit var httpClient: OkHttpClient

    private val vm: ExploreViewModel by viewModels()
    private var binding: FragmentExploreBinding? = null
    private lateinit var gridAdapter: GridAdapter
    private lateinit var recommendAdapter: RecommendAdapter
    private lateinit var loadAdapter: ExploreLoadAdapter
    private lateinit var concatAdapter: ConcatAdapter
    private lateinit var sourceAdapter: ArrayAdapter<String>
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var provider: ControllerBuilderProvider

    private lateinit var filterGroups: List<View>
    private lateinit var filterSpinners: List<AppCompatSpinner>

    private var updatingFilters = true

    override fun getLayoutRes(): Int {
        return R.layout.fragment_explore
    }

    override fun bindViews(view: View) {
        super.bindViews(view)
        binding = FragmentExploreBinding.bind(view)
    }

    override fun initView() {
        setHasOptionsMenu(true)
        val binding = binding ?: return
        filterGroups = listOf(
            binding.exploreSubject,
            binding.exploreArea,
            binding.exploreReader,
            binding.exploreYear,
            binding.exploreProgress,
            binding.exploreOrder
        )
        filterSpinners = listOf(
            binding.exploreSpinnerSubject,
            binding.exploreSpinnerArea,
            binding.exploreSpinnerReader,
            binding.exploreSpinnerYear,
            binding.exploreSpinnerProgress,
            binding.exploreSpinnerOrder
        )

        sourceAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner, mutableListOf())
        sourceAdapter.setDropDownViewResource(R.layout.item_spinner)
        binding.exploreSourceSpinner.adapter = sourceAdapter
        binding.exploreSourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingFilters) return
                val source = vm.sources.value.getOrNull(position) ?: return
                vm.selectSource(source.type)
                updatingFilters = true
                bindFilters(vm.category())
                updatingFilters = false
                applyCurrentFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingFilters || parent == null) return
                val category = vm.category()
                if (category != null && !category.isComposite) {
                    for (spinner in filterSpinners) {
                        spinner.isEnabled = position == 0 || parent === spinner
                    }
                }
                applyCurrentFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        for (spinner in filterSpinners) {
            spinner.onItemSelectedListener = filterListener
        }

        gridAdapter = GridAdapter(requireActivity())
        gridAdapter.setOnItemClickListener(this)
        recommendAdapter = RecommendAdapter(requireContext()) { comic ->
            openComic(comic.source, comic.cid, null)
        }
        loadAdapter = ExploreLoadAdapter()
        provider = ControllerBuilderProvider(requireContext(), vm.headerGetter(), true, httpClient)
        gridAdapter.setProvider(provider)
        gridAdapter.setTitleGetter(vm.titleGetter())
        recommendAdapter.setProvider(provider)
        recommendAdapter.setTitleGetter(vm.titleGetter())
        concatAdapter = ConcatAdapter(
            ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build(),
            recommendAdapter,
            gridAdapter,
            loadAdapter
        )
        layoutManager = GridLayoutManager(requireContext(), 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val header = recommendAdapter.itemCount
                val grid = gridAdapter.itemCount
                return if (position < header || position >= header + grid) 3 else 1
            }
        }
        layoutManager.spanSizeLookup.isSpanIndexCacheEnabled = false
        binding.exploreRecyclerView.layoutManager = layoutManager
        binding.exploreRecyclerView.setHasFixedSize(true)
        binding.exploreRecyclerView.itemAnimator = null
        binding.exploreRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) {
                    outRect.set(0, 0, 0, 0)
                    return
                }
                val header = recommendAdapter.itemCount
                val grid = gridAdapter.itemCount
                if (position < header || position >= header + grid) {
                    outRect.set(0, 0, 0, 0)
                    return
                }
                val offset = parent.width / 90
                outRect.set(offset, 0, offset, (2.8 * offset).toInt())
            }
        })
        binding.exploreRecyclerView.setRecycledViewPool(appInstance.gridRecycledPool)
        binding.exploreRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() >= concatAdapter.itemCount - 4 && dy > 0) {
                    vm.loadMore()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> provider.pause()
                    RecyclerView.SCROLL_STATE_IDLE -> provider.resume()
                }
            }
        })
        binding.exploreRecyclerView.adapter = concatAdapter
        binding.exploreGoSearch.setOnClickListener { openSearch() }
    }

    override fun initData() {
        vm.sources.collectOnStart(viewLifecycleOwner) { bindSources(it) }
        vm.comics.collectOnStart(viewLifecycleOwner) { comics ->
            recommendAdapter.setComics(comics.mapNotNull { it as? MiniComic })
            gridAdapter.submitList(comics.mapNotNull { it as? MiniComic })
            updateFooterAndProgress()
        }
        vm.loading.collectOnStart(viewLifecycleOwner) { updateFooterAndProgress() }
        vm.ended.collectOnStart(viewLifecycleOwner) { updateFooterAndProgress() }
        vm.unsupported.collectOnStart(viewLifecycleOwner) { unsupported ->
            val binding = binding ?: return@collectOnStart
            binding.exploreUnsupported.visibility = if (unsupported) View.VISIBLE else View.GONE
            binding.exploreRecyclerView.visibility = if (unsupported) View.GONE else View.VISIBLE
            updateFooterAndProgress()
        }
        vm.error.collectOnStart(viewLifecycleOwner) { error ->
            val layout = binding?.exploreLayout
            when (error) {
                ExploreViewModel.Error.NETWORK -> HintUtils.showSnackbar(layout, getString(R.string.common_network_error))
                ExploreViewModel.Error.PARSE -> HintUtils.showSnackbar(layout, getString(R.string.common_parse_error))
                ExploreViewModel.Error.EMPTY -> HintUtils.showSnackbar(layout, getString(R.string.result_empty))
            }
        }
        val source = arguments?.getInt(Extra.EXTRA_SOURCE, -1) ?: -1
        vm.setup(source)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_explore, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.explore_search) {
            openSearch()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::provider.isInitialized) {
            provider.clear()
        }
        binding = null
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = gridAdapter.comicAt(position)
        openComic(comic.source, comic.cid, view)
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
    }

    fun openSearch() {
        val type = vm.currentType.value
        val title = vm.sources.value.find { it.type == type }?.title ?: ""
        startActivity(SearchActivity.createIntent(requireActivity(), type, title))
    }

    private fun openComic(source: Int, cid: String?, view: View?) {
        val host = activity
        if (host is com.haleydu.cimoc.ui.main.MainActivity) {
            host.openDetail(null, source, cid, view)
        } else {
            startActivity(DetailActivity.createIntent(requireActivity(), null, source, cid))
        }
    }

    private fun bindSources(list: List<Source>) {
        val binding = binding ?: return
        updatingFilters = true
        sourceAdapter.clear()
        sourceAdapter.addAll(list.map { it.title })
        val type = vm.currentType.value
        val index = list.indexOfFirst { it.type == type }.let { if (it < 0) 0 else it }
        if (list.isNotEmpty()) {
            binding.exploreSourceSpinner.setSelection(index, false)
        }
        bindFilters(vm.category())
        updatingFilters = false
        applyCurrentFilters()
    }

    private fun bindFilters(category: Category?) {
        val binding = binding ?: return
        var anyVisible = false
        for (i in FILTER_TYPES.indices) {
            val group = filterGroups[i]
            val spinner = filterSpinners[i]
            spinner.isEnabled = true
            val attr = FILTER_TYPES[i]
            val list = if (category != null && category.hasAttribute(attr)) {
                category.getAttrList(attr)
            } else {
                null
            }
            if (list.isNullOrEmpty()) {
                group.visibility = View.GONE
            } else {
                group.visibility = View.VISIBLE
                spinner.adapter = CategoryAdapter(requireContext(), list)
                anyVisible = true
            }
        }
        binding.exploreFilterScroll.visibility = if (anyVisible) View.VISIBLE else View.GONE
    }

    private fun applyCurrentFilters() {
        if (vm.currentType.value < 0) return
        val args = Array(filterSpinners.size) { i -> spinnerValue(filterSpinners[i]) }
        vm.applyFilters(args)
    }

    private fun spinnerValue(spinner: AppCompatSpinner): String? {
        val group = spinner.parent as? View ?: return null
        if (group.visibility != View.VISIBLE) return null
        val adapter = spinner.adapter as? CategoryAdapter ?: return null
        val position = spinner.selectedItemPosition
        if (position < 0) return null
        return adapter.getValue(position)
    }

    private fun updateFooterAndProgress() {
        val comics = vm.comics.value
        val loading = vm.loading.value
        val ended = vm.ended.value
        loadAdapter.state = when {
            comics.isEmpty() -> ExploreLoadAdapter.State.HIDDEN
            loading -> ExploreLoadAdapter.State.LOADING
            ended -> ExploreLoadAdapter.State.END
            else -> ExploreLoadAdapter.State.HIDDEN
        }
        val firstLoad = loading && comics.isEmpty() && !vm.unsupported.value
        if (firstLoad) {
            mProgressBar?.visibility = View.VISIBLE
        } else {
            hideProgressBar()
        }
    }

    companion object {
        private val FILTER_TYPES = intArrayOf(
            Category.CATEGORY_SUBJECT,
            Category.CATEGORY_AREA,
            Category.CATEGORY_READER,
            Category.CATEGORY_YEAR,
            Category.CATEGORY_PROGRESS,
            Category.CATEGORY_ORDER
        )

        fun newInstance(source: Int): ExploreFragment {
            val fragment = ExploreFragment()
            val args = Bundle()
            args.putInt(Extra.EXTRA_SOURCE, source)
            fragment.arguments = args
            return fragment
        }
    }
}
