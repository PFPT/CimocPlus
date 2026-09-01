package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.haleydu.cimoc.R

class StorageEditorDialogFragment : EditorDialogFragment() {

    interface Host {
        fun launchStoragePicker()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as AlertDialog
        mEditText?.isEnabled = false
        val title = getString(R.string.settings_other_storage_edit_neutral)
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, title) { _, _ ->
            val host = activity
            if (host is Host) {
                host.launchStoragePicker()
            }
        }
        return dialog
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, content: String?, requestCode: Int): StorageEditorDialogFragment {
            val fragment = StorageEditorDialogFragment()
            fragment.arguments = createBundle(title, content, requestCode)
            return fragment
        }
    }
}
