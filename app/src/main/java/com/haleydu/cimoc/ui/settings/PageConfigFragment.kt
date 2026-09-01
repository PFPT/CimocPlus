package com.haleydu.cimoc.ui.settings
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.FragmentPageConfigBinding
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.global.ClickEvents
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment
import com.haleydu.cimoc.ui.widget.preference.CheckBoxPreference
import com.haleydu.cimoc.ui.widget.preference.ChoicePreference
import com.haleydu.cimoc.ui.widget.preference.SliderPreference

class PageConfigFragment : BaseFragment(), DialogCaller {

    lateinit var mReaderLoadPrev: CheckBoxPreference
    lateinit var mReaderLoadNext: CheckBoxPreference
    lateinit var mReaderBanTurn: CheckBoxPreference
    lateinit var mReaderQuickTurn: CheckBoxPreference
    lateinit var mReaderOrientation: ChoicePreference
    lateinit var mReaderTurn: ChoicePreference
    lateinit var mReaderTrigger: SliderPreference

    override fun initView() {
        mReaderLoadPrev.bindPreference(PreferenceManager.PREF_READER_PAGE_LOAD_PREV, true)
        mReaderLoadNext.bindPreference(PreferenceManager.PREF_READER_PAGE_LOAD_NEXT, true)
        mReaderBanTurn.bindPreference(PreferenceManager.PREF_READER_PAGE_BAN_TURN, false)
        mReaderQuickTurn.bindPreference(PreferenceManager.PREF_READER_PAGE_QUICK_TURN, false)
        mReaderOrientation.bindPreference(
            requireActivity().supportFragmentManager, this, PreferenceManager.PREF_READER_PAGE_ORIENTATION,
            PreferenceManager.READER_ORIENTATION_AUTO, R.array.reader_orientation_items, DIALOG_REQUEST_ORIENTATION
        )
        mReaderTurn.bindPreference(
            requireActivity().supportFragmentManager, this, PreferenceManager.PREF_READER_PAGE_TURN,
            PreferenceManager.READER_TURN_LTR, R.array.reader_turn_items, DIALOG_REQUEST_TURN
        )
        mReaderTrigger.bindPreference(
            requireActivity().supportFragmentManager, this, PreferenceManager.PREF_READER_PAGE_TRIGGER, 10,
            R.string.settings_reader_trigger, DIALOG_REQUEST_TRIGGER
        )
    }

    fun onReaderEventClick(view: View) {
        val isLong = view.id == R.id.settings_reader_long_click_event
        val intent = EventSettingsActivity.createIntent(
            requireActivity(), isLong,
            mReaderOrientation.value, false
        )
        startActivity(intent)
    }

    private fun showEventList(index: Int) {
        val mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fragment = ChoiceDialogFragment.newInstance(
                R.string.event_select,
                ClickEvents.getEventTitleArray(requireContext()), mChoiceArray[index], index
            )
            fragment.show(requireActivity().supportFragmentManager, null)
        }
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_ORIENTATION -> mReaderOrientation.value = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
            DIALOG_REQUEST_TURN -> mReaderTurn.value = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
            DIALOG_REQUEST_TRIGGER -> mReaderTrigger.value = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
            DIALOG_REQUEST_OPERATION -> {
                val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
                when (index) {
                    OPERATION_VOLUME_UP -> showEventList(5)
                    OPERATION_VOLUME_DOWN -> showEventList(6)
                }
            }
        }
    }

    override fun getLayoutRes(): Int {
        return R.layout.fragment_page_config
    }

    override fun bindViews(view: View) {
        super.bindViews(view)
        val binding = FragmentPageConfigBinding.bind(view)
        mReaderLoadPrev = binding.settingsReaderLoadPrev
        mReaderLoadNext = binding.settingsReaderLoadNext
        mReaderBanTurn = binding.settingsReaderBanTurn
        mReaderQuickTurn = binding.settingsReaderQuickTurn
        mReaderOrientation = binding.settingsReaderOrientation
        mReaderTurn = binding.settingsReaderTurn
        mReaderTrigger = binding.settingsReaderTrigger
        binding.settingsReaderClickEvent.setOnClickListener { onReaderEventClick(it) }
        binding.settingsReaderLongClickEvent.setOnClickListener { onReaderEventClick(it) }
    }

    companion object {
        private const val DIALOG_REQUEST_ORIENTATION = 0
        private const val DIALOG_REQUEST_TURN = 1
        private const val DIALOG_REQUEST_TRIGGER = 2
        private const val DIALOG_REQUEST_OPERATION = 3
        private const val OPERATION_VOLUME_UP = 0
        private const val OPERATION_VOLUME_DOWN = 1
    }
}
