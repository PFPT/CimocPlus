package com.haleydu.cimoc.event

import androidx.annotation.IntDef

class AppEvent(@EventType val type: Int, vararg data: Any?) {

    private val data: Array<out Any?> = data

    fun getData(): Any? = getData(0)

    fun getData(index: Int): Any? = if (index < data.size) data[index] else null

    @IntDef(
        EVENT_COMIC_FAVORITE, EVENT_COMIC_UNFAVORITE, EVENT_COMIC_READ, EVENT_COMIC_UPDATE, EVENT_COMIC_FAVORITE_RESTORE,
        EVENT_COMIC_HISTORY_RESTORE, EVENT_COMIC_CANCEL_HIGHLIGHT, EVENT_COMIC_UPDATE_INFO, EVENT_TASK_STATE_CHANGE, EVENT_TASK_PROCESS,
        EVENT_TASK_INSERT, EVENT_DOWNLOAD_REMOVE, EVENT_DOWNLOAD_START, EVENT_DOWNLOAD_STOP, EVENT_TAG_UPDATE,
        EVENT_TAG_RESTORE, EVENT_DIALOG_PROGRESS, EVENT_DOWNLOAD_CLEAR, EVENT_PICTURE_PAGING, EVENT_SWITCH_NIGHT
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class EventType

    companion object {
        const val EVENT_COMIC_FAVORITE = 1
        const val EVENT_COMIC_UNFAVORITE = 2
        const val EVENT_COMIC_READ = 3
        const val EVENT_COMIC_UPDATE = 4
        const val EVENT_COMIC_FAVORITE_RESTORE = 5
        const val EVENT_COMIC_HISTORY_RESTORE = 6
        const val EVENT_COMIC_CANCEL_HIGHLIGHT = 7
        const val EVENT_COMIC_UPDATE_INFO = 8

        const val EVENT_TASK_STATE_CHANGE = 21
        const val EVENT_TASK_PROCESS = 22
        const val EVENT_TASK_INSERT = 23

        const val EVENT_DOWNLOAD_REMOVE = 41
        const val EVENT_DOWNLOAD_START = 42
        const val EVENT_DOWNLOAD_STOP = 43
        const val EVENT_DOWNLOAD_CLEAR = 44

        const val EVENT_TAG_UPDATE = 81
        const val EVENT_TAG_RESTORE = 82

        const val EVENT_DIALOG_PROGRESS = 101

        const val EVENT_PICTURE_PAGING = 121

        const val EVENT_SWITCH_NIGHT = 141
    }
}
