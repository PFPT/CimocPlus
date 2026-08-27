package com.haleydu.cimoc.ui.search

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.ui.common.dialog.ItemDialogFragment
import com.haleydu.cimoc.ui.common.dialog.showForCaller
import com.haleydu.cimoc.ui.detail.DetailActivity
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class ResultFragment : BaseFragment(), BaseAdapter.OnItemClickListener, DialogCaller {

    private val vm: ResultViewModel by viewModels()
    private var binding: ActivityResultBinding? = null
    private lateinit var adapter: ResultAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var provider: ControllerBuilderProvider? = null
    private var type: Int = -1
    private var lastFilters: List<Int> = emptyList()
    private var pendingGroup: ResultViewModel.SearchGroup? = null

    @Inject
    lateinit var httpClient: OkHttpClient

    override fun getLayoutRes(): Int = R.layout.activity_result

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = ActivityResultBinding.inflate(inflater, container, false)
        binding = view
        bindViews(view.root)
        mPreference = dagger.hilt.android.EntryPointAccessors.fromApplication(
            requireActivity().applicationContext,
            com.haleydu.cimoc.di.AppEntryPoint::class.java
        ).preferenceManager()
        val keyword = arguments?.getString(Extra.EXTRA_KEYWORD)
        val source = arguments?.getIntArray(Extra.EXTRA_SOURCE_LIST)
        val strictSearch = arguments?.getBoolean(Extra.EXTRA_STRICT, true) ?: true
        vm.setup(source, keyword, strictSearch)
        initView()
        return view.root
    }

    override fun initView() {
        val bind = binding ?: return
        layoutManager = LinearLayoutManager(requireContext())
        adapter = ResultAdapter(requireContext(), ArrayList())
        adapter.setOnItemClickListener(this)
        provider = ControllerBuilderProvider(requireContext(), vm.headerGetter(), true, httpClient)
        adapter.setProvider(provider!!)
        adapter.setTitleGetter(vm.titleGetter())
        bind.resultRecyclerView.setHasFixedSize(true)
        bind.resultRecyclerView.layoutManager = layoutManager
        bind.resultRecyclerView.addItemDecoration(adapter.itemDecoration)
        bind.resultRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 4 && dy > 0) {
                    load()
                }
            }
        })
        bind.resultRecyclerView.adapter = adapter
        bind.resultFilterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: View.NO_ID
            if (checkedId == View.NO_ID) return@setOnCheckedStateChangeListener
            val chip = bind.resultFilterChips.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
            val source = chip.tag as? Int ?: ResultViewModel.FILTER_ALL
            vm.setFilter(source)
        }
    }

    override fun initData() {
        type = arguments?.getInt(Extra.EXTRA_MODE, -1) ?: -1
        vm.items.collectOnStart(viewLifecycleOwner) { onItems(it) }
        vm.filterSources.collectOnStart(viewLifecycleOwner) { onFilters(it) }
        vm.searchError.collectOnStart(viewLifecycleOwner) { onSearchError() }
        vm.loadFail.collectOnStart(viewLifecycleOwner) { onLoadFail() }
        vm.loadNetworkError.collectOnStart(viewLifecycleOwner) { onLoadNetworkError() }
        vm.loadEmpty.collectOnStart(viewLifecycleOwner) { onLoadEmpty() }
        load()
    }

    override fun onDestroyView() {
        provider?.clear()
        binding = null
        super.onDestroyView()
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
            .showForCaller(this)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_SOURCE) {
            val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
            val comic = pendingGroup?.comics?.getOrNull(index) ?: return
            openDetail(comic)
        }
    }

    private fun load() {
        when (type) {
            ResultActivity.LAUNCH_MODE_SEARCH -> vm.dispatch(ResultViewModel.SearchIntent.LoadMore)
            ResultActivity.LAUNCH_MODE_CATEGORY -> vm.loadCategory()
        }
    }

    private fun onItems(list: List<ResultViewModel.SearchGroup>) {
        if (list.isNotEmpty()) hideProgressBar()
        adapter.setData(list)
    }

    private fun onFilters(types: List<Int>) {
        val bind = binding ?: return
        if (type != ResultActivity.LAUNCH_MODE_SEARCH || types.size < 2) {
            bind.resultFilterScroll.visibility = View.GONE
            return
        }
        if (types == lastFilters) return
        lastFilters = types
        bind.resultFilterScroll.visibility = View.VISIBLE
        val chips = bind.resultFilterChips
        chips.removeAllViews()
        chips.addView(filterChip(getString(R.string.result_filter_all), ResultViewModel.FILTER_ALL, true, false))
        for (source in types) {
            chips.addView(filterChip(vm.titleGetter().getTitle(source), source, false, vm.isInvalid(source)))
        }
    }

    private fun filterChip(text: String, source: Int, checked: Boolean, invalid: Boolean): Chip {
        val chip = Chip(requireContext())
        val drawable = ChipDrawable.createFromAttributes(
            requireContext(),
            null,
            0,
            R.style.Guofeng_PillChip_Filter
        )
        chip.setChipDrawable(drawable)
        chip.elevation = 0f
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
        val activity = activity
        if (activity is MainActivity) {
            activity.openDetail(null, comic.source, comic.cid, null, comic.title, comic.cover, comic.author)
        } else {
            startActivity(
                DetailActivity.createIntent(
                    requireActivity(),
                    null,
                    comic.source,
                    comic.cid,
                    comic.title,
                    comic.cover,
                    comic.author
                )
            )
        }
    }

    private fun onLoadFail() {
        hideProgressBar()
        HintUtils.showSnackbar(binding?.resultLayout, getString(R.string.common_parse_error))
    }

    private fun onLoadNetworkError() {
        hideProgressBar()
        HintUtils.showSnackbar(binding?.resultLayout, getString(R.string.common_network_error))
    }

    private fun onLoadEmpty() {
        hideProgressBar()
        HintUtils.showSnackbar(binding?.resultLayout, getString(R.string.result_empty))
    }

    private fun onSearchError() {
        hideProgressBar()
        HintUtils.showSnackbar(binding?.resultLayout, getString(R.string.result_empty))
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CHARACTER, arguments?.getString(Extra.EXTRA_KEYWORD))
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "bySearch")
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false)
            FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }

    companion object {
        private const val DIALOG_SOURCE = 100

        fun newInstance(
            keyword: String?,
            source: IntArray?,
            type: Int,
            strictSearch: Boolean = true
        ): ResultFragment {
            val fragment = ResultFragment()
            val args = Bundle()
            args.putString(Extra.EXTRA_KEYWORD, keyword)
            args.putIntArray(Extra.EXTRA_SOURCE_LIST, source)
            args.putInt(Extra.EXTRA_MODE, type)
            args.putBoolean(Extra.EXTRA_STRICT, strictSearch)
            fragment.arguments = args
            return fragment
        }
    }
}
