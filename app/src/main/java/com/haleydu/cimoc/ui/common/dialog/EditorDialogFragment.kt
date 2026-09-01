package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.DialogEditorBinding

open class EditorDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    protected var mEditText: EditText? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogEditorBinding.inflate(requireActivity().layoutInflater)
        val view = binding.root
        mEditText = binding.dialogEditorText
        mEditText?.setText(requireArguments().getString(DialogCaller.EXTRA_DIALOG_CONTENT))
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setView(view)
            .setPositiveButton(R.string.dialog_positive, this)
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, mEditText?.text?.toString())
        val target = DialogCaller.from(this)
        target?.onDialogResult(requestCode, bundle)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, content: String?, requestCode: Int): EditorDialogFragment {
            val fragment = EditorDialogFragment()
            fragment.arguments = createBundle(title, content, requestCode)
            return fragment
        }

        @JvmStatic
        fun createBundle(title: Int, content: String?, requestCode: Int): Bundle {
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putString(DialogCaller.EXTRA_DIALOG_CONTENT, content)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            return bundle
        }
    }
}
