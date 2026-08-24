package com.haleydu.cimoc.script

import android.util.Log
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsConsole @Inject constructor() {

    private val logs = ArrayDeque<String>()

    @JvmOverloads
    fun log(a: Any? = null, b: Any? = null, c: Any? = null, d: Any? = null) {
        val message = listOf(a, b, c, d).filterNotNull().joinToString(" ") { it.toString() }
        Log.d(TAG, message)
        synchronized(logs) {
            if (logs.size >= MAX) {
                logs.removeFirst()
            }
            logs.addLast(message)
        }
    }

    fun dump(): String {
        synchronized(logs) {
            return logs.joinToString("\n")
        }
    }

    companion object {
        private const val TAG = "JsSource"
        private const val MAX = 100
    }
}
