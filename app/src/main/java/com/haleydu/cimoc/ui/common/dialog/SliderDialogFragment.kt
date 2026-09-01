package com.haleydu.cimoc.ui.common.dialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.DialogSliderBinding
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class SliderDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var mSeekBar: DiscreteSeekBar? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSliderBinding.inflate(requireActivity().layoutInflater)
        val view = binding.root
        val item = requireArguments().getIntArray(DialogCaller.EXTRA_DIALOG_ITEMS)!!
        mSeekBar = binding.dialogSliderBar
        mSeekBar!!.setMin(item[0])
        mSeekBar!!.setMax(item[1])
        mSeekBar!!.progress = item[1]
        mSeekBar!!.progress = item[2]
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setView(view)
            .setPositiveButton(R.string.dialog_positive, this)
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
        val bundle = Bundle()
        bundle.putInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, mSeekBar!!.progress)
        val target = DialogCaller.from(this)
        target?.onDialogResult(requestCode, bundle)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, min: Int, max: Int, progress: Int, requestCode: Int): SliderDialogFragment {
            val fragment = SliderDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putIntArray(DialogCaller.EXTRA_DIALOG_ITEMS, intArrayOf(min, max, progress))
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
