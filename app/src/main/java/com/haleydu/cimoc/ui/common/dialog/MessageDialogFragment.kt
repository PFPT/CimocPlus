package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller

class MessageDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        var content = requireArguments().getString(DialogCaller.EXTRA_DIALOG_CONTENT_TEXT)
        if (content == null) {
            content = getString(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_CONTENT))
        }
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setMessage(content)
            .setPositiveButton(R.string.dialog_positive, this)
        if (requireArguments().getBoolean(DialogCaller.EXTRA_DIALOG_NEGATIVE, false)) {
            isCancelable = true
            builder.setNegativeButton(R.string.dialog_negative, null)
        } else {
            isCancelable = false
        }
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val target = DialogCaller.from(this)
        target?.onDialogResult(requestCode, Bundle())
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, content: Int, negative: Boolean, requestCode: Int): MessageDialogFragment {
            val fragment = MessageDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_CONTENT, content)
            bundle.putBoolean(DialogCaller.EXTRA_DIALOG_NEGATIVE, negative)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun newInstance(title: Int, content: String?, negative: Boolean, requestCode: Int): MessageDialogFragment {
            val fragment = MessageDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putString(DialogCaller.EXTRA_DIALOG_CONTENT_TEXT, content)
            bundle.putBoolean(DialogCaller.EXTRA_DIALOG_NEGATIVE, negative)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
