package com.haleydu.cimoc.ui.common
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.utils.ThemeUtils

abstract class BackActivity : BaseActivity() {

    protected var mProgressBar: ProgressBar? = null

    override fun initToolbar() {
        super.initToolbar()
        mToolbar?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun initView() {
        val progressBar = mProgressBar ?: return
        val resId = ThemeUtils.getResourceId(this, R.attr.colorAccent)
        progressBar.indeterminateDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(this, resId),
                BlendModeCompat.SRC_ATOP
            )
    }

    protected val isProgressBarShown: Boolean
        get() = mProgressBar != null && mProgressBar!!.isShown

    protected fun hideProgressBar() {
        mProgressBar?.visibility = View.GONE
    }

    override fun bindViews() {
        super.bindViews()
        mProgressBar = mContentRoot.findViewById(R.id.custom_progress_bar)
    }
}
