package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller

class ChoiceDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var mItems: Array<String>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        mItems = requireArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS)
        val choice = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS)
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setSingleChoiceItems(mItems, choice, null)
            .setPositiveButton(R.string.dialog_positive, this)
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val index = (dialogInterface as AlertDialog).listView.checkedItemPosition
        val value = if (index == -1) null else mItems?.get(index)
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX, index)
        bundle.putString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, value)
        val target = DialogCaller.from(this)
        target?.onDialogResult(requestCode, bundle)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, item: Array<String>?, choice: Int, requestCode: Int): ChoiceDialogFragment {
            val fragment = ChoiceDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS, choice)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
