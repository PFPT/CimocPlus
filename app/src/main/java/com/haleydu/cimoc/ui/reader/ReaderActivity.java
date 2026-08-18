package com.haleydu.cimoc.ui.reader;

import com.haleydu.cimoc.ui.activity.BaseActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.haleydu.cimoc.App;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.fresco.ControllerBuilderSupplierFactory;
import com.haleydu.cimoc.fresco.ImagePipelineFactoryBuilder;
import com.haleydu.cimoc.fresco.OkHttpNetworkFetcher;
import com.haleydu.cimoc.global.ClickEvents;
import com.haleydu.cimoc.global.Extra;
import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.reader.ReaderAdapter;
import com.haleydu.cimoc.ui.reader.ReaderAdapter.OnLazyLoadListener;
import com.haleydu.cimoc.ui.widget.OnTapGestureListener;
import com.haleydu.cimoc.ui.widget.PreCacheLayoutManager;
import com.haleydu.cimoc.ui.widget.RetryDraweeView;
import com.haleydu.cimoc.ui.widget.ReverseSeekBar;
import com.haleydu.cimoc.utils.HintUtils;
import com.haleydu.cimoc.utils.StringUtils;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar.OnProgressChangeListener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import okhttp3.OkHttpClient;


/**
 * Created by Hiroshi on 2016/8/6.
 */
