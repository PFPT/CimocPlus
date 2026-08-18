package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.DialogReaderAutoBinding
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

fun interface OnAutoIntervalConfirm {
    fun onConfirm(seconds: Int)
}

object ReaderAutoDialog {

    @JvmStatic
    fun show(context: Context, current: Int, onConfirm: OnAutoIntervalConfirm) {
        val binding = DialogReaderAutoBinding.inflate(LayoutInflater.from(context))
        val value = current.coerceIn(2, 30)
        binding.readerAutoInterval.progress = value
        binding.readerAutoValue.text = context.getString(R.string.reader_auto_interval_value, value)
        binding.readerAutoInterval.setOnProgressChangeListener(
            object : DiscreteSeekBar.OnProgressChangeListener {
                override fun onProgressChanged(
                    seekBar: DiscreteSeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.readerAutoValue.text =
                        context.getString(R.string.reader_auto_interval_value, progress)
                }

                override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) = Unit
            }
        )
        AlertDialog.Builder(context)
            .setTitle(R.string.reader_auto_interval)
            .setView(binding.root)
            .setPositiveButton(R.string.dialog_positive) { _, _ ->
                onConfirm.onConfirm(binding.readerAutoInterval.progress)
            }
            .setNegativeButton(R.string.dialog_negative, null)
            .show()
    }
}
