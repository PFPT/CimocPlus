package com.haleydu.cimoc.ui.common
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabPagerAdapter(
    activity: FragmentActivity,
    fragment: Array<BaseFragment>,
    private val title: Array<String>
) : FragmentStateAdapter(activity) {

    private val fragment: Array<out Fragment> = fragment

    override fun createFragment(position: Int): Fragment {
        return fragment[position]
    }

    override fun getItemCount(): Int {
        return fragment.size
    }

    fun getPageTitle(position: Int): CharSequence {
        return title[position]
    }
}
