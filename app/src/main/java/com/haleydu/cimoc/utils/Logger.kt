package com.haleydu.cimoc.utils

import android.util.Log
import java.util.Locale

object Logger {
    private var isShow = true
    private val TAG = Logger::class.java.simpleName
    private const val PRE = "###"
    private const val SPACE = "===================================================================================================="
    private const val MAX_LENGTH = 1024

    @JvmStatic
    fun init(isShowLog: Boolean) {
        isShow = isShowLog
    }

    @JvmStatic
    fun v(tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        printLargeLog(Level.V, formattedTag, message)
    }

    @JvmStatic
    fun v(throwable: Throwable, tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        Log.v(formattedTag, message, throwable)
    }

    @JvmStatic
    fun d(tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        printLargeLog(Level.D, formattedTag, message)
    }

    @JvmStatic
    fun d(throwable: Throwable, tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        Log.d(formattedTag, message, throwable)
    }

    @JvmStatic
    fun i(tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        printLargeLog(Level.I, formattedTag, message)
    }

    @JvmStatic
    fun i(throwable: Throwable, tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        Log.i(formattedTag, message, throwable)
    }

    @JvmStatic
    fun w(tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        printLargeLog(Level.W, formattedTag, message)
    }

    @JvmStatic
    fun w(throwable: Throwable, tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        Log.w(formattedTag, message, throwable)
    }

    @JvmStatic
    fun e(tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        printLargeLog(Level.E, formattedTag, message)
    }

    @JvmStatic
    fun e(throwable: Throwable, tag: String, format: String, vararg args: Any?) {
        val message = buildMessage(format, args)
        val formattedTag = formatLength(PRE + tag, 28)
        Log.e(formattedTag, message, throwable)
    }

    private fun printLog(level: Level, tag: String, message: String) {
        when (level) {
            Level.V -> Log.v(tag, message)
            Level.D -> Log.d(tag, message)
            Level.I -> Log.i(tag, message)
            Level.W -> Log.w(tag, message)
            Level.E -> Log.e(tag, message)
        }
    }

    private fun printLargeLog(level: Level, tag: String, message: String) {
        if (!isShow) {
            return
        }
        val length = message.length
        val size = if (length % MAX_LENGTH == 0) length / MAX_LENGTH else length / MAX_LENGTH + 1
        for (i in 0 until size) {
            val end = if ((i + 1) * MAX_LENGTH > length) length else (i + 1) * MAX_LENGTH
            val subMessage = message.substring(i * MAX_LENGTH, end)
            printLog(level, tag, subMessage)
        }
    }

    private fun buildMessage(format: String, args: Array<out Any?>): String {
        try {
            val msg = if (args.isEmpty()) format else String.format(Locale.getDefault(), format, *args)
            if (!isShow) {
                return msg
            }
            val trace = Throwable().fillInStackTrace().stackTrace
            var caller = ""
            var callingClass = ""
            var callFile = ""
            var lineNumber = 0
            for (i in 2 until trace.size) {
                val clazz = trace[i].javaClass
                if (clazz != Logger::class.java) {
                    callingClass = trace[i].className
                    callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1)
                    caller = trace[i].methodName
                    callFile = trace[i].fileName ?: ""
                    lineNumber = trace[i].lineNumber
                    break
                }
            }
            val method = String.format(
                Locale.getDefault(),
                "[%03d] %s.%s(%s:%d)",
                Thread.currentThread().id,
                callingClass,
                caller,
                callFile,
                lineNumber
            )
            return String.format(Locale.getDefault(), "%s> %s", formatLength(method, 93), msg)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "")
        }
        return "----->ERROR LOG STRING<------"
    }

    private fun formatLength(src: String, len: Int): String {
        val sb = StringBuilder()
        if (src.length >= len) {
            sb.append(src)
        } else {
            sb.append(src)
            sb.append(SPACE.substring(0, len - src.length))
        }
        return sb.toString()
    }

    private enum class Level {
        V, D, I, W, E
    }
}
