package com.haleydu.cimoc.utils

import android.util.Log

object LogUtil {

    @JvmField
    var isDebug = true
    private const val TAG = "Haleydu"

    @JvmStatic
    fun i(msg: String) {
        if (isDebug) Log.i(TAG, msg)
    }

    @JvmStatic
    fun d(msg: String) {
        if (isDebug) Log.d(TAG, msg)
    }

    @JvmStatic
    fun e(msg: String) {
        if (isDebug) Log.e(TAG, msg)
    }

    @JvmStatic
    fun v(msg: String) {
        if (isDebug) Log.v(TAG, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    @JvmStatic
    fun v(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    @JvmStatic
    fun eLength(tag: String?, msg: String?) {
        if (isDebug) {
            var remaining = msg
            if (tag == null || tag.isEmpty() || remaining == null || remaining.isEmpty()) return
            val segmentSize = 3 * 1024
            val length = remaining.length.toLong()
            if (length <= segmentSize) {
                Log.e(tag, remaining)
            } else {
                while (remaining!!.length > segmentSize) {
                    val logContent = remaining.substring(0, segmentSize)
                    remaining = remaining.replace(logContent, "")
                    Log.e(tag, logContent)
                }
                Log.e(tag, remaining)
            }
        }
    }

    @JvmStatic
    fun iLength(tag: String?, msg: String?) {
        if (isDebug) {
            var remaining = msg
            if (tag == null || tag.isEmpty() || remaining == null || remaining.isEmpty()) return
            val segmentSize = 3 * 1024
            val length = remaining.length.toLong()
            if (length <= segmentSize) {
                Log.i(tag, remaining)
            } else {
                while (remaining!!.length > segmentSize) {
                    val logContent = remaining.substring(0, segmentSize)
                    remaining = remaining.replace(logContent, "")
                    Log.i(tag, logContent)
                }
                Log.i(tag, remaining)
            }
        }
    }
}
