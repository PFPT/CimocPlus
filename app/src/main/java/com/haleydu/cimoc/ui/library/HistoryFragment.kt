package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.ui.collectOnStart
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : GridFragment() {

    private val vm: HistoryViewModel by viewModels()

    override fun initData() {
        vm.comics.collectOnStart(viewLifecycleOwner) { onComicLoadSuccess(it) }
        vm.clearSuccess.collectOnStart(viewLifecycleOwner) { onHistoryClearSuccess() }
        vm.loadFail.collectOnStart(viewLifecycleOwner) { onComicLoadFail() }
        vm.fail.collectOnStart(viewLifecycleOwner) { onExecuteFail() }
        AppEventBus.observe(AppEvent.EVENT_COMIC_READ).collectOnStart(viewLifecycleOwner) {
            onItemUpdate(it.getData() as MiniComic)
        }
        @Suppress("UNCHECKED_CAST")
        AppEventBus.observe(AppEvent.EVENT_COMIC_HISTORY_RESTORE).collectOnStart(viewLifecycleOwner) {
            OnComicRestore(it.getData() as List<Any>)
        }
        vm.load()
    }

    override fun performActionButtonClick() {
        if (mGridAdapter.dateSet.isEmpty()) {
            return
        }
        val fragment = MessageDialogFragment.newInstance(
            R.string.dialog_confirm,
            R.string.history_clear_confirm,
            true,
            DIALOG_REQUEST_CLEAR
        )
        fragment.setTargetFragment(this, 0)
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_OPERATION -> when (bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)) {
                OPERATION_INFO -> showComicInfo(vm.loadComic(mSavedId), DIALOG_REQUEST_INFO)
                OPERATION_DELETE -> {
                    val fragment = MessageDialogFragment.newInstance(
                        R.string.dialog_confirm,
                        R.string.history_delete_confirm,
                        true,
                        DIALOG_REQUEST_DELETE
                    )
                    fragment.setTargetFragment(this, 0)
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
            DIALOG_REQUEST_CLEAR -> {
                showProgressDialog()
                vm.clear()
            }
            DIALOG_REQUEST_DELETE -> {
                showProgressDialog()
                vm.delete(mSavedId)
                onHistoryDelete(mSavedId)
            }
        }
    }

    fun onHistoryClearSuccess() {
        hideProgressDialog()
        mGridAdapter.clear()
        HintUtils.showToast(activity, R.string.common_execute_success)
    }

    fun onHistoryDelete(id: Long) {
        hideProgressDialog()
        mGridAdapter.removeItemById(mSavedId)
        HintUtils.showToast(activity, R.string.common_execute_success)
    }

    fun OnComicRestore(list: List<Any>) {
        mGridAdapter.addAll(0, list)
    }

    fun onItemUpdate(comic: MiniComic) {
        mGridAdapter.remove(comic)
        mGridAdapter.add(0, comic)
    }

    override fun getActionButtonRes(): Int = R.drawable.ic_delete_white_24dp

    override fun getOperationItems(): Array<String> =
        arrayOf(getString(R.string.comic_info), getString(R.string.history_delete))

    companion object {
        private const val DIALOG_REQUEST_CLEAR = 1
        private const val DIALOG_REQUEST_INFO = 2
        private const val DIALOG_REQUEST_DELETE = 3
        private const val OPERATION_INFO = 0
        private const val OPERATION_DELETE = 1
    }
}
