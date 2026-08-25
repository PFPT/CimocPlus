package com.haleydu.cimoc.ui.common.dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.DialogProgressBinding
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.utils.ThemeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProgressDialogFragment : DialogFragment() {

    private var progressBar: ProgressBar? = null
    private var textView: TextView? = null
    private var collectJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogProgressBinding.inflate(inflater, container, false)
        progressBar = binding.dialogProgressBar
        textView = binding.dialogProgressText
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        isCancelable = false
        val resId = ThemeUtils.getResourceId(activity, R.attr.colorAccent)
        progressBar?.indeterminateDrawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            ContextCompat.getColor(requireActivity(), resId),
            BlendModeCompat.SRC_ATOP
        )
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            AppEventBus.observe(AppEvent.EVENT_DIALOG_PROGRESS).collect { event ->
                textView?.text = event.getData() as String
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        collectJob?.cancel()
        collectJob = null
        progressBar = null
        textView = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(): ProgressDialogFragment = ProgressDialogFragment()
    }
}
