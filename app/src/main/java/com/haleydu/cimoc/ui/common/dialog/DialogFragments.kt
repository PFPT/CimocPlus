package com.haleydu.cimoc.ui.common.dialog

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

fun DialogFragment.showForCaller(caller: Fragment) {
    show(caller.childFragmentManager, javaClass.simpleName)
}
