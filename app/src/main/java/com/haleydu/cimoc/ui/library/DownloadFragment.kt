package com.haleydu.cimoc.ui.library
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.ui.library.TaskActivity
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.ServiceUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadFragment : GridFragment() {

    private val vm: DownloadViewModel by viewModels()
    private var isDownload = false

    override fun initView() {
        isDownload = ServiceUtils.isServiceRunning(activity, DownloadService::class.java)
        super.initView()
    }

    override fun initData() {
        vm.comics.collectOnStart(viewLifecycleOwner) { onComicLoadSuccess(it) }
        vm.deleteSuccess.collectOnStart(viewLifecycleOwner) { onDownloadDeleteSuccess(it) }
        vm.tasks.collectOnStart(viewLifecycleOwner) { onTaskLoadSuccess(it) }
        vm.loadFail.collectOnStart(viewLifecycleOwner) { onComicLoadFail() }
        vm.fail.collectOnStart(viewLifecycleOwner) { onExecuteFail() }
        AppEventBus.observe(AppEvent.EVENT_TASK_INSERT).collectOnStart(viewLifecycleOwner) {
            onDownloadAdd(it.getData() as MiniComic)
        }
        AppEventBus.observe(AppEvent.EVENT_DOWNLOAD_REMOVE).collectOnStart(viewLifecycleOwner) {
            onDownloadDelete(it.getData() as Long)
        }
        @Suppress("UNCHECKED_CAST")
        AppEventBus.observe(AppEvent.EVENT_DOWNLOAD_CLEAR).collectOnStart(viewLifecycleOwner) {
            for (id in it.getData() as List<Long>) {
                onDownloadDelete(id)
            }
        }
        AppEventBus.observe(AppEvent.EVENT_DOWNLOAD_START).collectOnStart(viewLifecycleOwner) {
            onDownloadStart()
        }
        AppEventBus.observe(AppEvent.EVENT_DOWNLOAD_STOP).collectOnStart(viewLifecycleOwner) {
            onDownloadStop()
        }
        vm.load()
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_OPERATION -> when (bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)) {
                OPERATION_INFO -> showComicInfo(vm.loadComic(mSavedId), DIALOG_REQUEST_INFO)
                OPERATION_DELETE -> {
                    val fragment = MessageDialogFragment.newInstance(
                        R.string.dialog_confirm,
                        R.string.download_delete_confirm,
                        true,
                        DIALOG_REQUEST_DELETE
                    )
                    fragment.setTargetFragment(this, 0)
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
            DIALOG_REQUEST_SWITCH -> if (isDownload) {
                ServiceUtils.stopService(activity, DownloadService::class.java)
                HintUtils.showToast(activity, R.string.download_stop_success)
            } else {
                showProgressDialog()
                vm.loadTask()
            }
            DIALOG_REQUEST_DELETE -> if (isDownload) {
                HintUtils.showToast(activity, R.string.download_ask_stop)
            } else {
                showProgressDialog()
                vm.deleteComic(mSavedId)
            }
        }
    }

    override fun performActionButtonClick() {
        if (mGridAdapter.dateSet.isEmpty()) {
            return
        }
        val fragment = MessageDialogFragment.newInstance(
            R.string.dialog_confirm,
            R.string.download_action_confirm,
            true,
            DIALOG_REQUEST_SWITCH
        )
        fragment.setTargetFragment(this, 0)
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = mGridAdapter.comicAt(position)
        startActivity(TaskActivity.createIntent(requireActivity(), comic.id))
    }

    fun onDownloadAdd(comic: MiniComic) {
        if (!mGridAdapter.exist(comic)) {
            mGridAdapter.add(0, comic)
        }
    }

    fun onDownloadDelete(id: Long) {
        mGridAdapter.removeItemById(id)
    }

    fun onDownloadDeleteSuccess(id: Long) {
        hideProgressDialog()
        mGridAdapter.removeItemById(id)
        HintUtils.showToast(activity, R.string.common_execute_success)
    }

    fun onDownloadStart() {
        if (!isDownload) {
            isDownload = true
            mActionButton.setImageResource(R.drawable.ic_pause_white_24dp)
        }
    }

    fun onDownloadStop() {
        if (isDownload) {
            isDownload = false
            mActionButton.setImageResource(R.drawable.ic_play_arrow_white_24dp)
        }
    }

    fun onTaskLoadSuccess(list: ArrayList<Task>) {
        if (list.isEmpty()) {
            HintUtils.showToast(activity, R.string.download_task_empty)
        } else {
            DownloadService.start(requireActivity(), DownloadService.createIntent(activity, list))
            HintUtils.showToast(activity, R.string.download_start_success)
        }
        hideProgressDialog()
    }

    override fun getActionButtonRes(): Int =
        if (isDownload) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp

    override fun getOperationItems(): Array<String> =
        arrayOf(getString(R.string.comic_info), getString(R.string.download_delete))

    companion object {
        private const val DIALOG_REQUEST_SWITCH = 1
        private const val DIALOG_REQUEST_INFO = 2
        private const val DIALOG_REQUEST_DELETE = 3
        private const val OPERATION_INFO = 0
        private const val OPERATION_DELETE = 1
    }
}
