package com.haleydu.cimoc.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object StringUtils {

    @JvmStatic
    fun endWith(str: String?, vararg args: String): Boolean {
        if (str != null) {
            for (arg in args) {
                if (str.endsWith(arg)) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun filter(str: String?): String? {
        if (str == null) {
            return null
        }
        return str.replace("\\|\\\\\\?\\*<\":\\+\\[\\]/'".toRegex(), "")
    }

    @JvmStatic
    fun isEmpty(vararg args: String?): Boolean {
        for (arg in args) {
            if (arg == null || arg.isEmpty()) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun split(str: String?, regex: String, position: Int): String? {
        if (str == null) {
            return null
        }
        val array = Pattern.compile(regex).split(str)
        var pos = position
        if (pos < 0) {
            pos = array.size + pos
        }
        return if (pos < 0 || pos >= array.size) null else array[pos]
    }

    @JvmStatic
    fun replaceAll(str: String?, regex: String, replacement: String): String? {
        if (str == null) {
            return null
        }
        return str.replace(regex.toRegex(), replacement)
    }

    @JvmStatic
    fun substring(str: String?, start: Int): String? {
        return substring(str, start, -1)
    }

    @JvmStatic
    fun substring(str: String?, start: Int, end: Int): String? {
        if (str == null) {
            return null
        }
        var endIndex = end
        if (endIndex < 0) {
            endIndex = str.length + 1 + endIndex
        }
        if (start >= 0 && start <= str.length) {
            return str.substring(start, endIndex)
        }
        return null
    }

    @JvmStatic
    fun format(format: String, vararg args: Any?): String {
        return String.format(Locale.getDefault(), format, *args)
    }

    @JvmStatic
    fun getProgress(progress: Int, max: Int): String {
        return format("%d/%d", progress, max)
    }

    @JvmStatic
    fun getFormatTime(format: String, time: Long): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(time))
    }

    @JvmStatic
    fun getDateStringWithSuffix(suffix: String): String {
        return SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()).plus(".").plus(suffix)
    }

    @JvmStatic
    fun match(regex: String, input: String, group: Int): String? {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(input)
            if (matcher.find()) {
                return matcher.group(group)!!.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun match(regex: String, input: String, vararg group: Int): Array<String>? {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(input)
            if (matcher.find()) {
                val result = arrayOfNulls<String>(group.size)
                for (i in result.indices) {
                    result[i] = matcher.group(group[i])
                }
                @Suppress("UNCHECKED_CAST")
                return result as Array<String>
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
