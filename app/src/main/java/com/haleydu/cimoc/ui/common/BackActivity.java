package com.haleydu.cimoc.ui.common;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import android.view.View;
import android.widget.ProgressBar;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.utils.ThemeUtils;


/**
 * Created by Hiroshi on 2016/9/11.
 */
public abstract class BackActivity extends BaseActivity {

    protected ProgressBar mProgressBar;

    @Override
    protected void initToolbar() {
        super.initToolbar();
        if (mToolbar != null) {
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }
    }

    @Override
    protected void initView() {
        if (mProgressBar != null) {
            int resId = ThemeUtils.getResourceId(this, R.attr.colorAccent);
            mProgressBar.getIndeterminateDrawable().setColorFilter(
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            ContextCompat.getColor(this, resId), BlendModeCompat.SRC_ATOP));
        }
    }

    protected boolean isProgressBarShown() {
        return mProgressBar != null && mProgressBar.isShown();
    }

    protected void hideProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mProgressBar = mContentRoot.findViewById(R.id.custom_progress_bar);
    }

}
