package com.haleydu.cimoc.ui.library
import com.haleydu.cimoc.ui.common.BaseFragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.fragment.app.viewModels
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.databinding.FragmentLibraryBinding
import com.haleydu.cimoc.data.TagManager
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.ui.library.PartFavoriteActivity
import com.haleydu.cimoc.ui.common.addMenu
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.ItemDialogFragment
import com.haleydu.cimoc.ui.common.dialog.showForCaller
import com.haleydu.cimoc.ui.library.DownloadFragment
import com.haleydu.cimoc.ui.library.FavoriteFragment
import com.haleydu.cimoc.ui.library.HistoryFragment
import com.haleydu.cimoc.ui.library.LocalFragment
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibraryFragment : BaseFragment(), DialogCaller, ThemeResponsive {

    private val vm: ComicViewModel by viewModels()
    private var binding: FragmentLibraryBinding? = null
    private val tagList = ArrayList<Tag>()
    private var currentTag = TAG_FAVORITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTag = savedInstanceState?.getString(KEY_TAG) ?: TAG_FAVORITE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAG, currentTag)
    }

    override fun getLayoutRes(): Int = R.layout.fragment_library

    override fun bindViews(view: View) {
        super.bindViews(view)
        binding = FragmentLibraryBinding.bind(view)
    }

    override fun initView() {
        val binding = binding ?: return
        ensureChildren()
        binding.libraryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: View.NO_ID
            if (checkedId == View.NO_ID) return@setOnCheckedStateChangeListener
            val tag = when (checkedId) {
                R.id.library_chip_history -> TAG_HISTORY
                R.id.library_chip_local -> TAG_LOCAL
                R.id.library_chip_download -> TAG_DOWNLOAD
                else -> TAG_FAVORITE
            }
            showChild(tag)
        }
        when (currentTag) {
            TAG_HISTORY -> binding.libraryChipHistory.isChecked = true
            TAG_LOCAL -> binding.libraryChipLocal.isChecked = true
            TAG_DOWNLOAD -> binding.libraryChipDownload.isChecked = true
            else -> binding.libraryChipFavorite.isChecked = true
        }
        showChild(currentTag)
    }

    override fun initData() {
        addMenu(R.menu.menu_comic) { item ->
            when (item.itemId) {
                R.id.comic_filter -> {
                    showProgressDialog()
                    tagList.clear()
                    vm.loadTag()
                    true
                }
                R.id.comic_search -> {
                    (activity as? MainActivity)?.openSearch()
                    true
                }
                R.id.comic_bbs -> {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_gitter_url))))
                    } catch (_: Exception) {
                    }
                    true
                }
                R.id.comic_cancel_highlight -> {
                    (childFragmentManager.findFragmentByTag(TAG_FAVORITE) as? FavoriteFragment)?.cancelAllHighlight()
                    true
                }
                else -> false
            }
        }
        vm.tags.collectOnStart(viewLifecycleOwner) { onTagLoadSuccess(it) }
        vm.fail.collectOnStart(viewLifecycleOwner) { onTagLoadFail() }
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        if (requestCode == DIALOG_REQUEST_FILTER) {
            val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
            startActivity(
                PartFavoriteActivity.createIntent(
                    activity,
                    tagList[index].id,
                    tagList[index].title
                )
            )
        }
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
        for (fragment in childFragmentManager.fragments) {
            (fragment as? ThemeResponsive)?.onThemeChange(primary, accent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    fun showDownload() {
        binding?.libraryChipDownload?.isChecked = true
    }

    private fun ensureChildren() {
        val fm = childFragmentManager
        val tx = fm.beginTransaction()
        var changed = false
        if (fm.findFragmentByTag(TAG_FAVORITE) == null) {
            val favorite = FavoriteFragment()
            val history = HistoryFragment()
            val local = LocalFragment()
            tx.add(R.id.library_container, favorite, TAG_FAVORITE)
                .add(R.id.library_container, history, TAG_HISTORY)
                .hide(history)
                .add(R.id.library_container, local, TAG_LOCAL)
                .hide(local)
            changed = true
        }
        if (fm.findFragmentByTag(TAG_DOWNLOAD) == null) {
            val download = DownloadFragment()
            tx.add(R.id.library_container, download, TAG_DOWNLOAD).hide(download)
            changed = true
        }
        if (changed) tx.commitNow()
    }

    private fun showChild(tag: String) {
        currentTag = tag
        val fm = childFragmentManager
        val favorite = fm.findFragmentByTag(TAG_FAVORITE) ?: return
        val history = fm.findFragmentByTag(TAG_HISTORY) ?: return
        val local = fm.findFragmentByTag(TAG_LOCAL) ?: return
        val download = fm.findFragmentByTag(TAG_DOWNLOAD) ?: return
        val tx = fm.beginTransaction()
        listOf(favorite, history, local, download).forEach { tx.hide(it) }
        when (tag) {
            TAG_HISTORY -> tx.show(history)
            TAG_LOCAL -> tx.show(local)
            TAG_DOWNLOAD -> tx.show(download)
            else -> tx.show(favorite)
        }
        tx.commit()
    }

    private fun onTagLoadSuccess(list: List<Tag>) {
        hideProgressDialog()
        tagList.add(Tag(TagManager.TAG_FINISH, getString(R.string.comic_status_finish)))
        tagList.add(Tag(TagManager.TAG_CONTINUE, getString(R.string.comic_status_continue)))
        tagList.addAll(list)
        val items = Array(tagList.size) { tagList[it].title }
        ItemDialogFragment.newInstance(R.string.comic_tag_select, items, DIALOG_REQUEST_FILTER)
            .showForCaller(this)
    }

    private fun onTagLoadFail() {
        hideProgressDialog()
        HintUtils.showToast(activity, R.string.comic_load_tag_fail)
    }

    companion object {
        private const val DIALOG_REQUEST_FILTER = 0
        private const val KEY_TAG = "library_current_tag"
        private const val TAG_FAVORITE = "library_favorite"
        private const val TAG_HISTORY = "library_history"
        private const val TAG_LOCAL = "library_local"
        private const val TAG_DOWNLOAD = "library_download"
    }
}
