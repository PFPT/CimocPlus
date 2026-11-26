package com.haleydu.cimoc.script

import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context

fun enterSandboxedRhino(): Context {
    val context = Context.enter()
    try {
        context.optimizationLevel = -1
        context.setClassShutter(ClassShutter { false })
        return context
    } catch (e: RuntimeException) {
        Context.exit()
        throw e
    }
}
