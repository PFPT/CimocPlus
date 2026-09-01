package com.haleydu.cimoc.component

import android.os.Bundle
import androidx.fragment.app.Fragment

interface DialogCaller {

    fun onDialogResult(requestCode: Int, bundle: Bundle)

    companion object {
        const val EXTRA_DIALOG_RESULT_INDEX = "cimoc.intent.extra.EXTRA_DIALOG_RESULT_INDEX"
        const val EXTRA_DIALOG_RESULT_VALUE = "cimoc.intent.extra.EXTRA_DIALOG_RESULT_VALUE"
        const val EXTRA_DIALOG_REQUEST_CODE = "cimoc.intent.extra.EXTRA_DIALOG_REQUEST_CODE"
        const val EXTRA_DIALOG_TITLE = "cimoc.intent.extra.EXTRA_DIALOG_TITLE"
        const val EXTRA_DIALOG_ITEMS = "cimoc.intent.extra.EXTRA_DIALOG_ITEMS"
        const val EXTRA_DIALOG_CONTENT = "cimoc.intent.extra.EXTRA_DIALOG_CONTENT"
        const val EXTRA_DIALOG_CONTENT_TEXT = "cimoc.intent.extra.EXTRA_DIALOG_CONTENT_TEXT"
        const val EXTRA_DIALOG_NEGATIVE = "cimoc.intent.extra.EXTRA_DIALOG_NEGATIVE"
        const val EXTRA_DIALOG_CHOICE_ITEMS = "cimoc.intent.extra.EXTRA_DIALOG_CHOICE_ITEMS"

        @JvmStatic
        fun from(fragment: Fragment): DialogCaller? {
            val parent = fragment.parentFragment
            if (parent is DialogCaller) {
                return parent
            }
            val activity = fragment.activity
            if (activity is DialogCaller) {
                return activity
            }
            return null
        }
    }
}
