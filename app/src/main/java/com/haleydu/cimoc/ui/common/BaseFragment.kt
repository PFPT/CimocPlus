package com.haleydu.cimoc.ui.common
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.AppGetter
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.utils.ThemeUtils
import dagger.hilt.android.EntryPointAccessors

abstract class BaseFragment : Fragment(), AppGetter {

    protected lateinit var mPreference: PreferenceManager
    @Nullable
    protected var mProgressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(getLayoutRes(), container, false)
        bindViews(view)
        mPreference = EntryPointAccessors.fromApplication(requireActivity().applicationContext, AppEntryPoint::class.java)
            .preferenceManager()
        initViewModel()
        initProgressBar()
        initView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getAppInstance(): App {
        return requireActivity().application as App
    }

    protected open fun bindViews(view: View) {
        mProgressBar = view.findViewById(R.id.custom_progress_bar)
    }

    private fun initProgressBar() {
        val progressBar = mProgressBar ?: return
        val resId = ThemeUtils.getResourceId(requireActivity(), R.attr.colorAccent)
        progressBar.indeterminateDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(requireActivity(), resId),
                BlendModeCompat.SRC_ATOP
            )
    }

    protected open fun initView() {
    }

    protected open fun initData() {
    }

    protected open fun initViewModel() {
    }

    @LayoutRes
    protected abstract fun getLayoutRes(): Int

    protected fun showProgressDialog() {
        (activity as BaseActivity).showProgressDialog()
    }

    protected fun hideProgressDialog() {
        (activity as BaseActivity).hideProgressDialog()
    }

    protected fun hideProgressBar() {
        mProgressBar?.visibility = View.GONE
    }
}
