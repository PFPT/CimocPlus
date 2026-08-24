package com.haleydu.cimoc.ui.reader

import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityPageReaderBinding
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.ui.widget.rvp.RecyclerViewPager
import com.haleydu.cimoc.ui.widget.rvp.RecyclerViewPager.OnPageChangedListener
import dagger.hilt.android.AndroidEntryPoint
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

@AndroidEntryPoint
class PageReaderActivity : ReaderActivity(), OnPageChangedListener {

    private lateinit var binding: ActivityPageReaderBinding

    override fun initView() {
        super.initView()
        mLoadPrev = mPreference.getBoolean(PreferenceManager.PREF_READER_PAGE_LOAD_PREV, true)
        mLoadNext = mPreference.getBoolean(PreferenceManager.PREF_READER_PAGE_LOAD_NEXT, true)
        val offset = mPreference.getInt(PreferenceManager.PREF_READER_PAGE_TRIGGER, 10)
        mReaderAdapter.setReaderMode(ReaderAdapter.READER_PAGE)
        val pager = mRecyclerView as RecyclerViewPager
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_PAGE_QUICK_TURN, false)) {
            pager.setScrollSpeed(0.000001f)
        } else {
            pager.setScrollSpeed(0.12f)
        }
        pager.setTriggerOffset(0.01f * offset)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mRecyclerView.isForceDarkAllowed = false
        }
        pager.setOnPageChangedListener(this)
        mRecyclerView.itemAnimator = null
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideControl()
                }
            }
        })
    }

    override fun OnPageChanged(oldPosition: Int, newPosition: Int) {
        if (oldPosition < 0 || newPosition < 0) {
            return
        }
        if (mLoadPrev && newPosition == 0) {
            vm.loadPrev()
        }
        if (mLoadNext && newPosition == mReaderAdapter.itemCount - 1) {
            vm.loadNext()
        }
        val newImage = mReaderAdapter.getItem(newPosition)
        val oldImage = mReaderAdapter.getItem(oldPosition)
        if (oldImage.chapter != newImage.chapter) {
            if (newPosition > oldPosition) {
                vm.toNextChapter()
            } else if (newPosition < oldPosition) {
                vm.toPrevChapter()
            }
        }
        pageProgress = newImage.num
        updateProgress()
        prefetchAround(newPosition)
    }

    override fun onPrevLoadSuccess(list: List<ImageUrl>) {
        setReaderAdapter(list)
        mReaderAdapter.addAll(0, list)
        (mRecyclerView as RecyclerViewPager).refreshPosition()
        showLoadSuccess()
    }

    override fun onNextLoadSuccess(list: List<ImageUrl>, silent: Boolean) {
        setReaderAdapter(list)
        mReaderAdapter.addAll(list)
        if (!silent) {
            showLoadSuccess()
        }
    }

    override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
        if (fromUser) {
            val current = getCurPosition() + value - pageProgress
            val pos = mReaderAdapter.getPositionByNum(current, value, value < pageProgress)
            mRecyclerView.scrollToPosition(pos)
        }
    }

    override fun prevPage() {
        hideControl()
        val position = getCurPosition()
        if (position == 0) {
            vm.loadPrev()
        } else {
            mRecyclerView.smoothScrollToPosition(position - 1)
        }
    }

    override fun nextPage() {
        hideControl()
        val position = getCurPosition()
        if (position == mReaderAdapter.itemCount - 1) {
            vm.loadNext()
        } else {
            mRecyclerView.smoothScrollToPosition(position + 1)
        }
    }

    override fun getCurPosition(): Int {
        return (mRecyclerView as RecyclerViewPager).currentPosition
    }

    override fun getLayoutRes(): Int = R.layout.activity_page_reader

    override fun inflateContentView(): View {
        binding = ActivityPageReaderBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun bindViews() {
        super.bindViews()
        bindReaderViews(
            binding.readerInfo.readerChapterTitle,
            binding.readerInfo.readerChapterPage,
            binding.readerInfo.readerBattery,
            binding.readerSeek.readerProgressLayout,
            binding.readerBack.readerBackLayout,
            binding.readerInfo.readerInfoLayout,
            binding.readerSeek.readerSeekBar,
            binding.readerLoading,
            binding.readerRecyclerView,
            binding.readerBox,
            binding.readerBack.readerBackBtn
        )
    }
}
