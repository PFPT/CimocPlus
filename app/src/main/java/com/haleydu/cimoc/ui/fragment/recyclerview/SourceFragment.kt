package com.haleydu.cimoc.ui.fragment.recyclerview

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.ui.activity.CategoryActivity
import com.haleydu.cimoc.ui.activity.SearchActivity
import com.haleydu.cimoc.ui.activity.SourceDetailActivity
import com.haleydu.cimoc.ui.adapter.BaseAdapter
import com.haleydu.cimoc.ui.adapter.SourceAdapter
import com.haleydu.cimoc.ui.collectOnStart
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourceFragment : RecyclerViewFragment(), ThemeResponsive, SourceAdapter.OnItemCheckedListener {

    private val vm: SourceViewModel by viewModels()
    private lateinit var mSourceAdapter: SourceAdapter

    override fun initView() {
        setHasOptionsMenu(true)
        super.initView()
    }

    override fun initAdapter(): BaseAdapter<*> {
        mSourceAdapter = SourceAdapter(activity, ArrayList())
        mSourceAdapter.setOnItemCheckedListener(this)
        return mSourceAdapter
    }

    override fun initLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(activity, 2)
    }

    override fun initData() {
        vm.sources.collectOnStart(viewLifecycleOwner) { onSourceLoadSuccess(it) }
        vm.fail.collectOnStart(viewLifecycleOwner) { onSourceLoadFail() }
        vm.load()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_source, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.comic_search -> startActivity(Intent(activity, SearchActivity::class.java))
            R.id.comic_inverseSelection -> {
                for (i in 0 until mSourceAdapter.itemCount) {
                    val source = mSourceAdapter.getItem(i)
                    source.enable = !source.enable
                    vm.update(source)
                }
                mSourceAdapter.notifyDataSetChanged()
            }
            R.id.comic_allSelection -> {
                for (i in 0 until mSourceAdapter.itemCount) {
                    val source = mSourceAdapter.getItem(i)
                    source.enable = true
                    vm.update(source)
                }
                mSourceAdapter.notifyDataSetChanged()
            }
            R.id.comic_AllDeselect -> {
                for (i in 0 until mSourceAdapter.itemCount) {
                    val source = mSourceAdapter.getItem(i)
                    source.enable = false
                    vm.update(source)
                }
                mSourceAdapter.notifyDataSetChanged()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val source = mSourceAdapter.getItem(position)
        val intent = if (!vm.hasCategory(source.type)) {
            SearchActivity.createIntent(activity, source.type, source.title)
        } else {
            CategoryActivity.createIntent(activity, source.type, source.title)
        }
        startActivity(intent)
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        startActivity(SourceDetailActivity.createIntent(activity, mSourceAdapter.getItem(position).type))
        return true
    }

    override fun onItemCheckedListener(isChecked: Boolean, position: Int) {
        val source = mSourceAdapter.getItem(position)
        source.enable = isChecked
        vm.update(source)
    }

    fun onSourceLoadSuccess(list: List<Source>) {
        hideProgressBar()
        mSourceAdapter.addAll(list)
    }

    fun onSourceLoadFail() {
        hideProgressBar()
        HintUtils.showToast(activity, R.string.common_data_load_fail)
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
        mSourceAdapter.setColor(ContextCompat.getColor(requireActivity(), accent))
        mSourceAdapter.notifyDataSetChanged()
    }
}
