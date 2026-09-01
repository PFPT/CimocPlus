package com.haleydu.cimoc.global

import android.content.Context
import com.haleydu.cimoc.R
import com.haleydu.cimoc.data.PreferenceManager

object ClickEvents {

    const val EVENT_NULL = 0
    const val EVENT_PREV_PAGE = 1
    const val EVENT_NEXT_PAGE = 2
    const val EVENT_SAVE_PICTURE = 3
    const val EVENT_LOAD_PREV = 4
    const val EVENT_LOAD_NEXT = 5
    const val EVENT_EXIT_READER = 6
    const val EVENT_TO_FIRST = 7
    const val EVENT_TO_LAST = 8
    const val EVENT_SWITCH_SCREEN = 9
    const val EVENT_SWITCH_MODE = 10
    const val EVENT_SWITCH_CONTROL = 11
    const val EVENT_RELOAD_IMAGE = 12
    const val EVENT_SWITCH_NIGHT = 13

    private var mEventTitle: Array<String>? = null

    enum class JoyLocks {
        LT,
        RT
    }

    @JvmStatic
    fun getPageClickEvents(): Array<String> {
        return arrayOf(
            PreferenceManager.PREF_READER_PAGE_CLICK_LEFT,
            PreferenceManager.PREF_READER_PAGE_CLICK_TOP,
            PreferenceManager.PREF_READER_PAGE_CLICK_MIDDLE,
            PreferenceManager.PREF_READER_PAGE_CLICK_BOTTOM,
            PreferenceManager.PREF_READER_PAGE_CLICK_RIGHT,
            PreferenceManager.PREF_READER_PAGE_CLICK_UP,
            PreferenceManager.PREF_READER_PAGE_CLICK_DOWN,
            PreferenceManager.PREF_READER_PAGE_JOY_LT,
            PreferenceManager.PREF_READER_PAGE_JOY_RT,
            PreferenceManager.PREF_READER_PAGE_JOY_LEFT,
            PreferenceManager.PREF_READER_PAGE_JOY_RIGHT,
            PreferenceManager.PREF_READER_PAGE_JOY_UP,
            PreferenceManager.PREF_READER_PAGE_JOY_DOWN,
            PreferenceManager.PREF_READER_PAGE_JOY_B,
            PreferenceManager.PREF_READER_PAGE_JOY_A,
            PreferenceManager.PREF_READER_PAGE_JOY_X,
            PreferenceManager.PREF_READER_PAGE_JOY_Y
        )
    }

    @JvmStatic
    fun getPageLongClickEvents(): Array<String> {
        return arrayOf(
            PreferenceManager.PREF_READER_PAGE_LONG_CLICK_LEFT,
            PreferenceManager.PREF_READER_PAGE_LONG_CLICK_TOP,
            PreferenceManager.PREF_READER_PAGE_LONG_CLICK_MIDDLE,
            PreferenceManager.PREF_READER_PAGE_LONG_CLICK_BOTTOM,
            PreferenceManager.PREF_READER_PAGE_LONG_CLICK_RIGHT
        )
    }

