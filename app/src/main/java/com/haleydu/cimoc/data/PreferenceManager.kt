package com.haleydu.cimoc.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val mSharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getString(key: String): String? = getString(key, null)

    fun getString(key: String, defValue: String?): String? = mSharedPreferences.getString(key, defValue)

    fun getBoolean(key: String, defValue: Boolean): Boolean = mSharedPreferences.getBoolean(key, defValue)

    fun getInt(key: String, defValue: Int): Int = mSharedPreferences.getInt(key, defValue)

    fun getLong(key: String, defValue: Long): Long = mSharedPreferences.getLong(key, defValue)

    fun putString(key: String, value: String?) {
        mSharedPreferences.edit().putString(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        mSharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        mSharedPreferences.edit().putInt(key, value).apply()
    }

    fun putLong(key: String, value: Long) {
        mSharedPreferences.edit().putLong(key, value).apply()
    }

    fun getAll(): Map<String, *> = mSharedPreferences.all

    fun putObject(key: String, value: Any?) {
        when (value) {
            is Boolean -> mSharedPreferences.edit().putBoolean(key, value).apply()
            is Float -> mSharedPreferences.edit().putFloat(key, value).apply()
            is Int -> mSharedPreferences.edit().putInt(key, value).apply()
            is Long -> mSharedPreferences.edit().putLong(key, value).apply()
            is String -> mSharedPreferences.edit().putString(key, value).apply()
        }
    }

    companion object {
        const val READER_MODE_PAGE = 0
        const val READER_MODE_STREAM = 1

        const val READER_TURN_LTR = 0
        const val READER_TURN_RTL = 1
        const val READER_TURN_ATB = 2

        const val READER_ORIENTATION_PORTRAIT = 0
        const val READER_ORIENTATION_LANDSCAPE = 1
        const val READER_ORIENTATION_AUTO = 2

        const val HOME_FAVORITE = 0
        const val HOME_SOURCE = 1
        const val HOME_TAG = 2
        const val HOME_HISTORY = 3
        const val HOME_DOWNLOAD = 4
        const val HOME_LOCAL = 5
        const val HOME_EXPLORE = 6

        const val PREF_APP_VERSION = "pref_app_version"

        const val PREF_MAIN_NOTICE = "pref_main_notice"
        const val PREF_MAIN_NOTICE_LAST = "pref_main_notice_last"

        const val PREF_READER_MODE = "pref_reader_mode"
        const val PREF_READER_KEEP_BRIGHT = "pref_reader_keep_on"
        const val PREF_READER_HIDE_INFO = "pref_reader_hide"
        const val PREF_READER_HIDE_NAV = "pref_reader_hide_nav"
        const val PREF_READER_BAN_DOUBLE_CLICK = "pref_reader_ban_double_click"
        const val PREF_READER_PAGING = "pref_reader_paging"
        const val PREF_READER_CLOSEAUTORESIZEIMAGE = "pref_reader_closeautoresizeimage "
        const val PREF_READER_PAGING_REVERSE = "pref_reader_paging_reverse"
        const val PREF_READER_WHITE_EDGE = "pref_reader_white_edge"
        const val PREF_READER_WHITE_BACKGROUND = "pref_reader_white_background"
        const val PREF_READER_SCALE_FACTOR = "pref_reader_scale_factor"
        const val PREF_READER_CONTROLLER_TRIG_THRESHOLD = "pref_reader_controller_trig_threshold"
        const val PREF_READER_VOLUME_KEY_CONTROLS_PAGE_TURNING = "pref_reader_volume_key_controls_page_turning"
        const val PREF_READER_PRELOAD = "pref_reader_preload"
        const val PREF_READER_PRELOAD_COUNT = "pref_reader_preload_count"

        const val PREF_READER_PAGE_TURN = "pref_reader_page_turn"
        const val PREF_READER_PAGE_ORIENTATION = "pref_reader_page_orientation"
        const val PREF_READER_PAGE_CLICK_LEFT = "pref_reader_page_click_left"
        const val PREF_READER_PAGE_CLICK_TOP = "pref_reader_page_click_top"
        const val PREF_READER_PAGE_CLICK_MIDDLE = "pref_reader_page_click_middle"
        const val PREF_READER_PAGE_CLICK_BOTTOM = "pref_reader_page_click_bottom"
        const val PREF_READER_PAGE_CLICK_RIGHT = "pref_reader_page_click_right"
        const val PREF_READER_PAGE_CLICK_UP = "pref_reader_page_click_up"
        const val PREF_READER_PAGE_CLICK_DOWN = "pref_reader_page_click_down"
        const val PREF_READER_PAGE_JOY_LT = "pref_reader_page_joy_lt"
        const val PREF_READER_PAGE_JOY_RT = "pref_reader_page_joy_rt"
        const val PREF_READER_PAGE_JOY_A = "pref_reader_page_joy_a"
        const val PREF_READER_PAGE_JOY_B = "pref_reader_page_joy_b"
        const val PREF_READER_PAGE_JOY_X = "pref_reader_page_joy_x"
        const val PREF_READER_PAGE_JOY_Y = "pref_reader_page_joy_y"
        const val PREF_READER_PAGE_JOY_LEFT = "pref_reader_page_joy_left"
        const val PREF_READER_PAGE_JOY_RIGHT = "pref_reader_page_joy_right"
        const val PREF_READER_PAGE_JOY_UP = "pref_reader_page_joy_up"
        const val PREF_READER_PAGE_JOY_DOWN = "pref_reader_page_joy_down"
        const val PREF_READER_PAGE_LONG_CLICK_LEFT = "pref_reader_page_long_click_left"
        const val PREF_READER_PAGE_LONG_CLICK_TOP = "pref_reader_page_long_click_top"
        const val PREF_READER_PAGE_LONG_CLICK_MIDDLE = "pref_reader_page_long_click_middle"
        const val PREF_READER_PAGE_LONG_CLICK_BOTTOM = "pref_reader_page_long_click_bottom"
        const val PREF_READER_PAGE_LONG_CLICK_RIGHT = "pref_reader_page_long_click_right"
        const val PREF_READER_PAGE_LOAD_PREV = "pref_reader_page_load_prev"
        const val PREF_READER_PAGE_LOAD_NEXT = "pref_reader_page_load_next"
        const val PREF_READER_PAGE_TRIGGER = "pref_reader_page_trigger"
        const val PREF_READER_PAGE_BAN_TURN = "pref_reader_page_ban_turn"
        const val PREF_READER_PAGE_QUICK_TURN = "pref_reader_page_quick_turn"

        const val PREF_READER_STREAM_TURN = "pref_reader_stream_turn"
        const val PREF_READER_STREAM_ORIENTATION = "pref_reader_stream_orientation"
        const val PREF_READER_STREAM_CLICK_LEFT = "pref_reader_stream_click_left"
        const val PREF_READER_STREAM_CLICK_TOP = "pref_reader_stream_click_top"
        const val PREF_READER_STREAM_CLICK_MIDDLE = "pref_reader_stream_click_middle"
        const val PREF_READER_STREAM_CLICK_BOTTOM = "pref_reader_stream_click_bottom"
        const val PREF_READER_STREAM_CLICK_RIGHT = "pref_reader_stream_click_right"
        const val PREF_READER_STREAM_CLICK_UP = "pref_reader_stream_click_up"
        const val PREF_READER_STREAM_CLICK_DOWN = "pref_reader_stream_click_down"
        const val PREF_READER_STREAM_JOY_LT = "pref_reader_stream_joy_lt"
        const val PREF_READER_STREAM_JOY_RT = "pref_reader_stream_joy_rt"
        const val PREF_READER_STREAM_JOY_A = "pref_reader_stream_joy_a"
        const val PREF_READER_STREAM_JOY_B = "pref_reader_stream_joy_b"
        const val PREF_READER_STREAM_JOY_X = "pref_reader_stream_joy_x"
        const val PREF_READER_STREAM_JOY_Y = "pref_reader_stream_joy_y"
        const val PREF_READER_STREAM_JOY_LEFT = "pref_reader_stream_joy_left"
        const val PREF_READER_STREAM_JOY_RIGHT = "pref_reader_stream_joy_right"
        const val PREF_READER_STREAM_JOY_UP = "pref_reader_stream_joy_up"
        const val PREF_READER_STREAM_JOY_DOWN = "pref_reader_stream_joy_down"
        const val PREF_READER_STREAM_LONG_CLICK_LEFT = "pref_reader_stream_long_click_left"
        const val PREF_READER_STREAM_LONG_CLICK_TOP = "pref_reader_stream_long_click_top"
        const val PREF_READER_STREAM_LONG_CLICK_MIDDLE = "pref_reader_stream_long_click_middle"
        const val PREF_READER_STREAM_LONG_CLICK_BOTTOM = "pref_reader_stream_long_click_bottom"
        const val PREF_READER_STREAM_LONG_CLICK_RIGHT = "pref_reader_stream_long_click_right"
        const val PREF_READER_STREAM_LOAD_PREV = "pref_reader_stream_load_prev"
        const val PREF_READER_STREAM_LOAD_NEXT = "pref_reader_stream_load_next"
        const val PREF_READER_STREAM_INTERVAL = "pref_reader_stream_interval"

        const val PREF_READER_AUTO_INTERVAL = "pref_reader_auto_interval"
        const val PREF_READER_BRIGHTNESS = "pref_reader_brightness"
        const val PREF_READER_COLOR_RED = "pref_reader_color_red"
        const val PREF_READER_COLOR_GREEN = "pref_reader_color_green"
        const val PREF_READER_COLOR_BLUE = "pref_reader_color_blue"
        const val PREF_READER_COLOR_INVERT = "pref_reader_color_invert"
        const val PREF_READER_COLOR_GRAY = "pref_reader_color_gray"
        const val PREF_READER_INFO_BOTTOM = "pref_reader_info_bottom"
        const val PREF_READER_HIDE_LOAD_TOAST = "pref_reader_hide_load_toast"

        const val PREF_NIGHT = "pref_night"

        const val PREF_UPDATE_APP_AUTO = "pref_update_app_auto"

        const val PREF_UPDATE_CURRENT_URL = "pref_update_current_url"

        const val PREF_OTHER_CHECK_UPDATE = "pref_other_check_update"
        const val PREF_OTHER_CHECK_SOFTWARE_UPDATE = "pref_other_check_software_update"
        const val PREF_OTHER_CONNECT_ONLY_WIFI = "pref_other_connect_only_wifi"
        const val PREF_OTHER_LOADCOVER_ONLY_WIFI = "pref_other_loadcover_only_wifi"
        const val PREF_OTHER_FIREBASE_EVENT = "pref_other_firebase_event"
        const val PREF_OTHER_CHECK_UPDATE_LAST = "pref_other_check_update_last"
        const val PREF_OTHER_STORAGE = "pref_other_storage"
        const val PREF_OTHER_THEME = "pref_other_theme"
        const val PREF_OTHER_LAUNCH = "pref_other_launch"
        const val PREF_EXPLORE_SOURCE = "pref_explore_source"
        const val PREF_OTHER_NIGHT_ALPHA = "pref_other_night_alpha"
        const val PREF_OTHER_SHOW_TOPBAR = "pref_other_show_topbar"

        const val PREF_DOWNLOAD_THREAD = "pref_download_thread"

        const val PREF_BACKUP_SAVE_COMIC = "pref_backup_save_favorite"
        const val PREF_BACKUP_SAVE_COMIC_COUNT = "pref_backup_save_favorite_count"

        const val PREF_SEARCH_AUTO_COMPLETE = "pref_search_auto_complete"
        const val PREF_SEARCH_HISTORY = "pref_search_history"
        const val PREF_SEARCH_STRICT = "pref_search_strict"

        const val PREF_CHAPTER_BUTTON_MODE = "pref_chapter_button_mode"
        const val PREF_CHAPTER_ASCEND_MODE = "pref_chapter_ascend_mode"
        const val PREFERENCES_USER_TOCKEN = "user_tocken"
        const val PREFERENCES_USER_NAME = "user_name"
        const val PREFERENCES_USER_EMAIL = "user_email"
        const val PREFERENCES_USER_ID = "user_id"
        private const val PREFERENCES_NAME = "cimoc_preferences"

        const val PREFERENCES_MH50_KEY_MSG = "preferences_mh50_key_msg"
        const val PREFERENCES_MH50_IV_MSG = "preferences_mh50_iv_msg"

        const val PREF_HHAAZZ_BASEURL = "pref_hhaazz_baseurl"
        const val PREF_HHAAZZ_SW = "pref_hhaazz_sw"
        const val PREF_SOURCE_RESTORE_ENABLE = "pref_source_restore_enable_v1"
        const val PREF_SOURCE_CATALOG_REFRESH_AT = "pref_source_catalog_refresh_at"
        const val PREF_SOURCE_CATALOG_VERSION = "pref_source_catalog_version"
    }
}
