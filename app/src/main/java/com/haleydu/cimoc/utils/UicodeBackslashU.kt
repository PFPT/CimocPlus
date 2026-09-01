package com.haleydu.cimoc.utils

import java.util.regex.Pattern

object UicodeBackslashU {
    private const val singlePattern = "[0-9|a-f|A-F]"
    private val pattern = singlePattern + singlePattern + singlePattern + singlePattern

    private fun ustartToCn(str: String): String {
        val sb = StringBuilder().append("0x").append(str.substring(2, 6))
        val codeInteger = Integer.decode(sb.toString())
        val code = codeInteger.toInt()
        val c = code.toChar()
        return c.toString()
    }

    private fun isStartWithUnicode(str: String?): Boolean {
        if (str == null || str.isEmpty()) {
            return false
        }
        if (!str.startsWith("\\u")) {
            return false
        }
        if (str.length < 6) {
            return false
        }
        val content = str.substring(2, 6)
        return Pattern.matches(pattern, content)
    }

    @JvmStatic
    fun unicodeToCn(str: String): String {
        val sb = StringBuilder()
        val length = str.length
        var i = 0
        while (i < length) {
            val tmpStr = str.substring(i)
            if (isStartWithUnicode(tmpStr)) {
                sb.append(ustartToCn(tmpStr))
                i += 6
            } else {
                sb.append(str.substring(i, i + 1))
                i++
            }
        }
        return sb.toString()
    }
}