    @JvmStatic
    fun getPageClickEventChoice(manager: PreferenceManager): IntArray {
        if (!manager.getBoolean(PreferenceManager.PREF_READER_VOLUME_KEY_CONTROLS_PAGE_TURNING, false)) {
            manager.putInt(PreferenceManager.PREF_READER_PAGE_CLICK_UP, EVENT_NULL)
            manager.putInt(PreferenceManager.PREF_READER_PAGE_CLICK_DOWN, EVENT_NULL)
        } else {
            manager.putInt(PreferenceManager.PREF_READER_PAGE_CLICK_UP, EVENT_PREV_PAGE)
            manager.putInt(PreferenceManager.PREF_READER_PAGE_CLICK_DOWN, EVENT_NEXT_PAGE)
        }
        return intArrayOf(
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_LEFT, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_TOP, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_MIDDLE, EVENT_SWITCH_CONTROL),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_BOTTOM, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_RIGHT, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_UP, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_CLICK_DOWN, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_LT, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_RT, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_LEFT, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_RIGHT, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_UP, EVENT_LOAD_PREV),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_DOWN, EVENT_LOAD_NEXT),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_B, EVENT_EXIT_READER),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_A, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_X, EVENT_SWITCH_CONTROL),
            manager.getInt(PreferenceManager.PREF_READER_PAGE_JOY_Y, EVENT_SAVE_PICTURE)
        )
    }

    @JvmStatic
    fun getPageLongClickEventChoice(manager: PreferenceManager): IntArray {
        val array = IntArray(5)
        array[0] = manager.getInt(PreferenceManager.PREF_READER_PAGE_LONG_CLICK_LEFT, 0)
        array[1] = manager.getInt(PreferenceManager.PREF_READER_PAGE_LONG_CLICK_TOP, 0)
        array[2] = manager.getInt(PreferenceManager.PREF_READER_PAGE_LONG_CLICK_MIDDLE, 0)
        array[3] = manager.getInt(PreferenceManager.PREF_READER_PAGE_LONG_CLICK_BOTTOM, 0)
        array[4] = manager.getInt(PreferenceManager.PREF_READER_PAGE_LONG_CLICK_RIGHT, 0)
        return array
    }

    @JvmStatic
    fun getStreamClickEvents(): Array<String> {
        return arrayOf(
            PreferenceManager.PREF_READER_STREAM_CLICK_LEFT,
            PreferenceManager.PREF_READER_STREAM_CLICK_TOP,
            PreferenceManager.PREF_READER_STREAM_CLICK_MIDDLE,
            PreferenceManager.PREF_READER_STREAM_CLICK_BOTTOM,
            PreferenceManager.PREF_READER_STREAM_CLICK_RIGHT,
            PreferenceManager.PREF_READER_STREAM_CLICK_UP,
            PreferenceManager.PREF_READER_STREAM_CLICK_DOWN,
            PreferenceManager.PREF_READER_STREAM_JOY_LT,
            PreferenceManager.PREF_READER_STREAM_JOY_RT,
            PreferenceManager.PREF_READER_STREAM_JOY_LEFT,
            PreferenceManager.PREF_READER_STREAM_JOY_RIGHT,
            PreferenceManager.PREF_READER_STREAM_JOY_UP,
            PreferenceManager.PREF_READER_STREAM_JOY_DOWN,
            PreferenceManager.PREF_READER_STREAM_JOY_B,
            PreferenceManager.PREF_READER_STREAM_JOY_A,
            PreferenceManager.PREF_READER_STREAM_JOY_X,
            PreferenceManager.PREF_READER_STREAM_JOY_Y
        )
    }

    @JvmStatic
    fun getStreamLongClickEvents(): Array<String> {
        return arrayOf(
            PreferenceManager.PREF_READER_STREAM_LONG_CLICK_LEFT,
            PreferenceManager.PREF_READER_STREAM_LONG_CLICK_TOP,
            PreferenceManager.PREF_READER_STREAM_LONG_CLICK_MIDDLE,
            PreferenceManager.PREF_READER_STREAM_LONG_CLICK_BOTTOM,
            PreferenceManager.PREF_READER_STREAM_LONG_CLICK_RIGHT
        )
    }

    @JvmStatic
    fun getStreamClickEventChoice(manager: PreferenceManager): IntArray {
        if (!manager.getBoolean(PreferenceManager.PREF_READER_VOLUME_KEY_CONTROLS_PAGE_TURNING, false)) {
            manager.putInt(PreferenceManager.PREF_READER_STREAM_CLICK_UP, EVENT_NULL)
            manager.putInt(PreferenceManager.PREF_READER_STREAM_CLICK_DOWN, EVENT_NULL)
        } else {
            manager.putInt(PreferenceManager.PREF_READER_STREAM_CLICK_UP, EVENT_PREV_PAGE)
            manager.putInt(PreferenceManager.PREF_READER_STREAM_CLICK_DOWN, EVENT_NEXT_PAGE)
        }
        return intArrayOf(
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_LEFT, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_TOP, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_MIDDLE, EVENT_SWITCH_CONTROL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_BOTTOM, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_RIGHT, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_UP, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_CLICK_DOWN, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_LT, EVENT_PREV_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_RT, EVENT_NEXT_PAGE),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_LEFT, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_RIGHT, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_UP, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_DOWN, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_B, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_A, EVENT_NULL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_X, EVENT_SWITCH_CONTROL),
            manager.getInt(PreferenceManager.PREF_READER_STREAM_JOY_Y, EVENT_SAVE_PICTURE)
        )
    }

    @JvmStatic
    fun getStreamLongClickEventChoice(manager: PreferenceManager): IntArray {
        val array = IntArray(7)
        array[0] = manager.getInt(PreferenceManager.PREF_READER_STREAM_LONG_CLICK_LEFT, 0)
        array[1] = manager.getInt(PreferenceManager.PREF_READER_STREAM_LONG_CLICK_TOP, 0)
        array[2] = manager.getInt(PreferenceManager.PREF_READER_STREAM_LONG_CLICK_MIDDLE, 0)
        array[3] = manager.getInt(PreferenceManager.PREF_READER_STREAM_LONG_CLICK_BOTTOM, 0)
        array[4] = manager.getInt(PreferenceManager.PREF_READER_STREAM_LONG_CLICK_RIGHT, 0)
        return array
    }

    @JvmStatic
    fun getEventTitleArray(context: Context): Array<String> {
        if (mEventTitle == null) {
            mEventTitle = context.resources.getStringArray(R.array.event_items)
        }
        return mEventTitle!!
    }

    @JvmStatic
    fun getEventTitle(context: Context, value: Int): String {
        if (mEventTitle == null) {
            mEventTitle = context.resources.getStringArray(R.array.event_items)
        }
        return mEventTitle!![value]
    }
}
