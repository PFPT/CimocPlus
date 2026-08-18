package com.haleydu.cimoc.ui.fragment.recyclerview

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.ui.activity.PartFavoriteActivity
import com.haleydu.cimoc.ui.activity.SearchActivity
import com.haleydu.cimoc.ui.adapter.BaseAdapter
import com.haleydu.cimoc.ui.adapter.TagAdapter
import com.haleydu.cimoc.ui.collectOnStart
import com.haleydu.cimoc.ui.fragment.dialog.EditorDialogFragment
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagFragment : RecyclerViewFragment(), DialogCaller, ThemeResponsive {

    private val vm: TagViewModel by viewModels()
    private lateinit var mTagAdapter: TagAdapter
    private var mSavedTag: Tag? = null

    override fun initView() {
        setHasOptionsMenu(true)
        super.initView()
    }

    override fun initAdapter(): BaseAdapter<*> {
        mTagAdapter = TagAdapter(activity, ArrayList())
        return mTagAdapter
    }

    override fun initLayoutManager(): RecyclerView.LayoutManager {
        return StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun initData() {
        vm.tags.collectOnStart(viewLifecycleOwner) { onTagLoadSuccess(it) }
        vm.loadFail.collectOnStart(viewLifecycleOwner) { onTagLoadFail() }
        vm.deleteSuccess.collectOnStart(viewLifecycleOwner) { onTagDeleteSuccess(it) }
        vm.deleteFail.collectOnStart(viewLifecycleOwner) { onTagDeleteFail() }
        @Suppress("UNCHECKED_CAST")
        AppEventBus.observe(AppEvent.EVENT_TAG_RESTORE).collectOnStart(viewLifecycleOwner) {
            onTagRestore(it.getData() as List<Tag>)
        }
        vm.load()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_tag, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.comic_search -> startActivity(Intent(activity, SearchActivity::class.java))
            R.id.tag_add -> {
                val fragment = EditorDialogFragment.newInstance(R.string.tag_add, null, DIALOG_REQUEST_EDITOR)
                fragment.setTargetFragment(this, 0)
                fragment.show(requireActivity().supportFragmentManager, null)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val tag = mTagAdapter.getItem(position)
        startActivity(PartFavoriteActivity.createIntent(activity, tag.id, tag.title))
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        mSavedTag = mTagAdapter.getItem(position)
        val fragment = MessageDialogFragment.newInstance(
            R.string.dialog_confirm,
            R.string.tag_delete_confirm,
            true,
            DIALOG_REQUEST_DELETE
        )
        fragment.setTargetFragment(this, 0)
        fragment.show(requireActivity().supportFragmentManager, null)
        return true
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_DELETE -> {
                showProgressDialog()
                mSavedTag?.let { vm.delete(it) }
            }
            DIALOG_REQUEST_EDITOR -> {
                val text = bundle.getString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
                if (text != null && !StringUtils.isEmpty(text)) {
                    val tag = Tag(null, text)
                    vm.insert(tag)
                    mTagAdapter.add(tag)
                    HintUtils.showToast(activity, R.string.common_execute_success)
                }
            }
        }
    }

    fun onTagRestore(list: List<Tag>) {
        mTagAdapter.addAll(list)
    }

    fun onTagDeleteSuccess(tag: Tag) {
        hideProgressDialog()
        mTagAdapter.remove(tag)
        HintUtils.showToast(activity, R.string.common_execute_success)
    }

    fun onTagDeleteFail() {
        hideProgressDialog()
        HintUtils.showToast(activity, R.string.common_execute_fail)
    }

    fun onTagLoadSuccess(list: List<Tag>) {
        hideProgressBar()
        mTagAdapter.addAll(list)
    }

    fun onTagLoadFail() {
        hideProgressBar()
        HintUtils.showToast(activity, R.string.common_data_load_fail)
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
        mTagAdapter.setColor(ContextCompat.getColor(requireActivity(), primary))
        mTagAdapter.notifyDataSetChanged()
    }

    companion object {
        private const val DIALOG_REQUEST_DELETE = 0
        private const val DIALOG_REQUEST_EDITOR = 1
    }
}
