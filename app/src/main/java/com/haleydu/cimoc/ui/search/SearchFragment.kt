package com.haleydu.cimoc.ui.search

import com.haleydu.cimoc.ui.common.BaseFragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.databinding.FragmentSearchBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.misc.Switcher
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.ui.search.SearchViewModel
import com.haleydu.cimoc.ui.search.AutoCompleteAdapter
import com.haleydu.cimoc.ui.common.addMenu
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.MultiAdpaterDialogFragment
import com.haleydu.cimoc.ui.common.dialog.showForCaller
import com.haleydu.cimoc.utils.CollectionUtils
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : BaseFragment(), DialogCaller, ThemeResponsive, TextView.OnEditorActionListener {

    private val vm: SearchViewModel by viewModels()
    private var binding: FragmentSearchBinding? = null
    private var arrayAdapter: ArrayAdapter<String>? = null
    private val sourceList = ArrayList<Switcher<Source>>()
    private var autoComplete = false

    override fun getLayoutRes(): Int = R.layout.fragment_search

    override fun bindViews(view: View) {
        super.bindViews(view)
        binding = FragmentSearchBinding.bind(view)
    }

    override fun initView() {
        val binding = binding ?: return
        autoComplete = mPreference.getBoolean(PreferenceManager.PREF_SEARCH_AUTO_COMPLETE, false)
        binding.searchKeywordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.searchTextLayout.error = null
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (autoComplete) {
                    val keyword = binding.searchKeywordInput.text?.toString()
                    if (!keyword.isNullOrEmpty()) {
                        vm.loadAutoComplete(keyword)
                    }
                }
            }
        })
        binding.searchKeywordInput.setOnEditorActionListener(this)
        binding.searchTextLayout.setStartIconOnClickListener { submitSearch() }
        if (autoComplete) {
            arrayAdapter = AutoCompleteAdapter(requireContext())
            binding.searchKeywordInput.setAdapter(arrayAdapter)
        }
        binding.searchHistoryClear.setOnClickListener {
            vm.clearHistory()
        }
        bindGenreChips()
    }

    override fun initData() {
        addMenu(R.menu.menu_search, onCreate = { menu ->
            menu.findItem(R.id.search_menu_strict)?.isChecked = vm.strictSearch
        }) { item ->
            when (item.itemId) {
                R.id.search_menu_source -> {
                    if (sourceList.isEmpty()) return@addMenu true
                    val titles = Array(sourceList.size) { sourceList[it].element.title }
                    val checks = BooleanArray(sourceList.size) { sourceList[it].isEnable }
                    MultiAdpaterDialogFragment.newInstance(
                        R.string.search_source_select,
                        titles,
                        checks,
                        DIALOG_REQUEST_SOURCE
                    ).showForCaller(this)
                    true
                }
                R.id.search_menu_strict -> {
                    item.isChecked = !item.isChecked
                    vm.strictSearch = item.isChecked
                    true
                }
                else -> false
            }
        }
        vm.sources.collectOnStart(viewLifecycleOwner) { onSourceLoadSuccess(it) }
        vm.sourceFail.collectOnStart(viewLifecycleOwner) { onSourceLoadFail() }
        vm.autoComplete.collectOnStart(viewLifecycleOwner) { onAutoCompleteLoadSuccess(it) }
        vm.history.collectOnStart(viewLifecycleOwner) { bindHistoryChips(it) }
        vm.loadSource()
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_REQUEST_SOURCE) {
            val check = bundle.getBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE) ?: return
            for (i in sourceList.indices) {
                sourceList[i].isEnable = check[i]
            }
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            submitSearch()
            return true
        }
        return false
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun submitSearch(raw: String? = null) {
        val binding = binding ?: return
        val keyword = (raw ?: binding.searchKeywordInput.text?.toString()).orEmpty().trim()
        if (keyword.isEmpty()) {
            binding.searchTextLayout.error = getString(R.string.search_keyword_empty)
            return
        }
        val list = ArrayList<Int>()
        for (switcher in sourceList) {
            if (switcher.isEnable) {
                list.add(switcher.element.type)
            }
        }
        if (list.isEmpty()) {
            HintUtils.showToast(activity, R.string.search_source_none)
            return
        }
        vm.addHistory(keyword)
        val sources = CollectionUtils.unbox(list)
        val host = activity
        if (host is com.haleydu.cimoc.ui.main.MainActivity) {
            host.openResult(keyword, sources, vm.strictSearch, ResultActivity.LAUNCH_MODE_SEARCH)
        } else {
            startActivity(
                ResultActivity.createIntent(
                    requireActivity(),
                    keyword,
                    vm.strictSearch,
                    sources,
                    ResultActivity.LAUNCH_MODE_SEARCH
                )
            )
        }
    }

    private fun bindHistoryChips(items: List<String>) {
        val binding = binding ?: return
        binding.searchHistoryGroup.removeAllViews()
        binding.searchHistoryHeader.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        for (keyword in items) {
            binding.searchHistoryGroup.addView(suggestionChip(keyword) {
                binding.searchKeywordInput.setText(keyword)
                submitSearch(keyword)
            })
        }
    }

    private fun bindGenreChips() {
        val binding = binding ?: return
        binding.searchGenreGroup.removeAllViews()
        val tags = resources.getStringArray(R.array.search_genre_tags)
        for (tag in tags) {
            binding.searchGenreGroup.addView(suggestionChip(tag) {
                binding.searchKeywordInput.setText(tag)
                submitSearch(tag)
            })
        }
    }

    private fun suggestionChip(text: String, onClick: () -> Unit): Chip {
        val chip = Chip(requireContext())
        val drawable = ChipDrawable.createFromAttributes(
            requireContext(),
            null,
            0,
            R.style.Guofeng_PillChip
        )
        chip.setChipDrawable(drawable)
        chip.text = text
        chip.isCheckable = false
        chip.isClickable = true
        chip.elevation = 0f
        chip.setOnClickListener { onClick() }
        return chip
    }

    private fun onAutoCompleteLoadSuccess(list: List<String>) {
        arrayAdapter?.clear()
        arrayAdapter?.addAll(list)
    }

    private fun onSourceLoadSuccess(list: List<Source>) {
        hideProgressBar()
        sourceList.clear()
        val filterSource = arguments?.getInt(Extra.EXTRA_SOURCE, -1) ?: -1
        var found = false
        for (source in list) {
            val enable = filterSource < 0 || source.type == filterSource
            if (enable && source.type == filterSource) {
                found = true
            }
            sourceList.add(Switcher(source, enable))
        }
        if (filterSource >= 0 && !found) {
            val source = vm.loadSource(filterSource)
            if (source != null) {
                sourceList.add(Switcher(source, true))
            }
        }
    }

    private fun onSourceLoadFail() {
        hideProgressBar()
        HintUtils.showToast(activity, R.string.search_source_load_fail)
    }

    companion object {
        private const val DIALOG_REQUEST_SOURCE = 0

        fun newInstance(sourceType: Int = -1): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putInt(Extra.EXTRA_SOURCE, sourceType)
            fragment.arguments = args
            return fragment
        }
    }
}
