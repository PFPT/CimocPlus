package com.haleydu.cimoc.ui.common;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.fragment.app.Fragment;

import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.di.AppEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.data.PreferenceManager;
import com.haleydu.cimoc.ui.common.BaseActivity;
import com.haleydu.cimoc.utils.ThemeUtils;

public abstract class BaseFragment extends Fragment implements AppGetter {

    protected PreferenceManager mPreference;
    @Nullable
    protected ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutRes(), container, false);
        bindViews(view);
        mPreference = EntryPointAccessors.fromApplication(requireActivity().getApplicationContext(), AppEntryPoint.class)
                .preferenceManager();
        initViewModel();
        initProgressBar();
        initView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
    }

    @Override
    public App getAppInstance() {
        return (App) requireActivity().getApplication();
    }

    protected void bindViews(View view) {
        mProgressBar = view.findViewById(R.id.custom_progress_bar);
    }

    private void initProgressBar() {
        if (mProgressBar != null) {
            int resId = ThemeUtils.getResourceId(requireActivity(), R.attr.colorAccent);
            mProgressBar.getIndeterminateDrawable().setColorFilter(
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            ContextCompat.getColor(requireActivity(), resId), BlendModeCompat.SRC_ATOP));
        }
    }

    protected void initView() {
    }

    protected void initData() {
    }

    protected void initViewModel() {
    }

    protected abstract @LayoutRes
    int getLayoutRes();

    protected void showProgressDialog() {
        ((BaseActivity) getActivity()).showProgressDialog();
    }

    protected void hideProgressDialog() {
        ((BaseActivity) getActivity()).hideProgressDialog();
    }

    protected void hideProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

}
