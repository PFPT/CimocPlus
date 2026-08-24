package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.ui.activity.DirPickerActivity
import com.haleydu.cimoc.ui.activity.TaskActivity
import com.haleydu.cimoc.ui.collectOnStart
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class LocalFragment : GridFragment() {

    private val vm: LocalViewModel by viewModels()

    override fun initData() {
        vm.comics.collectOnStart(viewLifecycleOwner) { onComicLoadSuccess(it) }
        vm.scanSuccess.collectOnStart(viewLifecycleOwner) { onLocalScanSuccess(it) }
        vm.deleteSuccess.collectOnStart(viewLifecycleOwner) { onLocalDeleteSuccess(it) }
        vm.loadFail.collectOnStart(viewLifecycleOwner) { onComicLoadFail() }
        vm.fail.collectOnStart(viewLifecycleOwner) { onExecuteFail() }
        vm.load()
    }

    override fun performActionButtonClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                requireActivity().startActivityForResult(intent, DIALOG_REQUEST_SCAN)
            } catch (e: ActivityNotFoundException) {
                HintUtils.showToast(activity, R.string.settings_other_storage_not_found)
            }
        } else {
            startActivityForResult(Intent(activity, DirPickerActivity::class.java), DIALOG_REQUEST_SCAN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == DIALOG_REQUEST_SCAN) {
            showProgressDialog()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val uri = data?.data
                if (uri != null) {
                    val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireActivity().contentResolver.takePersistableUriPermission(uri, flags)
                    vm.scan(DocumentFile.fromTreeUri(activity, uri))
                }
            } else {
                val path = data?.getStringExtra(Extra.EXTRA_PICKER_PATH)
                if (path != null) {
                    if (!StringUtils.isEmpty(path)) {
                        vm.scan(DocumentFile.fromFile(File(path)))
                    } else {
                        onExecuteFail()
                    }
                }
            }
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = mGridAdapter.getItem(position) as MiniComic
        startActivity(TaskActivity.createIntent(activity, comic.id))
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_OPERATION -> when (bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)) {
                OPERATION_INFO -> showComicInfo(vm.loadComic(mSavedId), DIALOG_REQUEST_INFO)
                OPERATION_DELETE -> {
                    val fragment = MessageDialogFragment.newInstance(
                        R.string.dialog_confirm,
                        R.string.local_delete_confirm,
                        true,
                        DIALOG_REQUEST_DELETE
                    )
                    fragment.setTargetFragment(this, 0)
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
            DIALOG_REQUEST_DELETE -> {
                showProgressDialog()
                vm.deleteComic(mSavedId)
            }
        }
    }

    fun onLocalDeleteSuccess(id: Long) {
        hideProgressDialog()
        mGridAdapter.removeItemById(id)
        HintUtils.showToast(activity, R.string.common_execute_success)
    }

    fun onLocalScanSuccess(list: List<Any>) {
        hideProgressDialog()
        mGridAdapter.addAll(list)
    }

    override fun onExecuteFail() {
        hideProgressDialog()
        HintUtils.showToast(activity, R.string.common_execute_fail)
    }

    override fun getActionButtonRes(): Int = R.drawable.ic_add_white_24dp

    override fun getOperationItems(): Array<String> =
        arrayOf(getString(R.string.comic_info), getString(R.string.local_delete))

    companion object {
        private const val DIALOG_REQUEST_SCAN = 1
        private const val DIALOG_REQUEST_INFO = 2
        private const val DIALOG_REQUEST_DELETE = 3
        private const val OPERATION_INFO = 0
        private const val OPERATION_DELETE = 1
    }
}
