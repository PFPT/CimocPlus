package com.haleydu.cimoc.misc

class Switcher<T>(var element: T, var isEnable: Boolean) {

    fun switchEnable() {
        isEnable = !isEnable
    }
}
