package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller

class MultiDialogFragment : DialogFragment(), DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {

    private var mCheckArray: BooleanArray? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        var item = requireArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS)
        if (item == null) {
            item = arrayOf()
        }
        initCheckArray(item.size)
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setMultiChoiceItems(item, mCheckArray, this)
            .setPositiveButton(R.string.dialog_positive, this)
        return builder.create()
    }

    private fun initCheckArray(length: Int) {
        mCheckArray = requireArguments().getBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS)
        if (mCheckArray == null) {
            mCheckArray = BooleanArray(length)
            for (i in 0 until length) {
                mCheckArray!![i] = false
            }
        }
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int, check: Boolean) {
        mCheckArray!![which] = check
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, mCheckArray)
        val target = DialogCaller.from(this)
        target?.onDialogResult(requestCode, bundle)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, item: Array<String>?, check: BooleanArray?, requestCode: Int): MultiDialogFragment {
            val fragment = MultiDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item)
            bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS, check)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
