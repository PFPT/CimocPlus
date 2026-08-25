package com.haleydu.cimoc.ui.settings;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.databinding.ActivityReaderConfigBinding;
import com.haleydu.cimoc.global.ClickEvents;
import com.haleydu.cimoc.ui.common.BackActivity;
import com.haleydu.cimoc.ui.common.TabPagerAdapter;
import com.haleydu.cimoc.ui.common.BaseFragment;

public class ReaderConfigActivity extends BackActivity implements DialogCaller {

    TabLayout mTabLayout;
    ViewPager2 mViewPager;
    private ActivityReaderConfigBinding binding;

    private String[] mKeyArray;
    private int[] mChoiceArray;

    @Override
    protected void initView() {
        TabPagerAdapter tabAdapter = new TabPagerAdapter(this,
                new BaseFragment[]{new PageConfigFragment(), new StreamConfigFragment()},
                new String[]{getString(R.string.reader_config_page), getString(R.string.reader_config_stream)});
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(tabAdapter);
        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) ->
                tab.setText(tabAdapter.getPageTitle(position))).attach();
        updateClickEvents(mViewPager.getCurrentItem());
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateClickEvents(position);
            }
        });
    }

    private void updateClickEvents(int position) {
        boolean isStream = position == 1;
        if (isStream) {
            mKeyArray = ClickEvents.getStreamClickEvents();
            mChoiceArray = ClickEvents.getStreamClickEventChoice(mPreference);
        } else {
            mKeyArray = ClickEvents.getPageClickEvents();
            mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.reader_config_title);
    }

    @Override
    protected android.view.View inflateContentView() {
        binding = ActivityReaderConfigBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_reader_config;
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
        mChoiceArray[requestCode] = index;
        mPreference.putInt(mKeyArray[requestCode], index);
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        mTabLayout = binding.readerConfigTabLayout;
        mViewPager = binding.readerConfigViewPager;
    }

}
