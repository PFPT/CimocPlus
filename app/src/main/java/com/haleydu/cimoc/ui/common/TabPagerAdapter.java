package com.haleydu.cimoc.ui.common;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabPagerAdapter extends FragmentStateAdapter {

    private final Fragment[] fragment;
    private final String[] title;

    public TabPagerAdapter(FragmentActivity activity, BaseFragment[] fragment, String[] title) {
        super(activity);
        this.fragment = fragment;
        this.title = title;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragment[position];
    }

    @Override
    public int getItemCount() {
        return fragment.length;
    }

    public CharSequence getPageTitle(int position) {
        return title[position];
    }

}
