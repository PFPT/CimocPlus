package com.haleydu.cimoc.ui.reader

import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.global.ClickEvents
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class ReaderOverlayManager(
    private val window: Window,
    private val preference: PreferenceManager,
    private val nightMask: View?
) {
    var hideNav: Boolean = false
        private set
    var showTopbar: Boolean = false
        private set

    fun init() {
        hideNav = preference.getBoolean(PreferenceManager.PREF_READER_HIDE_NAV, false)
        showTopbar = preference.getBoolean(PreferenceManager.PREF_OTHER_SHOW_TOPBAR, false)
        if (preference.getBoolean(PreferenceManager.PREF_READER_KEEP_BRIGHT, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        applySavedBrightness(null)
        applyNight()
    }

    fun applySystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        when {
            hideNav -> {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            showTopbar -> controller.show(WindowInsetsCompat.Type.systemBars())
            else -> {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    fun applyBrightness(value: Int, bar: DiscreteSeekBar?) {
        preference.putInt(PreferenceManager.PREF_READER_BRIGHTNESS, value)
        val lp = window.attributes
        lp.screenBrightness = (value / 100f).coerceAtLeast(0.01f)
        window.attributes = lp
        if (bar != null && bar.progress != value) {
            bar.progress = value
        }
    }

    fun applySavedBrightness(bar: DiscreteSeekBar?) {
        val brightness = preference.getInt(PreferenceManager.PREF_READER_BRIGHTNESS, 0)
        if (brightness > 0) {
            applyBrightness(brightness, bar)
        } else if (bar != null) {
            bar.progress = 50
        }
    }

    fun applyNight() {
        val night = preference.getBoolean(PreferenceManager.PREF_NIGHT, false)
        nightMask?.visibility = if (night) View.VISIBLE else View.INVISIBLE
    }

    fun switchNight(): Boolean {
        val night = !preference.getBoolean(PreferenceManager.PREF_NIGHT, false)
        preference.putBoolean(PreferenceManager.PREF_NIGHT, night)
        applyNight()
        return night
    }

    fun mapKey(keyCode: Int, clickArray: IntArray): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> clickArray.getOrElse(5) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_VOLUME_DOWN -> clickArray.getOrElse(6) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 ->
                clickArray.getOrElse(7) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 ->
                clickArray.getOrElse(8) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_A -> clickArray.getOrElse(14) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_B -> clickArray.getOrElse(13) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_X -> clickArray.getOrElse(15) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_BUTTON_Y -> clickArray.getOrElse(16) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_DPAD_LEFT -> clickArray.getOrElse(9) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_DPAD_RIGHT -> clickArray.getOrElse(10) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_DPAD_UP -> clickArray.getOrElse(11) { ClickEvents.EVENT_NULL }
            KeyEvent.KEYCODE_DPAD_DOWN -> clickArray.getOrElse(12) { ClickEvents.EVENT_NULL }
            else -> ClickEvents.EVENT_NULL
        }
    }
}
