package com.haleydu.cimoc.ui.settings
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.data.PreferenceManager.READER_ORIENTATION_AUTO
import com.haleydu.cimoc.databinding.ActivityEventBinding
import com.haleydu.cimoc.global.ClickEvents
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.ui.common.BaseActivity
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment

class EventSettingsActivity : BaseActivity(), DialogCaller {

    lateinit var mButtonList: List<Button>
    private lateinit var binding: ActivityEventBinding

    private lateinit var mChoiceArray: IntArray
    private lateinit var mKeyArray: Array<String>
    private var isLong = false

    private val JoyLock = booleanArrayOf(false, false)
    private val JoyEvent = intArrayOf(7, 8)
    private val thredhold = 0.3f

    override fun initTheme() {
        super.initTheme()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val oArray = intArrayOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        )
        val value = oArray[intent.getIntExtra(Extra.EXTRA_IS_PORTRAIT, READER_ORIENTATION_AUTO)]
        requestedOrientation = value
    }

    override fun initView() {
        val isStream = intent.getBooleanExtra(Extra.EXTRA_IS_STREAM, false)
        isLong = intent.getBooleanExtra(Extra.EXTRA_IS_LONG, false)
        if (isStream) {
            mKeyArray = if (isLong) ClickEvents.getStreamLongClickEvents() else ClickEvents.getStreamClickEvents()
            mChoiceArray = if (isLong) ClickEvents.getStreamLongClickEventChoice(mPreference)
            else ClickEvents.getStreamClickEventChoice(mPreference)
        } else {
            mKeyArray = if (isLong) ClickEvents.getPageLongClickEvents() else ClickEvents.getPageClickEvents()
            mChoiceArray = if (isLong) ClickEvents.getPageLongClickEventChoice(mPreference)
            else ClickEvents.getPageClickEventChoice(mPreference)
        }

        for (i in 0 until 5) {
            val index = i
            mButtonList[i].text = ClickEvents.getEventTitle(this, mChoiceArray[i])
            mButtonList[i].setOnClickListener { showEventList(index) }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isLong) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    showEventList(5)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    showEventList(6)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> {
                    showEventList(7)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> {
                    showEventList(8)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_A -> {
                    showEventList(14)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    showEventList(13)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_X -> {
                    showEventList(15)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_Y -> {
                    showEventList(16)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    showEventList(9)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    showEventList(10)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    showEventList(11)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    showEventList(12)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val historySize = event.historySize
            for (i in 0 until historySize) {
                processJoystickInput(event, i)
            }
            processJoystickInput(event, -1)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun checkKey(value: Float, joy: ClickEvents.JoyLocks) {
        if (JoyLock[joy.ordinal] && value < thredhold) {
            JoyLock[joy.ordinal] = false
        }
        if (!JoyLock[joy.ordinal] && value > thredhold) {
            JoyLock[joy.ordinal] = true
            showEventList(JoyEvent[joy.ordinal])
        }
    }

    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {
        checkKey(event.getAxisValue(MotionEvent.AXIS_GAS), ClickEvents.JoyLocks.RT)
        checkKey(event.getAxisValue(MotionEvent.AXIS_BRAKE), ClickEvents.JoyLocks.LT)
    }

    private fun showEventList(index: Int) {
        val fragment = ChoiceDialogFragment.newInstance(
            R.string.event_select,
            ClickEvents.getEventTitleArray(this),
            mChoiceArray[index],
            index
        )
        fragment.show(supportFragmentManager, null)
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
        mChoiceArray[requestCode] = index
        mPreference.putInt(mKeyArray[requestCode], index)
        if (requestCode < 5) {
            mButtonList[requestCode].text = ClickEvents.getEventTitle(this, index)
        }
    }

    override fun inflateContentView(): View {
        binding = ActivityEventBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_event
    }

    override fun bindViews() {
        super.bindViews()
        mButtonList = listOf(
            binding.eventLeft,
            binding.eventTop,
            binding.eventMiddle,
            binding.eventBottom,
            binding.eventRight
        )
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, isLong: Boolean, orientation: Int, isStream: Boolean): Intent {
            val intent = Intent(context, EventSettingsActivity::class.java)
            intent.putExtra(Extra.EXTRA_IS_LONG, isLong)
            intent.putExtra(Extra.EXTRA_IS_PORTRAIT, orientation)
            intent.putExtra(Extra.EXTRA_IS_STREAM, isStream)
            return intent
        }
    }
}
