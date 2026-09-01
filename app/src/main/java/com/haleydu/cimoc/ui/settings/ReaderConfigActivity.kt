package com.haleydu.cimoc.ui.settings
import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivityReaderConfigBinding
import com.haleydu.cimoc.global.ClickEvents
import com.haleydu.cimoc.ui.common.BackActivity
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.common.TabPagerAdapter

class ReaderConfigActivity : BackActivity(), DialogCaller {

    lateinit var mTabLayout: TabLayout
    lateinit var mViewPager: ViewPager2
    private lateinit var binding: ActivityReaderConfigBinding

    private lateinit var mKeyArray: Array<String>
    private lateinit var mChoiceArray: IntArray

    override fun initView() {
        val tabAdapter = TabPagerAdapter(
            this,
            arrayOf(PageConfigFragment(), StreamConfigFragment()),
            arrayOf(getString(R.string.reader_config_page), getString(R.string.reader_config_stream))
        )
        mViewPager.offscreenPageLimit = 1
        mViewPager.adapter = tabAdapter
        TabLayoutMediator(mTabLayout, mViewPager) { tab, position ->
            tab.text = tabAdapter.getPageTitle(position)
        }.attach()
        updateClickEvents(mViewPager.currentItem)
        mViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateClickEvents(position)
            }
        })
    }

    private fun updateClickEvents(position: Int) {
        val isStream = position == 1
        if (isStream) {
            mKeyArray = ClickEvents.getStreamClickEvents()
            mChoiceArray = ClickEvents.getStreamClickEventChoice(mPreference)
        } else {
            mKeyArray = ClickEvents.getPageClickEvents()
            mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference)
        }
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.reader_config_title)
    }

    override fun inflateContentView(): View {
        binding = ActivityReaderConfigBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_reader_config
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        val index = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)
        mChoiceArray[requestCode] = index
        mPreference.putInt(mKeyArray[requestCode], index)
    }

    override fun bindViews() {
        super.bindViews()
        mTabLayout = binding.readerConfigTabLayout
        mViewPager = binding.readerConfigViewPager
    }
}
