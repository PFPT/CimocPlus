package com.haleydu.cimoc.global

object FastClick {
    private var last = 0L

    @JvmStatic
    fun isClickValid(): Boolean {
        val cur = System.currentTimeMillis()
        val valid = cur - last > 400
        if (valid) {
            last = cur
        }
        return valid
    }
}
