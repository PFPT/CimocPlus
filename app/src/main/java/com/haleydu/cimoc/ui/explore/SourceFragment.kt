package com.haleydu.cimoc.ui.explore
import com.haleydu.cimoc.ui.common.RecyclerViewFragment
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.ui.explore.ExploreActivity
import com.haleydu.cimoc.ui.search.SearchActivity
import com.haleydu.cimoc.ui.explore.SourceDetailActivity
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.explore.SourceAdapter
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.explore.ScriptImportDialogFragment
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourceFragment : RecyclerViewFragment(), ThemeResponsive, SourceAdapter.OnItemCheckedListener, DialogCaller {

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
        vm.invalidTypes.collectOnStart(viewLifecycleOwner) { mSourceAdapter.setInvalidTypes(it) }
        vm.fail.collectOnStart(viewLifecycleOwner) { onSourceLoadFail() }
        vm.importSuccess.collectOnStart(viewLifecycleOwner) {
            HintUtils.showToast(activity, R.string.source_import_success)
        }
        vm.importFail.collectOnStart(viewLifecycleOwner) {
            HintUtils.showToast(activity, R.string.source_import_fail)
        }
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
            R.id.source_import_script -> {
                val fragment = ScriptImportDialogFragment.newInstance(DIALOG_IMPORT)
                fragment.setTargetFragment(this, 0)
                fragment.show(requireActivity().supportFragmentManager, null)
            }
            R.id.source_script_log -> {
                val logs = vm.logs().ifBlank { getString(R.string.source_script_log_empty) }
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.source_script_log)
                    .setMessage(logs)
                    .setPositiveButton(R.string.dialog_positive, null)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val source = mSourceAdapter.getItem(position)
        val host = activity
        if (host is com.haleydu.cimoc.ui.main.MainActivity) {
            host.openExploreSource(source.type)
        } else {
            startActivity(ExploreActivity.createIntent(requireActivity(), source.type))
        }
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
        mSourceAdapter.setData(list)
    }

    fun onSourceLoadFail() {
        hideProgressBar()
        HintUtils.showToast(activity, R.string.common_data_load_fail)
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
        mSourceAdapter.setColor(ContextCompat.getColor(requireActivity(), accent))
        mSourceAdapter.notifyDataSetChanged()
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_IMPORT) {
            val value = bundle.getString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE).orEmpty()
            vm.importScript(value)
        }
    }

    companion object {
        private const val DIALOG_IMPORT = 0
    }
}
