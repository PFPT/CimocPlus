package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.DialogReaderColorBinding
import com.haleydu.cimoc.manager.PreferenceManager
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

fun interface OnColorChanged {
    fun onChanged()
}

class ReaderColorSheet(
    context: Context,
    private val preference: PreferenceManager,
    private val onChanged: OnColorChanged
) : BottomSheetDialog(context, R.style.ReaderBottomSheetDialog) {

    init {
        val binding = DialogReaderColorBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        behavior.skipCollapsed = true
        bindBar(binding.readerColorRed, PreferenceManager.PREF_READER_COLOR_RED)
        bindBar(binding.readerColorGreen, PreferenceManager.PREF_READER_COLOR_GREEN)
        bindBar(binding.readerColorBlue, PreferenceManager.PREF_READER_COLOR_BLUE)
        binding.readerColorInvert.isChecked =
            preference.getBoolean(PreferenceManager.PREF_READER_COLOR_INVERT, false)
        binding.readerColorGray.isChecked =
            preference.getBoolean(PreferenceManager.PREF_READER_COLOR_GRAY, false)
        binding.readerColorInvert.setOnCheckedChangeListener { _, checked ->
            preference.putBoolean(PreferenceManager.PREF_READER_COLOR_INVERT, checked)
            onChanged.onChanged()
        }
        binding.readerColorGray.setOnCheckedChangeListener { _, checked ->
            preference.putBoolean(PreferenceManager.PREF_READER_COLOR_GRAY, checked)
            onChanged.onChanged()
        }
    }

    private fun bindBar(bar: DiscreteSeekBar, key: String) {
        bar.progress = preference.getInt(key, 100)
        bar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    preference.putInt(key, value)
                    onChanged.onChanged()
                }
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) = Unit
        })
    }
}
