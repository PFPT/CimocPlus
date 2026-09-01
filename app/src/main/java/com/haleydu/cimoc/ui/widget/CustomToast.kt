package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object CustomToast {

    private var mToast: Toast? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private val r = Runnable { mToast?.cancel() }

    @JvmStatic
    fun showToast(mContext: Context, text: String?, duration: Int) {
        mHandler.removeCallbacks(r)
        val toast = mToast
        if (toast != null) {
            toast.setText(text)
        } else {
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT)
        }
        mHandler.postDelayed(r, duration.toLong())
        mToast?.show()
    }

    @JvmStatic
    fun showToast(mContext: Context, resId: Int, duration: Int) {
        showToast(mContext, mContext.resources.getString(resId), duration)
    }
}
