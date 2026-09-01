package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.component.DialogCaller

class ItemDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val item = requireArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS)
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setItems(item, this)
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX, which)
        val target = resolveCaller()
        target?.onDialogResult(requestCode, bundle)
    }

    private fun resolveCaller(): DialogCaller? {
        return DialogCaller.from(this)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, item: Array<String>?, requestCode: Int): ItemDialogFragment {
            val fragment = ItemDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