public abstract class ReaderActivity extends BaseActivity implements OnTapGestureListener,
        OnProgressChangeListener, OnLazyLoadListener {

    protected PreCacheLayoutManager mLayoutManager;
    protected ReaderAdapter mReaderAdapter;
    protected ImagePipelineFactory mImagePipelineFactory;
    protected ImagePipelineFactory mLargeImagePipelineFactory;
    private OkHttpNetworkFetcher mNetworkFetcher;
    private OkHttpNetworkFetcher mLargeNetworkFetcher;
    protected ReaderViewModel vm;
    @Inject
    OkHttpClient httpClient;
    protected int mLastDx = 0;
    protected int mLastDy = 0;
    protected int progress = 1;
    protected int max = 1;
    protected int turn;
    protected int orientation;
    protected int mode;
    protected boolean mLoadPrev;
    protected boolean mLoadNext;
    TextView mChapterTitle;
    TextView mChapterPage;
    TextView mBatteryText;
    View mProgressLayout;
    View mBackLayout;
    View mInfoLayout;
    ReverseSeekBar mSeekBar;
    TextView mLoadingText;
    RecyclerView mRecyclerView;
    RelativeLayout mReaderBox;
    private DiscreteSeekBar mBrightnessBar;
    private TextView mAutoBtn;
    private final Handler autoHandler = new Handler(Looper.getMainLooper());
    private boolean autoPaging = false;
    private boolean isSavingPicture = false;

    private boolean mHideInfo;
    private boolean mHideNav;
    private boolean mShowTopbar;
    private int[] mClickArray;
    private int[] mLongClickArray;
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                String text = (level * 100 / scale) + "%";
                mBatteryText.setText(text);
            }
        }
    };
    private int _source;
    private boolean _local;

    public static Intent createIntent(Context context, long id, List<Chapter> list, int mode) {
        Intent intent = getIntent(context, mode);
        intent.putExtra(Extra.EXTRA_ID, id);
        intent.putExtra(Extra.EXTRA_CHAPTER, new ArrayList<>(list));
        intent.putExtra(Extra.EXTRA_MODE, mode);
        return intent;
    }

    private static Intent getIntent(Context context, int mode) {
        if (mode == PreferenceManager.READER_MODE_PAGE) {
            return new Intent(context, PageReaderActivity.class);
        } else {
            return new Intent(context, StreamReaderActivity.class);
        }
    }

    @Override
    protected void initTheme() {
        super.initTheme();
        mHideNav = mPreference.getBoolean(PreferenceManager.PREF_READER_HIDE_NAV, false);
        mShowTopbar = mPreference.getBoolean(PreferenceManager.PREF_OTHER_SHOW_TOPBAR, false);
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_KEEP_BRIGHT, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        mode = getIntent().getIntExtra(Extra.EXTRA_MODE, PreferenceManager.READER_MODE_PAGE);
        String key = mode == PreferenceManager.READER_MODE_PAGE ?
                PreferenceManager.PREF_READER_PAGE_ORIENTATION : PreferenceManager.PREF_READER_STREAM_ORIENTATION;
        orientation = mPreference.getInt(key, PreferenceManager.READER_ORIENTATION_PORTRAIT);
        final int[] oArray = {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED};
        setRequestedOrientation(oArray[orientation]);
    }

    @Override
    protected void applyWindowInsets() {
    }

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(ReaderViewModel.class);
    }

    @Override
    protected void initView() {
        mHideInfo = mPreference.getBoolean(PreferenceManager.PREF_READER_HIDE_INFO, false);
        mControllerTrigThreshold = mPreference.getInt(PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD, 30) * 0.01f;
        mInfoLayout.setVisibility(mHideInfo ? View.INVISIBLE : View.VISIBLE);
        // 防止miui及其他魔改ROM启用反色
        setTheme(R.style.AppThemeNoDark);
        String key = mode == PreferenceManager.READER_MODE_PAGE ?
                PreferenceManager.PREF_READER_PAGE_TURN : PreferenceManager.PREF_READER_STREAM_TURN;
        turn = mPreference.getInt(key, PreferenceManager.READER_TURN_LTR);
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_WHITE_BACKGROUND, false)) {
            mReaderBox.setBackgroundResource(R.color.white);
        }
        initSeekBar();

        initLayoutManager();
        initReaderAdapter();
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mReaderAdapter);


        mRecyclerView.setItemViewCacheSize(2);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                mLastDx = dx;
                mLastDy = dy;
            }
        });
        initReaderMenu();
        applyColorFilter();
        applySavedBrightness();
        applyInfoPosition();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (mHideNav) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else if (mShowTopbar) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.show(WindowInsetsCompat.Type.navigationBars());
        }
    }

    private void initSeekBar() {
        mSeekBar.setReverse(turn == PreferenceManager.READER_TURN_RTL);
        mSeekBar.setOnProgressChangeListener(this);
    }

    private void initReaderAdapter() {
        mReaderAdapter = new ReaderAdapter(this, new LinkedList<>());
        mReaderAdapter.setTapGestureListener(this);
        mReaderAdapter.setLazyLoadListener(this);
        mReaderAdapter.setScaleFactor(mPreference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200) * 0.01f);
        mReaderAdapter.setDoubleTap(!mPreference.getBoolean(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false));
        mReaderAdapter.setVertical(turn == PreferenceManager.READER_TURN_ATB);
        boolean paging = mode != PreferenceManager.READER_MODE_STREAM
                && mPreference.getBoolean(PreferenceManager.PREF_READER_PAGING, false);
        mReaderAdapter.setPaging(paging);
        mReaderAdapter.setCloseAutoResizeImage(mPreference.getBoolean(PreferenceManager.PREF_READER_CLOSEAUTORESIZEIMAGE, false));
        mReaderAdapter.setPagingReverse(mPreference.getBoolean(PreferenceManager.PREF_READER_PAGING_REVERSE, false));
        mReaderAdapter.setWhiteEdge(mPreference.getBoolean(PreferenceManager.PREF_READER_WHITE_EDGE, false));
        mReaderAdapter.setBanTurn(mPreference.getBoolean(PreferenceManager.PREF_READER_PAGE_BAN_TURN, false));
    }

    private void initLayoutManager() {
        mLayoutManager = new PreCacheLayoutManager(this);
        mLayoutManager.setOrientation(turn == PreferenceManager.READER_TURN_ATB ? LinearLayoutManager.VERTICAL : LinearLayoutManager.HORIZONTAL);
        mLayoutManager.setReverseLayout(turn == PreferenceManager.READER_TURN_RTL);
        mLayoutManager.setExtraSpace(2);

    }

    @Override
    protected void initData() {
        try {
            mClickArray = mode == PreferenceManager.READER_MODE_PAGE ?
                    ClickEvents.getPageClickEventChoice(mPreference) : ClickEvents.getStreamClickEventChoice(mPreference);
            mLongClickArray = mode == PreferenceManager.READER_MODE_PAGE ?
                    ClickEvents.getPageLongClickEventChoice(mPreference) : ClickEvents.getStreamLongClickEventChoice(mPreference);
            FlowExtKt.collectOnStart(vm.getEvents(), this, event -> {
                if (event instanceof ReaderViewModel.Event.ParseError) {
                    onParseError();
                } else if (event instanceof ReaderViewModel.Event.NextLoadNone) {
                    onNextLoadNone();
                } else if (event instanceof ReaderViewModel.Event.PrevLoadNone) {
                    onPrevLoadNone();
                } else if (event instanceof ReaderViewModel.Event.NextLoading) {
                    onNextLoading();
                } else if (event instanceof ReaderViewModel.Event.PrevLoading) {
                    onPrevLoading();
                } else if (event instanceof ReaderViewModel.Event.NextLoadSuccess) {
                    ReaderViewModel.Event.NextLoadSuccess success = (ReaderViewModel.Event.NextLoadSuccess) event;
                    onNextLoadSuccess(success.getList(), success.getSilent());
                } else if (event instanceof ReaderViewModel.Event.PrevLoadSuccess) {
                    onPrevLoadSuccess(((ReaderViewModel.Event.PrevLoadSuccess) event).getList());
                } else if (event instanceof ReaderViewModel.Event.InitLoadSuccess) {
                    ReaderViewModel.Event.InitLoadSuccess success = (ReaderViewModel.Event.InitLoadSuccess) event;
                    onInitLoadSuccess(success.getList(), success.getProgress(), success.getSource(), success.getLocal());
                } else if (event instanceof ReaderViewModel.Event.ChapterChange) {
                    onChapterChange(((ReaderViewModel.Event.ChapterChange) event).getChapter());
                } else if (event instanceof ReaderViewModel.Event.ImageLoadSuccess) {
                    ReaderViewModel.Event.ImageLoadSuccess success = (ReaderViewModel.Event.ImageLoadSuccess) event;
                    onImageLoadSuccess(success.getId(), success.getUrl());
                } else if (event instanceof ReaderViewModel.Event.ImageLoadFail) {
                    onImageLoadFail(((ReaderViewModel.Event.ImageLoadFail) event).getId());
                } else if (event instanceof ReaderViewModel.Event.PictureSaveSuccess) {
                    onPictureSaveSuccess(((ReaderViewModel.Event.PictureSaveSuccess) event).getUri());
                } else if (event instanceof ReaderViewModel.Event.PictureSaveFail) {
                    onPictureSaveFail();
                } else if (event instanceof ReaderViewModel.Event.PicturePaging) {
                    onPicturePaging(((ReaderViewModel.Event.PicturePaging) event).getImage());
                } else if (event instanceof ReaderViewModel.Event.ScrollToStart) {
                    toFirst();
                    progress = 1;
                    updateProgress();
                }
            });
            long id = getIntent().getLongExtra(Extra.EXTRA_ID, -1);
            List<Chapter> list = getIntent().getParcelableArrayListExtra(Extra.EXTRA_CHAPTER);
            vm.loadInit(id, Objects.requireNonNull(list).toArray(new Chapter[list.size()]));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vm != null) {
            vm.updateComic(progress);
        }
        unregisterReceiver(batteryReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImagePipelineFactory != null) {
            mImagePipelineFactory.getImagePipeline().clearMemoryCaches();
            mImagePipelineFactory = null;
        }
        if (mLargeImagePipelineFactory != null) {
            mLargeImagePipelineFactory.getImagePipeline().clearMemoryCaches();
            mLargeImagePipelineFactory = null;
        }
        mNetworkFetcher = null;
        mLargeNetworkFetcher = null;
        stopAutoPage();
        autoHandler.removeCallbacksAndMessages(null);
    }

    void onBackClick() {
        onBackPressed();
    }

    @Override
    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
    }

    @Override
    public void onLoad(ImageUrl imageUrl) {
        vm.lazyLoad(imageUrl);
    }

    protected void hideControl() {
        if (mProgressLayout.isShown()) {
            Animation upAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f);
            upAction.setDuration(300);
            Animation downAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 1.0f);
            downAction.setDuration(300);
            mProgressLayout.startAnimation(downAction);
            mProgressLayout.setVisibility(View.INVISIBLE);
            mBackLayout.startAnimation(upAction);
            mBackLayout.setVisibility(View.INVISIBLE);
            if (mHideInfo) {
                mInfoLayout.startAnimation(upAction);
                mInfoLayout.setVisibility(View.INVISIBLE);
            }
            applyInfoPosition();
        }
    }

    protected void showControl() {
        Animation upAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        upAction.setDuration(300);
        Animation downAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        downAction.setDuration(300);
        if (mSeekBar.getMax() != max) {
            mSeekBar.setMax(max);
            mSeekBar.setProgress(max);
        }
        mSeekBar.setProgress(progress);
        mProgressLayout.startAnimation(upAction);
        mProgressLayout.setVisibility(View.VISIBLE);
        mBackLayout.startAnimation(downAction);
        mBackLayout.setVisibility(View.VISIBLE);
        if (mHideInfo) {
            mInfoLayout.startAnimation(downAction);
            mInfoLayout.setVisibility(View.VISIBLE);
        }
        applyInfoPosition();
    }

    protected void updateProgress() {
        mChapterPage.setText(StringUtils.getProgress(progress, max));
    }

    public void onPicturePaging(ImageUrl image) {
        int pos = mReaderAdapter.getPositionById(image.getId());
        mReaderAdapter.add(pos + 1, new ImageUrl(image.getId()+900,image.getComicChapter(),image.getNum(), image.getUrls(),
                image.getChapter(), ImageUrl.STATE_PAGE_2, false));
    }

    public void onParseError() {
        HintUtils.showToast(this, R.string.common_parse_error);
    }

    protected void setReaderAdapter(List<ImageUrl> list) {
        setReaderAdapter(list, _source, _local);
    }

    protected void setReaderAdapter(List<ImageUrl> list, int source, boolean local) {
        _source = source;
        _local = local;
        okhttp3.Headers header = local ? null : vm.parserHeader(list);
        if (mImagePipelineFactory == null) {
            mNetworkFetcher = new OkHttpNetworkFetcher(httpClient, header);
            mLargeNetworkFetcher = new OkHttpNetworkFetcher(httpClient, header);
            mImagePipelineFactory = ImagePipelineFactoryBuilder.build(this, false, mNetworkFetcher);
            mLargeImagePipelineFactory = ImagePipelineFactoryBuilder.build(this, true, mLargeNetworkFetcher);
            mReaderAdapter.setControllerSupplier(ControllerBuilderSupplierFactory.get(this, mImagePipelineFactory),
                    ControllerBuilderSupplierFactory.get(this, mLargeImagePipelineFactory));
        } else {
            mNetworkFetcher.setHeaders(header);
            mLargeNetworkFetcher.setHeaders(header);
        }
    }

    public void onNextLoadSuccess(List<ImageUrl> list) {
        onNextLoadSuccess(list, false);
    }

    public void onNextLoadSuccess(List<ImageUrl> list, boolean silent) {
        setReaderAdapter(list);
        mReaderAdapter.addAll(list);
        if (!silent) {
            showLoadSuccess();
        }
    }

    public void onPrevLoadSuccess(List<ImageUrl> list) {
        setReaderAdapter(list);
        mReaderAdapter.addAll(0, list);
        showLoadSuccess();
    }

    public void onInitLoadSuccess(List<ImageUrl> list, int progress, int source, boolean local) {
        setReaderAdapter(list, source, local);
        mReaderAdapter.clear();
        mReaderAdapter.addAll(list);
        if (progress != 1) {
            mRecyclerView.scrollToPosition(progress - 1);
        } else {
            mRecyclerView.scrollToPosition(0);
        }
        mLoadingText.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        this.progress = Math.max(progress, 1);
        updateProgress();
    }

    public void onChapterChange(Chapter chapter) {
        max = chapter.getCount();
        final String title = chapter.getTitle();
        final int titleLengthMax = 15;
        mChapterTitle.setText(
                title.length() > titleLengthMax ?
                        title.substring(0, titleLengthMax).concat("...") :
                        title
        );
    }

    public void onImageLoadSuccess(Long id, String url) {
        mReaderAdapter.update(id, url);
    }

    public void onImageLoadFail(Long id) {
        mReaderAdapter.update(id, null);
    }

    public void onPictureSaveSuccess(Uri uri) {
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        isSavingPicture = false;
        HintUtils.showToast(this, R.string.reader_picture_save_success);
    }

    public void onPictureSaveFail() {
        isSavingPicture = false;
        HintUtils.showToast(this, R.string.reader_picture_save_fail);
    }

    public void onPrevLoading() {
        HintUtils.showToast(this, R.string.reader_load_prev);
    }

    public void onPrevLoadNone() {
        HintUtils.showToast(this, R.string.reader_prev_none);
    }

    public void onNextLoading() {
        HintUtils.showToast(this, R.string.reader_load_next);
    }

    public void onNextLoadNone() {
        stopAutoPage();
        HintUtils.showToast(this, R.string.reader_next_none);
    }

    /**
     * Click Event Function
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mReaderAdapter.getItemCount() != 0) {
            int value = ClickEvents.EVENT_NULL;
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    value = mClickArray[5];
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    value = mClickArray[6];
                    break;

                case KeyEvent.KEYCODE_BUTTON_L1:
                case KeyEvent.KEYCODE_BUTTON_L2:
                    value = mClickArray[7];
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                case KeyEvent.KEYCODE_BUTTON_R2:
                    value = mClickArray[8];
                    break;
                case KeyEvent.KEYCODE_BUTTON_A:
                    value = mClickArray[14];
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    value = mClickArray[13];
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    value = mClickArray[15];
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    value = mClickArray[16];
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    value = mClickArray[9];
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    value = mClickArray[10];
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    value = mClickArray[11];
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    value = mClickArray[12];
                    break;

            }
            if (value != ClickEvents.EVENT_NULL) {
                doClickEvent(value);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // Check that the event came from a game controller
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                processJoystickInput(event);
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(event);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean[] JoyLock = {false, false};
    private int[] JoyEvent = {7, 8};
    private float mControllerTrigThreshold = 0.3f;


    private void checkKey(float val, ClickEvents.JoyLocks joy) {
        //unlock
        if (JoyLock[joy.ordinal()] && val < this.mControllerTrigThreshold) {
            JoyLock[joy.ordinal()] = false;
        }
        //lock
        if (!JoyLock[joy.ordinal()] && val > this.mControllerTrigThreshold) {
            JoyLock[joy.ordinal()] = true;
            doClickEvent(mClickArray[JoyEvent[joy.ordinal()]]);
        }
    }

    private void processJoystickInput(MotionEvent event) {
        checkKey(event.getAxisValue(MotionEvent.AXIS_GAS), ClickEvents.JoyLocks.RT);
        checkKey(event.getAxisValue(MotionEvent.AXIS_BRAKE), ClickEvents.JoyLocks.LT);
    }

    @Override
    public void onSingleTap(float x, float y) {
        doClickEvent(getValue(x, y, false));
    }

    @Override
    public void onLongPress(float x, float y) {
        doClickEvent(getValue(x, y, true));
    }

    private int getValue(float x, float y, boolean isLong) {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        int position = getCurPosition();
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition();
        }
        RetryDraweeView draweeView = ((ReaderAdapter.ImageHolder)
                Objects.requireNonNull(mRecyclerView.findViewHolderForAdapterPosition(position))).draweeView;
        float limitX = point.x / 3.0f;
        float limitY = point.y / 3.0f;
        if (x < limitX) {
            return isLong ? mLongClickArray[0] : mClickArray[0];
        } else if (x > 2 * limitX) {
            return isLong ? mLongClickArray[4] : mClickArray[4];
        } else if (y < limitY) {
            return isLong ? mLongClickArray[1] : mClickArray[1];
        } else if (y > 2 * limitY) {
            return isLong ? mLongClickArray[3] : mClickArray[3];
        } else if (!draweeView.retry()) {
            return isLong ? mLongClickArray[2] : mClickArray[2];
        }
        return 0;
    }

    private void doClickEvent(int value) {
        switch (value) {
            case ClickEvents.EVENT_PREV_PAGE:
                prevPage();
                break;
            case ClickEvents.EVENT_NEXT_PAGE:
                nextPage();
                break;
            case ClickEvents.EVENT_SAVE_PICTURE:
                savePicture();
                break;
            case ClickEvents.EVENT_LOAD_PREV:
                loadPrev();
                break;
            case ClickEvents.EVENT_LOAD_NEXT:
                loadNext();
                break;
            case ClickEvents.EVENT_EXIT_READER:
                exitReader();
                break;
            case ClickEvents.EVENT_TO_FIRST:
                toFirst();
                break;
            case ClickEvents.EVENT_TO_LAST:
                toLast();
                break;
            case ClickEvents.EVENT_SWITCH_SCREEN:
                switchScreen();
                break;
            case ClickEvents.EVENT_SWITCH_MODE:
                switchMode();
                break;
            case ClickEvents.EVENT_SWITCH_CONTROL:
                switchControl();
                break;
            case ClickEvents.EVENT_RELOAD_IMAGE:
                reloadImage();
                break;
            case ClickEvents.EVENT_SWITCH_NIGHT:
                switchNight();
                break;
        }
    }

    protected abstract int getCurPosition();

    protected abstract void prevPage();

    protected abstract void nextPage();

    protected void switchNight() {
        boolean night = !mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false);
        mPreference.putBoolean(PreferenceManager.PREF_NIGHT, night);
        if (mNightMask != null) {
            mNightMask.setVisibility(night ? View.VISIBLE : View.INVISIBLE);
        }
        vm.switchNight();
    }

    protected void reloadImage() {
        int position = getCurPosition();
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition();
        }
        ImageUrl image = mReaderAdapter.getItem(position);
        String rawUrl = image.getUrl();
        String postUrl = StringUtils.format("%s-post-%d", image.getUrl(), image.getId());
        ImagePipelineFactory factory = image.getSize() > App.mLargePixels ?
                mLargeImagePipelineFactory : mImagePipelineFactory;
        factory.getImagePipeline().evictFromCache(Uri.parse(rawUrl));
        factory.getImagePipeline().evictFromCache(Uri.parse(postUrl));
        mReaderAdapter.notifyItemChanged(position);
    }

    protected void savePicture() {
        if (isSavingPicture) {
            return;
        }
        isSavingPicture = true;

        int position = getCurPosition();
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition();
        }
        ImageUrl imageUrl = mReaderAdapter.getItem(position);
        String[] urls = imageUrl.getUrls();
        try {
            String title = mChapterTitle.getText().toString();
            for (String url : urls) {
                if (url.startsWith("file")) {
                    vm.savePicture(new FileInputStream(new File(Objects.requireNonNull(Uri.parse(url).getPath()))), url, title, progress);
                    return;
                } else if (url.startsWith("content")) {
                    vm.savePicture(getContentResolver().openInputStream(Uri.parse(url)), url, title, progress);
                    return;
                } else {
                    ImagePipelineFactory factory = imageUrl.getSize() > App.mLargePixels ?
                            mLargeImagePipelineFactory : mImagePipelineFactory;
                    BinaryResource resource = factory.getMainFileCache().getResource(new SimpleCacheKey(url));
                    if (resource != null) {
                        vm.savePicture(resource.openStream(), url, title, progress);
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        onPictureSaveFail();
    }

    protected void loadPrev() {
        vm.loadPrev();
    }

    protected void loadNext() {
        vm.loadNext();
    }

    protected void exitReader() {
        finish();
    }

    protected void toFirst() {
        mRecyclerView.scrollToPosition(0);
    }

    protected void toLast() {
        mRecyclerView.scrollToPosition(mReaderAdapter.getItemCount() - 1);
    }

    protected void switchScreen() {
        final int[] oArray = {ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};
        setRequestedOrientation(oArray[this.getResources().getConfiguration().orientation]);
    }

    protected void switchMode() {
        int targetMode = mode == PreferenceManager.READER_MODE_PAGE ?
                PreferenceManager.READER_MODE_STREAM : PreferenceManager.READER_MODE_PAGE;
        int targetTurn = targetMode == PreferenceManager.READER_MODE_PAGE ?
                PreferenceManager.READER_TURN_LTR : PreferenceManager.READER_TURN_ATB;
        applyReaderStyle(targetMode, targetTurn);
    }

    protected void applyReaderStyle(int targetMode, int targetTurn) {
        if (targetMode == PreferenceManager.READER_MODE_STREAM) {
            if (mode == PreferenceManager.READER_MODE_STREAM && turn == targetTurn) {
                return;
            }
        } else if (mode == PreferenceManager.READER_MODE_PAGE) {
            String turnKey = PreferenceManager.PREF_READER_PAGE_TURN;
            if (mPreference.getInt(turnKey, PreferenceManager.READER_TURN_LTR) == targetTurn) {
                return;
            }
        }
        mPreference.putInt(PreferenceManager.PREF_READER_MODE, targetMode);
        if (targetMode == PreferenceManager.READER_MODE_PAGE) {
            mPreference.putInt(PreferenceManager.PREF_READER_PAGE_TURN, targetTurn);
        } else {
            mPreference.putInt(PreferenceManager.PREF_READER_STREAM_TURN, targetTurn);
            mPreference.putBoolean(PreferenceManager.PREF_READER_STREAM_INTERVAL, false);
        }
        Intent intent = getIntent();
        if (targetMode == PreferenceManager.READER_MODE_PAGE) {
            intent.setClass(this, PageReaderActivity.class);
        } else {
            intent.setClass(this, StreamReaderActivity.class);
        }
        intent.putExtra(Extra.EXTRA_MODE, targetMode);
        finish();
        startActivity(intent);
    }

    protected void switchControl() {
        if (mProgressLayout.isShown()) {
            hideControl();
        } else {
            showControl();
        }
    }


    protected void bindReaderViews(TextView chapterTitle, TextView chapterPage, TextView battery,
                                   View progressLayout, View backLayout, View infoLayout,
                                   ReverseSeekBar seekBar, TextView loading, RecyclerView recycler,
                                   RelativeLayout box, View backBtn) {
        mChapterTitle = chapterTitle;
        mChapterPage = chapterPage;
        mBatteryText = battery;
        mProgressLayout = progressLayout;
        mBackLayout = backLayout;
        mInfoLayout = infoLayout;
        mSeekBar = seekBar;
        mLoadingText = loading;
        mRecyclerView = recycler;
        mReaderBox = box;
        backBtn.setOnClickListener(v -> onBackClick());
        if (mProgressLayout != null) {
            mProgressLayout.setId(R.id.reader_progress_layout);
        }
    }

    private void initReaderMenu() {
        if (mProgressLayout == null) {
            return;
        }
        mBrightnessBar = mProgressLayout.findViewById(R.id.reader_brightness_bar);
        mAutoBtn = mProgressLayout.findViewById(R.id.reader_menu_auto);
        View catalogBtn = mProgressLayout.findViewById(R.id.reader_menu_catalog);
        View colorBtn = mProgressLayout.findViewById(R.id.reader_menu_color);
        View settingsBtn = mProgressLayout.findViewById(R.id.reader_menu_settings);
        if (catalogBtn != null) {
            catalogBtn.setOnClickListener(v -> openCatalog());
        }
        if (mAutoBtn != null) {
            mAutoBtn.setOnClickListener(v -> onAutoClick());
        }
        if (colorBtn != null) {
            colorBtn.setOnClickListener(v -> openColor());
        }
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> openSettings());
        }
        if (mBrightnessBar != null) {
            int brightness = mPreference.getInt(PreferenceManager.PREF_READER_BRIGHTNESS, 0);
            mBrightnessBar.setProgress(brightness == 0 ? 50 : brightness);
            mBrightnessBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                    if (fromUser) {
                        applyBrightness(value);
                    }
                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                }
            });
        }
    }

    private void openCatalog() {
        List<Chapter> chapters = vm.chapters();
        if (chapters.isEmpty()) {
            return;
        }
        Chapter current = vm.currentChapter();
        String path = current != null ? current.getPath() : null;
        new ReaderChapterSheet(this, chapters, path, selected -> {
            hideControl();
            vm.jumpToChapter(selected);
        }).show();
    }

    private void onAutoClick() {
        if (autoPaging) {
            stopAutoPage();
            return;
        }
        int current = mPreference.getInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, 5);
        ReaderAutoDialog.show(this, current, interval -> {
            mPreference.putInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, interval);
            startAutoPage(interval);
        });
    }

    private void startAutoPage(int seconds) {
        stopAutoPage();
        autoPaging = true;
        updateAutoButton();
        hideControl();
        long delay = seconds * 1000L;
        autoHandler.postDelayed(autoRunnable, delay);
    }

    private void stopAutoPage() {
        autoPaging = false;
        autoHandler.removeCallbacks(autoRunnable);
        updateAutoButton();
    }

    private final Runnable autoRunnable = new Runnable() {
        @Override
        public void run() {
            if (!autoPaging) {
                return;
            }
            nextPage();
            int seconds = mPreference.getInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, 5);
            autoHandler.postDelayed(this, seconds * 1000L);
        }
    };

    private void updateAutoButton() {
        if (mAutoBtn == null) {
            return;
        }
        mAutoBtn.setTextColor(autoPaging ? ContextCompat.getColor(this, R.color.colorPrimaryBlue) : Color.WHITE);
    }

    private void openColor() {
        new ReaderColorSheet(this, mPreference, this::applyColorFilter).show();
    }

    private void openSettings() {
        new ReaderSettingsSheet(this, mPreference, mode, turn, this::applyBrightness, bottom -> {
            mPreference.putBoolean(PreferenceManager.PREF_READER_INFO_BOTTOM, bottom);
            applyInfoPosition();
        }, this::applyReaderStyle, this::applyWhiteEdge, this::applyStitch, this::applyPreload).show();
    }

    protected void applyWhiteEdge(boolean enabled) {
        mPreference.putBoolean(PreferenceManager.PREF_READER_WHITE_EDGE, enabled);
        if (mReaderAdapter != null) {
            mReaderAdapter.setWhiteEdge(enabled);
            mReaderAdapter.notifyDataSetChanged();
        }
    }

    protected void applyStitch(boolean enabled) {
        if (enabled) {
            applyReaderStyle(PreferenceManager.READER_MODE_STREAM, PreferenceManager.READER_TURN_ATB);
        } else {
            int pageTurn = mPreference.getInt(
                    PreferenceManager.PREF_READER_PAGE_TURN, PreferenceManager.READER_TURN_LTR);
            applyReaderStyle(PreferenceManager.READER_MODE_PAGE, pageTurn);
        }
    }

    protected void applyPreload() {
        vm.ensurePreload();
    }

    private void applyColorFilter() {
        if (mRecyclerView != null) {
            ReaderColorFilter.apply(mRecyclerView, mPreference);
        }
    }

    private void applySavedBrightness() {
        int brightness = mPreference.getInt(PreferenceManager.PREF_READER_BRIGHTNESS, 0);
        if (brightness > 0) {
            applyBrightness(brightness);
        }
    }

    protected void applyBrightness(int value) {
        mPreference.putInt(PreferenceManager.PREF_READER_BRIGHTNESS, value);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Math.max(0.01f, value / 100f);
        getWindow().setAttributes(lp);
        if (mBrightnessBar != null && mBrightnessBar.getProgress() != value) {
            mBrightnessBar.setProgress(value);
        }
    }

    protected void applyInfoPosition() {
        if (mInfoLayout == null || !(mInfoLayout.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
            return;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mInfoLayout.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        boolean bottom = mPreference.getBoolean(PreferenceManager.PREF_READER_INFO_BOTTOM, false);
        if (bottom) {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            if (mProgressLayout != null && mProgressLayout.getVisibility() == View.VISIBLE) {
                lp.addRule(RelativeLayout.ABOVE, mProgressLayout.getId());
                lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            } else {
                lp.removeRule(RelativeLayout.ABOVE);
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            }
        } else {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.removeRule(RelativeLayout.ABOVE);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        }
        mInfoLayout.setLayoutParams(lp);
    }

    protected void showLoadSuccess() {
        if (!mPreference.getBoolean(PreferenceManager.PREF_READER_HIDE_LOAD_TOAST, false)) {
            HintUtils.showToast(this, R.string.reader_load_success);
        }
    }

}
