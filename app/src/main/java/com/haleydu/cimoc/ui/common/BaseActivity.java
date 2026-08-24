package com.haleydu.cimoc.ui.common;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.di.AppEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.data.PreferenceManager;
import com.haleydu.cimoc.ui.common.dialog.ProgressDialogFragment;
import com.haleydu.cimoc.utils.HintUtils;
import com.haleydu.cimoc.utils.ThemeUtils;

public abstract class BaseActivity extends AppCompatActivity implements AppGetter {

    protected PreferenceManager mPreference;
    @Nullable
    protected View mNightMask;
    @Nullable
    protected Toolbar mToolbar;
    private ProgressDialogFragment mProgressDialog;
    protected View mContentRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreference = EntryPointAccessors.fromApplication(getApplicationContext(), AppEntryPoint.class)
                .preferenceManager();
        initTheme();
        mContentRoot = inflateContentView();
        setContentView(mContentRoot);
        bindViews();
        applyWindowInsets();
        initNight();
        initToolbar();
        initViewModel();
        mProgressDialog = ProgressDialogFragment.newInstance();
        initView();
        initData();
        initUser();
    }

    @Override
    public App getAppInstance() {
        return (App) getApplication();
    }

    public void onNightSwitch() {
        initNight();
    }

    protected void initTheme() {
        applyNightMode();
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_BLUE);
        setTheme(ThemeUtils.getThemeById(theme));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            if (isNavTranslation()) {
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }

    protected void applyNightMode() {
        boolean night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false);
        int mode = night ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    protected boolean useNightMask() {
        return true;
    }

    protected void initNight() {
        if (mNightMask == null) {
            return;
        }
        if (!useNightMask()) {
            mNightMask.setVisibility(View.GONE);
            return;
        }
        boolean night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false);
        int color = mPreference.getInt(PreferenceManager.PREF_OTHER_NIGHT_ALPHA, 0xB0) << 24;
        mNightMask.setBackgroundColor(color);
        mNightMask.setVisibility(night ? View.VISIBLE : View.INVISIBLE);
    }

    protected void initToolbar() {
        if (mToolbar != null) {
            mToolbar.setTitle(getDefaultTitle());
            setSupportActionBar(mToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    protected void applyWindowInsets() {
        View insetTarget = mToolbar != null ? mToolbar : mContentRoot;
        if (insetTarget == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(insetTarget, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(),
                    isNavTranslation() ? v.getPaddingBottom() : bars.bottom);
            return insets;
        });
    }

    protected View inflateContentView() {
        return getLayoutInflater().inflate(getLayoutRes(), null, false);
    }

    protected void bindViews() {
        mNightMask = mContentRoot.findViewById(R.id.custom_night_mask);
        mToolbar = mContentRoot.findViewById(R.id.custom_toolbar);
    }

    protected View getLayoutView() {
        return null;
    }

    protected String getDefaultTitle() {
        return null;
    }

    protected void initViewModel() {
    }

    protected void initView() {
    }

    protected void initData() {
    }

    protected void initUser() {
    }

    protected abstract int getLayoutRes();

    protected boolean isNavTranslation() {
        return false;
    }

    protected void showSnackbar(String msg) {
        HintUtils.showSnackbar(getLayoutView(), msg);
    }

    protected void showSnackbar(int resId) {
        showSnackbar(getString(resId));
    }

    public void showProgressDialog() {
        mProgressDialog.show(getSupportFragmentManager(), null);
    }

    public void hideProgressDialog() {
        mProgressDialog.dismissAllowingStateLoss();
    }

}
