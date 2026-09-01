package com.haleydu.cimoc.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object HintUtils {

    @JvmStatic
    fun showSnackbar(layout: View?, msg: String) {
        if (layout != null && layout.isShown) {
            Snackbar.make(layout, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun showToast(context: Context, resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}
