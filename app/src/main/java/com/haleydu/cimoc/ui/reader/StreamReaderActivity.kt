package com.haleydu.cimoc.ui.reader

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityStreamReaderBinding
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.ui.widget.ZoomableRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

@AndroidEntryPoint
class StreamReaderActivity : ReaderActivity() {

    private var mLastPosition = 0
    private lateinit var binding: ActivityStreamReaderBinding

    override fun initView() {
        super.initView()
        mLoadPrev = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_PREV, false)
        mLoadNext = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_NEXT, true)
        mReaderAdapter.setReaderMode(ReaderAdapter.READER_STREAM)
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_INTERVAL, false)) {
            mRecyclerView.addItemDecoration(mReaderAdapter.itemDecoration)
        }
        val zoomable = mRecyclerView as ZoomableRecyclerView
        zoomable.setScaleFactor(
            mPreference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200) * 0.01f
        )
        zoomable.setVertical(turn == PreferenceManager.READER_TURN_ATB)
        zoomable.setDoubleTap(
            !mPreference.getBoolean(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false)
        )
        zoomable.setTapListenerListener(this)
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> hideControl()
                    RecyclerView.SCROLL_STATE_IDLE, RecyclerView.SCROLL_STATE_SETTLING -> {
                        if (mLoadPrev && mLayoutManager.findFirstVisibleItemPosition() == 0) {
                            vm.loadPrev()
                        }
                        if (mLoadNext &&
                            mLayoutManager.findLastVisibleItemPosition() == mReaderAdapter.itemCount - 1
                        ) {
                            vm.loadNext()
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val target = mLayoutManager.findFirstVisibleItemPosition()
                if (target != mLastPosition) {
                    val newImage = mReaderAdapter.getItem(target)
                    val oldImage = mReaderAdapter.getItem(mLastPosition)
                    if (oldImage.chapter != newImage.chapter) {
                        when (turn) {
                            PreferenceManager.READER_TURN_ATB -> {
                                if (dy > 0) vm.toNextChapter() else if (dy < 0) vm.toPrevChapter()
                            }
                            PreferenceManager.READER_TURN_LTR -> {
                                if (dx > 0) vm.toNextChapter() else if (dx < 0) vm.toPrevChapter()
                            }
                            PreferenceManager.READER_TURN_RTL -> {
                                if (dx > 0) vm.toPrevChapter() else if (dx < 0) vm.toNextChapter()
                            }
                        }
                    }
                    pageProgress = mReaderAdapter.getItem(target).num
                    mLastPosition = target
                    updateProgress()
                    prefetchAround(target)
                }
            }
        })
    }

    override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
        if (fromUser) {
            val current = mLastPosition + value - pageProgress
            val pos = mReaderAdapter.getPositionByNum(current, value, value < pageProgress)
            mLayoutManager.scrollToPositionWithOffset(pos, 0)
        }
    }

    override fun prevPage() {
        val point = windowSize()
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, -point.y + point.y / 5)
        } else {
            mRecyclerView.smoothScrollBy(-point.x, 0)
        }
        if (mLayoutManager.findFirstVisibleItemPosition() == 0) {
            loadPrev()
        }
    }

    override fun nextPage() {
        val point = windowSize()
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, point.y - point.y / 5)
        } else {
            mRecyclerView.smoothScrollBy(point.x, 0)
        }
        if (mLayoutManager.findLastVisibleItemPosition() == mReaderAdapter.itemCount - 1) {
            loadNext()
        }
    }

    override fun onInitLoadSuccess(list: List<ImageUrl>, progress: Int, source: Int, local: Boolean) {
        mLastPosition = (progress - 1).coerceAtLeast(0)
        super.onInitLoadSuccess(list, progress, source, local)
    }

    override fun onPrevLoadSuccess(list: List<ImageUrl>) {
        super.onPrevLoadSuccess(list)
        if (mLastPosition == 0) {
            mLastPosition = list.size
        }
    }

    override fun getCurPosition(): Int = mLastPosition

    override fun getLayoutRes(): Int = R.layout.activity_stream_reader

    override fun inflateContentView(): View {
        binding = ActivityStreamReaderBinding.inflate(layoutInflater)
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
