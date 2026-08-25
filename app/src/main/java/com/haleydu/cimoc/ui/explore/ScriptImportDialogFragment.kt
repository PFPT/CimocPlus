package com.haleydu.cimoc.ui.explore
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller

class ScriptImportDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val padding = (20 * resources.displayMetrics.density).toInt()
        editText = EditText(requireContext())
        editText.hint = getString(R.string.source_import_hint)
        editText.minLines = 8
        editText.gravity = android.view.Gravity.TOP
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(padding, padding / 2, padding, padding / 2)
        val container = FrameLayout(requireContext())
        container.addView(editText, params)
        return AlertDialog.Builder(requireActivity())
            .setTitle(R.string.source_import_script)
            .setView(container)
            .setPositiveButton(R.string.dialog_positive, this)
            .setNegativeButton(R.string.dialog_negative, null)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, editText.text.toString())
        DialogCaller.from(this)?.onDialogResult(requestCode, bundle)
    }

    companion object {
        fun newInstance(requestCode: Int): ScriptImportDialogFragment {
            val fragment = ScriptImportDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
