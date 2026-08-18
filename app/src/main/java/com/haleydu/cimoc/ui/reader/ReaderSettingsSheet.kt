package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.DialogReaderSettingsBinding
import com.haleydu.cimoc.manager.PreferenceManager
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

fun interface OnBrightnessChanged {
    fun onChanged(value: Int)
}

fun interface OnInfoBottomChanged {
    fun onChanged(bottom: Boolean)
}

fun interface OnReaderStyleChanged {
    fun onChanged(mode: Int, turn: Int)
}

fun interface OnWhiteEdgeChanged {
    fun onChanged(enabled: Boolean)
}

fun interface OnStitchChanged {
    fun onChanged(enabled: Boolean)
}

fun interface OnPreloadChanged {
    fun onChanged()
}

class ReaderSettingsSheet(
    context: Context,
    private val preference: PreferenceManager,
    currentMode: Int,
    currentTurn: Int,
    private val onBrightness: OnBrightnessChanged,
    private val onInfoBottom: OnInfoBottomChanged,
    private val onStyle: OnReaderStyleChanged,
    private val onWhiteEdge: OnWhiteEdgeChanged,
    private val onStitch: OnStitchChanged,
    private val onPreload: OnPreloadChanged
) : BottomSheetDialog(context, R.style.ReaderBottomSheetDialog) {

    init {
        val binding = DialogReaderSettingsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        behavior.skipCollapsed = true
        when {
            currentMode == PreferenceManager.READER_MODE_STREAM ->
                binding.readerStyleGroup.check(R.id.reader_style_stream)
            currentTurn == PreferenceManager.READER_TURN_RTL ->
                binding.readerStyleGroup.check(R.id.reader_style_manga)
            else ->
                binding.readerStyleGroup.check(R.id.reader_style_page)
        }
        binding.readerStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.reader_style_page ->
                    onStyle.onChanged(PreferenceManager.READER_MODE_PAGE, PreferenceManager.READER_TURN_LTR)
                R.id.reader_style_stream ->
                    onStyle.onChanged(PreferenceManager.READER_MODE_STREAM, PreferenceManager.READER_TURN_ATB)
                R.id.reader_style_manga ->
                    onStyle.onChanged(PreferenceManager.READER_MODE_PAGE, PreferenceManager.READER_TURN_RTL)
            }
            dismiss()
        }
        binding.readerWhiteEdge.isChecked =
            preference.getBoolean(PreferenceManager.PREF_READER_WHITE_EDGE, false)
        binding.readerWhiteEdge.setOnCheckedChangeListener { _, checked ->
            onWhiteEdge.onChanged(checked)
        }
        binding.readerStitch.isChecked = currentMode == PreferenceManager.READER_MODE_STREAM
        binding.readerStitch.setOnCheckedChangeListener { _, checked ->
            onStitch.onChanged(checked)
            dismiss()
        }
        val preload = preference.getBoolean(PreferenceManager.PREF_READER_PRELOAD, false)
        val preloadCount = preference.getInt(PreferenceManager.PREF_READER_PRELOAD_COUNT, 1)
            .coerceIn(1, 5)
        binding.readerPreload.isChecked = preload
        binding.readerPreloadBar.progress = preloadCount
        fun updatePreloadCount(enabled: Boolean, count: Int) {
            binding.readerPreloadCount.text =
                context.getString(R.string.reader_preload_count, count)
            binding.readerPreloadCount.isEnabled = enabled
            binding.readerPreloadBar.isEnabled = enabled
            binding.readerPreloadCount.alpha = if (enabled) 1f else 0.4f
            binding.readerPreloadBar.alpha = if (enabled) 1f else 0.4f
        }
        updatePreloadCount(preload, preloadCount)
        binding.readerPreload.setOnCheckedChangeListener { _, checked ->
            preference.putBoolean(PreferenceManager.PREF_READER_PRELOAD, checked)
            updatePreloadCount(checked, binding.readerPreloadBar.progress)
            onPreload.onChanged()
        }
        binding.readerPreloadBar.setOnProgressChangeListener(
            object : DiscreteSeekBar.OnProgressChangeListener {
                override fun onProgressChanged(
                    seekBar: DiscreteSeekBar,
                    value: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        preference.putInt(PreferenceManager.PREF_READER_PRELOAD_COUNT, value)
                        updatePreloadCount(binding.readerPreload.isChecked, value)
                        onPreload.onChanged()
                    }
                }

                override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) = Unit
            }
        )
        val brightness = preference.getInt(PreferenceManager.PREF_READER_BRIGHTNESS, 0)
        binding.readerSettingsBrightness.progress = if (brightness == 0) 50 else brightness
        binding.readerSettingsBrightness.setOnProgressChangeListener(
            object : DiscreteSeekBar.OnProgressChangeListener {
                override fun onProgressChanged(
                    seekBar: DiscreteSeekBar,
                    value: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        onBrightness.onChanged(value)
                    }
                }

                override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) = Unit
            }
        )
        val infoBottom = preference.getBoolean(PreferenceManager.PREF_READER_INFO_BOTTOM, false)
        binding.readerInfoGroup.check(
            if (infoBottom) R.id.reader_info_bottom else R.id.reader_info_top
        )
        binding.readerInfoGroup.setOnCheckedChangeListener { _, checkedId ->
            onInfoBottom.onChanged(checkedId == R.id.reader_info_bottom)
        }
    }
}
