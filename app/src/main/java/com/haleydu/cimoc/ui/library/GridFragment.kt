package com.haleydu.cimoc.ui.library
import android.content.Intent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.databinding.FragmentGridBinding
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.ui.common.GridAdapter
import com.haleydu.cimoc.ui.common.RecyclerViewFragment
import com.haleydu.cimoc.ui.common.dialog.ItemDialogFragment
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.ui.detail.DetailActivity
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import javax.inject.Inject

abstract class GridFragment : RecyclerViewFragment(), DialogCaller, ThemeResponsive {

    protected val DIALOG_REQUEST_OPERATION = 0
    protected lateinit var mGridAdapter: GridAdapter
    protected var mSavedId: Long = -1
    protected lateinit var mActionButton: FloatingActionButton
    @Inject
    lateinit var sourceManager: SourceManager

    override fun initView() {
        super.initView()
        mRecyclerView.itemAnimator = DefaultItemAnimator()
    }

    override fun initAdapter(): RecyclerView.Adapter<*> {
        mGridAdapter = GridAdapter(requireActivity())
        mGridAdapter.setProvider(appInstance.builderProvider)
        mGridAdapter.setTitleGetter(sourceManager.TitleGetter())
        mRecyclerView.setRecycledViewPool(appInstance.gridRecycledPool)
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> appInstance.builderProvider.pause()
                    RecyclerView.SCROLL_STATE_IDLE -> appInstance.builderProvider.resume()
                }
            }
        })
        mActionButton.setImageResource(getActionButtonRes())
        return mGridAdapter
    }

    override fun initLayoutManager(): RecyclerView.LayoutManager {
        val manager = GridLayoutManager(activity, 3)
        manager.recycleChildrenOnDetach = true
        return manager
    }

    fun onActionButtonClick() {
        performActionButtonClick()
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = mGridAdapter.comicAt(position) ?: return
        if (comic.isLocal) {
            startActivity(TaskActivity.createIntent(requireActivity(), comic.id))
            return
        }
        val host = activity
        if (host is MainActivity) {
            host.openDetail(comic.id, comic.source, comic.cid, view)
            return
        }
        val intent = DetailActivity.createIntent(requireActivity(), comic.id, comic.source, comic.cid)
        startActivity(intent)
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        val comic = mGridAdapter.comicAt(position) ?: return true
        mSavedId = comic.id ?: -1
        val fragment = ItemDialogFragment.newInstance(
            R.string.common_operation_select,
            getOperationItems(),
            DIALOG_REQUEST_OPERATION
        )
        fragment.show(childFragmentManager, null)
        return true
    }

    fun onComicLoadSuccess(list: List<*>) {
        mGridAdapter.setData(list)
    }

    fun onComicLoadFail() {
        HintUtils.showToast(requireActivity(), R.string.common_data_load_fail)
    }

    open fun onExecuteFail() {
        hideProgressDialog()
        HintUtils.showToast(requireActivity(), R.string.common_execute_fail)
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
        mActionButton.backgroundTintList = ContextCompat.getColorStateList(requireActivity(), accent)
    }

    protected fun showComicInfo(comic: Comic?, request: Int) {
        if (comic == null) {
            val fragment = MessageDialogFragment.newInstance(
                R.string.common_execute_fail,
                R.string.comic_info_not_found,
                true,
                request
            )
            fragment.show(childFragmentManager, null)
            return
        }
        val content = StringUtils.format(
            "%s  %s\n%s  %s\n%s  %s\n%s  %s\n%s  %s",
            getString(R.string.comic_info_title),
            comic.title,
            getString(R.string.comic_info_source),
            sourceManager.getParser(comic.source).getTitle(),
            getString(R.string.comic_info_status),
            if (comic.finish == null) getString(R.string.comic_status_finish)
            else getString(R.string.comic_status_continue),
            getString(R.string.comic_info_chapter),
            comic.chapter ?: getString(R.string.common_null),
            getString(R.string.comic_info_time),
            if (comic.history == null) getString(R.string.common_null)
            else StringUtils.getFormatTime("yyyy-MM-dd HH:mm:ss", comic.history!!)
        )
        val fragment = MessageDialogFragment.newInstance(R.string.comic_info, content, true, request)
        fragment.show(childFragmentManager, null)
    }

    protected abstract fun performActionButtonClick()

    protected abstract fun getActionButtonRes(): Int

    protected abstract fun getOperationItems(): Array<String>

    override fun getLayoutRes(): Int {
        return R.layout.fragment_grid
    }

    override fun bindViews(view: View) {
        super.bindViews(view)
        val binding = FragmentGridBinding.bind(view)
        mRecyclerView = binding.recyclerViewContent
        mActionButton = binding.gridActionButton
        binding.gridActionButton.setOnClickListener { onActionButtonClick() }
    }
}
