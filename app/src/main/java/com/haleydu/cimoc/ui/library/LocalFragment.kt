package com.haleydu.cimoc.ui.library
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.ui.library.DirPickerActivity
import com.haleydu.cimoc.ui.library.TaskActivity
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.ui.common.dialog.showForCaller
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class LocalFragment : GridFragment() {

    private val vm: LocalViewModel by viewModels()

    private val treeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        showProgressDialog()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        requireActivity().contentResolver.takePersistableUriPermission(uri, flags)
        vm.scan(DocumentFile.fromTreeUri(activity, uri))
    }

    private val dirLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        showProgressDialog()
        val path = result.data?.getStringExtra(Extra.EXTRA_PICKER_PATH)
        if (path != null) {
            if (!StringUtils.isEmpty(path)) {
                vm.scan(DocumentFile.fromFile(File(path)))
            } else {
                onExecuteFail()
            }
        }
    }

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
                treeLauncher.launch(null)
            } catch (e: ActivityNotFoundException) {
                HintUtils.showToast(activity, R.string.settings_other_storage_not_found)
            }
        } else {
            dirLauncher.launch(Intent(activity, DirPickerActivity::class.java))
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = mGridAdapter.comicAt(position)
        startActivity(TaskActivity.createIntent(requireActivity(), comic.id))
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
                    fragment.showForCaller(this)
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
        private const val DIALOG_REQUEST_INFO = 2
        private const val DIALOG_REQUEST_DELETE = 3
        private const val OPERATION_INFO = 0
        private const val OPERATION_DELETE = 1
    }
}
