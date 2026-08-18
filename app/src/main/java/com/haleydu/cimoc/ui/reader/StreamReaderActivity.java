package com.haleydu.cimoc.ui.reader;

import android.graphics.Point;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivityStreamReaderBinding;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.ui.reader.ReaderAdapter;
import com.haleydu.cimoc.ui.widget.ZoomableRecyclerView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StreamReaderActivity extends ReaderActivity {

    private int mLastPosition = 0;
    private ActivityStreamReaderBinding binding;

    @Override
    protected void initView() {
        super.initView();
        mLoadPrev = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_PREV, false);
        mLoadNext = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_NEXT, true);
        mReaderAdapter.setReaderMode(ReaderAdapter.READER_STREAM);
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_INTERVAL, false)) {
            mRecyclerView.addItemDecoration(mReaderAdapter.getItemDecoration());
        }
        ((ZoomableRecyclerView) mRecyclerView).setScaleFactor(
                mPreference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200) * 0.01f);
        ((ZoomableRecyclerView) mRecyclerView).setVertical(turn == PreferenceManager.READER_TURN_ATB);
        ((ZoomableRecyclerView) mRecyclerView).setDoubleTap(
                !mPreference.getBoolean(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false));
        ((ZoomableRecyclerView) mRecyclerView).setTapListenerListener(this);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        hideControl();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (mLoadPrev) {
                            int item = mLayoutManager.findFirstVisibleItemPosition();
                            if (item == 0) {
                                vm.loadPrev();
                            }
                        }
                        if (mLoadNext) {
                            int item = mLayoutManager.findLastVisibleItemPosition();
                            if (item == mReaderAdapter.getItemCount() - 1) {
                                vm.loadNext();
                            }
                        }
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int target = mLayoutManager.findFirstVisibleItemPosition();
                if (target != mLastPosition) {
                    ImageUrl newImage = mReaderAdapter.getItem(target);
                    ImageUrl oldImage = mReaderAdapter.getItem(mLastPosition);

                    if (!oldImage.getChapter().equals(newImage.getChapter())) {
                        switch (turn) {
                            case PreferenceManager.READER_TURN_ATB:
                                if (dy > 0) {
                                    vm.toNextChapter();
                                } else if (dy < 0) {
                                    vm.toPrevChapter();
                                }
                                break;
                            case PreferenceManager.READER_TURN_LTR:
                                if (dx > 0) {
                                    vm.toNextChapter();
                                } else if (dx < 0) {
                                    vm.toPrevChapter();
                                }
                                break;
                            case PreferenceManager.READER_TURN_RTL:
                                if (dx > 0) {
                                    vm.toPrevChapter();
                                } else if (dx < 0) {
                                    vm.toNextChapter();
                                }
                                break;
                        }
                    }
                    progress = mReaderAdapter.getItem(target).getNum();
                    mLastPosition = target;
                    updateProgress();
                }
            }
        });
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) {
            int current = mLastPosition + value - progress;
            int pos = mReaderAdapter.getPositionByNum(current, value, value < progress);
            mLayoutManager.scrollToPositionWithOffset(pos, 0);
        }
    }

    @Override
    protected void prevPage() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, -point.y+point.y/5);
        } else {
            mRecyclerView.smoothScrollBy(-point.x, 0);
        }
        if (mLayoutManager.findFirstVisibleItemPosition() == 0) {
            loadPrev();
        }
    }

    @Override
    protected void nextPage() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, point.y-point.y/5);
        } else {
            mRecyclerView.smoothScrollBy(point.x, 0);
        }
        if (mLayoutManager.findLastVisibleItemPosition() == mReaderAdapter.getItemCount() - 1) {
            loadNext();
        }
    }

    @Override
    public void onInitLoadSuccess(List<ImageUrl> list, int progress, int source, boolean local) {
        mLastPosition = Math.max(progress - 1, 0);
        super.onInitLoadSuccess(list, progress, source, local);
    }

    @Override
    public void onPrevLoadSuccess(List<ImageUrl> list) {
        super.onPrevLoadSuccess(list);
        if (mLastPosition == 0) {
            mLastPosition = list.size();
        }
    }

    @Override
    protected int getCurPosition() {
        return mLastPosition;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_stream_reader;
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityStreamReaderBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected void bindViews() {
        super.bindViews();
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
        );
    }

}
