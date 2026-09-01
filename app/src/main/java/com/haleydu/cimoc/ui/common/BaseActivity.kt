package com.haleydu.cimoc.ui.common
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.AppGetter
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.ui.common.dialog.ProgressDialogFragment
import com.haleydu.cimoc.ui.widget.GuofengAtmosphereView
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.ThemeUtils
import dagger.hilt.android.EntryPointAccessors

abstract class BaseActivity : AppCompatActivity(), AppGetter {

    protected lateinit var mPreference: PreferenceManager
    @Nullable
    protected var mNightMask: View? = null
    @Nullable
    protected var mToolbar: Toolbar? = null
    private lateinit var mProgressDialog: ProgressDialogFragment
    protected lateinit var mContentRoot: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPreference = EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
            .preferenceManager()
        initTheme()
        mContentRoot = inflateContentView()
        setContentView(mContentRoot)
        bindViews()
        initAtmosphere()
        applyWindowInsets()
        initNight()
        initToolbar()
        initViewModel()
        mProgressDialog = ProgressDialogFragment.newInstance()
        initView()
        initData()
        initUser()
    }

    override val appInstance: App
        get() = application as App

    fun onNightSwitch() {
        initNight()
    }

    protected open fun initTheme() {
        applyNightMode()
        val theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_GUOFENG)
        setTheme(ThemeUtils.getThemeById(theme))
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun applyNightMode() {
        val night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false)
        val mode = if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    protected open fun useNightMask(): Boolean {
        return true
    }

    protected open fun useGuofengAtmosphere(): Boolean {
        return true
    }

    protected fun initAtmosphere() {
        if (!useGuofengAtmosphere()) {
            return
        }
        val root = mContentRoot
        if (root !is ViewGroup) {
            root.setBackgroundResource(R.drawable.bg_guofeng_paper)
            return
        }
        val atmosphere = GuofengAtmosphereView(this)
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (root is FrameLayout ||
            root is RelativeLayout ||
            root is CoordinatorLayout
        ) {
            root.addView(atmosphere, 0, lp)
        } else {
            root.setBackgroundResource(R.drawable.bg_guofeng_paper)
        }
    }

    protected fun initNight() {
        val mask = mNightMask ?: return
        if (!useNightMask()) {
            mask.visibility = View.GONE
            return
        }
        val night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false)
        val color = mPreference.getInt(PreferenceManager.PREF_OTHER_NIGHT_ALPHA, 0xB0) shl 24
        mask.setBackgroundColor(color)
        mask.visibility = if (night) View.VISIBLE else View.INVISIBLE
    }

    protected open fun initToolbar() {
        val toolbar = mToolbar ?: return
        toolbar.title = getDefaultTitle()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected open fun applyWindowInsets() {
        val insetTarget = mToolbar ?: mContentRoot
        ViewCompat.setOnApplyWindowInsetsListener(insetTarget) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                bars.top,
                v.paddingRight,
                if (isNavTranslation()) v.paddingBottom else bars.bottom
            )
            insets
        }
    }

    protected open fun inflateContentView(): View {
        return layoutInflater.inflate(getLayoutRes(), null, false)
    }

    protected open fun bindViews() {
        mNightMask = mContentRoot.findViewById(R.id.custom_night_mask)
        mToolbar = mContentRoot.findViewById(R.id.custom_toolbar)
    }

    protected open fun getLayoutView(): View? {
        return null
    }

    protected open fun getDefaultTitle(): String? {
        return null
    }

    protected open fun initViewModel() {
    }

    protected open fun initView() {
    }

    protected open fun initData() {
    }

    protected open fun initUser() {
    }

    protected abstract fun getLayoutRes(): Int

    protected open fun isNavTranslation(): Boolean {
        return false
    }

    protected fun showSnackbar(msg: String) {
        HintUtils.showSnackbar(getLayoutView(), msg)
    }

    protected fun showSnackbar(resId: Int) {
        showSnackbar(getString(resId))
    }

    fun showProgressDialog() {
        mProgressDialog.show(supportFragmentManager, null)
    }

    fun hideProgressDialog() {
        mProgressDialog.dismissAllowingStateLoss()
    }
}
