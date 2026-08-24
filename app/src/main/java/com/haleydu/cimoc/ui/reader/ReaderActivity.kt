package com.haleydu.cimoc.ui.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.haleydu.cimoc.R
import com.haleydu.cimoc.global.ClickEvents
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.ui.common.BaseActivity
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.widget.OnTapGestureListener
import com.haleydu.cimoc.ui.widget.PreCacheLayoutManager
import com.haleydu.cimoc.ui.widget.ReverseSeekBar
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.StringUtils
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

abstract class ReaderActivity : BaseActivity(), OnTapGestureListener,
    DiscreteSeekBar.OnProgressChangeListener, ReaderAdapter.OnLazyLoadListener {

    protected lateinit var mLayoutManager: PreCacheLayoutManager
    protected lateinit var mReaderAdapter: ReaderAdapter
    protected val vm: ReaderViewModel by viewModels()

    @Inject
    lateinit var imageLoader: ImageLoader

    protected var mLastDx = 0
    protected var mLastDy = 0
    protected var pageProgress = 1
    protected var max = 1
    protected var turn = 0
    protected var orientation = 0
    protected var mode = 0
    protected var mLoadPrev = false
    protected var mLoadNext = false
    lateinit var mChapterTitle: TextView
    lateinit var mChapterPage: TextView
    lateinit var mBatteryText: TextView
    lateinit var mProgressLayout: View
    lateinit var mBackLayout: View
    lateinit var mInfoLayout: View
    lateinit var mSeekBar: ReverseSeekBar
    lateinit var mLoadingText: TextView
    lateinit var mRecyclerView: RecyclerView
    lateinit var mReaderBox: RelativeLayout
    private var mBrightnessBar: DiscreteSeekBar? = null
    private var mAutoBtn: TextView? = null
    private var mSourceBtn: View? = null
    private val autoHandler = Handler(Looper.getMainLooper())
    private var autoPaging = false
    private var isSavingPicture = false
    private var mHideInfo = false
    private var mClickArray: IntArray = IntArray(0)
    protected lateinit var overlay: ReaderOverlayManager
    private var mLongClickArray: IntArray = IntArray(0)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                val level = intent.getIntExtra("level", 0)
                val scale = intent.getIntExtra("scale", 100)
                mBatteryText.text = "${level * 100 / scale}%"
            }
        }
    }
    private val joyLock = booleanArrayOf(false, false)
    private val joyEvent = intArrayOf(7, 8)
    private var mControllerTrigThreshold = 0.3f

    override fun initTheme() {
        super.initTheme()
        mode = intent.getIntExtra(Extra.EXTRA_MODE, PreferenceManager.READER_MODE_PAGE)
        val key = if (mode == PreferenceManager.READER_MODE_PAGE) {
            PreferenceManager.PREF_READER_PAGE_ORIENTATION
        } else {
            PreferenceManager.PREF_READER_STREAM_ORIENTATION
        }
        orientation = mPreference.getInt(key, PreferenceManager.READER_ORIENTATION_PORTRAIT)
        val oArray = intArrayOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        )
        requestedOrientation = oArray[orientation]
    }

    override fun applyWindowInsets() {
    }

    override fun initView() {
        mHideInfo = mPreference.getBoolean(PreferenceManager.PREF_READER_HIDE_INFO, false)
        mControllerTrigThreshold =
            mPreference.getInt(PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD, 30) * 0.01f
        mInfoLayout.visibility = if (mHideInfo) View.INVISIBLE else View.VISIBLE
        setTheme(R.style.AppThemeNoDark)
        val key = if (mode == PreferenceManager.READER_MODE_PAGE) {
            PreferenceManager.PREF_READER_PAGE_TURN
        } else {
            PreferenceManager.PREF_READER_STREAM_TURN
        }
        turn = mPreference.getInt(key, PreferenceManager.READER_TURN_LTR)
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_WHITE_BACKGROUND, false)) {
            mReaderBox.setBackgroundResource(R.color.white)
        }
        initSeekBar()
        initLayoutManager()
        initReaderAdapter()
        mRecyclerView.itemAnimator = null
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mReaderAdapter
        mRecyclerView.setItemViewCacheSize(2)
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                mLastDx = dx
                mLastDy = dy
            }
        })
        initReaderMenu()
        overlay = ReaderOverlayManager(window, mPreference, mNightMask)
        overlay.init()
        overlay.applySavedBrightness(mBrightnessBar)
        applyColorFilter()
        applyInfoPosition()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (::overlay.isInitialized) {
            overlay.applySystemBars()
        }
    }

    private fun initSeekBar() {
        mSeekBar.setReverse(turn == PreferenceManager.READER_TURN_RTL)
        mSeekBar.setOnProgressChangeListener(this)
    }

    private fun initReaderAdapter() {
        mReaderAdapter = ReaderAdapter(this, mutableListOf())
        mReaderAdapter.setTapGestureListener(this)
        mReaderAdapter.setLazyLoadListener(this)
        mReaderAdapter.setScaleFactor(
            mPreference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200) * 0.01f
        )
        mReaderAdapter.setDoubleTap(
            !mPreference.getBoolean(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false)
        )
        mReaderAdapter.setVertical(turn == PreferenceManager.READER_TURN_ATB)
        val paging = mode != PreferenceManager.READER_MODE_STREAM &&
            mPreference.getBoolean(PreferenceManager.PREF_READER_PAGING, false)
        mReaderAdapter.setPaging(paging)
        mReaderAdapter.setCloseAutoResizeImage(
            mPreference.getBoolean(PreferenceManager.PREF_READER_CLOSEAUTORESIZEIMAGE, false)
        )
        mReaderAdapter.setPagingReverse(
            mPreference.getBoolean(PreferenceManager.PREF_READER_PAGING_REVERSE, false)
        )
        mReaderAdapter.setWhiteEdge(
            mPreference.getBoolean(PreferenceManager.PREF_READER_WHITE_EDGE, false)
        )
        mReaderAdapter.setBanTurn(
            mPreference.getBoolean(PreferenceManager.PREF_READER_PAGE_BAN_TURN, false)
        )
        mReaderAdapter.setStreamTile(mode == PreferenceManager.READER_MODE_STREAM)
        if (::imageLoader.isInitialized) {
            mReaderAdapter.setImageLoader(imageLoader)
        }
    }

    private fun initLayoutManager() {
        mLayoutManager = PreCacheLayoutManager(this)
        mLayoutManager.orientation =
            if (turn == PreferenceManager.READER_TURN_ATB) {
                LinearLayoutManager.VERTICAL
            } else {
                LinearLayoutManager.HORIZONTAL
            }
        mLayoutManager.reverseLayout = turn == PreferenceManager.READER_TURN_RTL
        mLayoutManager.setExtraSpace(2)
    }

    override fun initData() {
        try {
            mClickArray = if (mode == PreferenceManager.READER_MODE_PAGE) {
                ClickEvents.getPageClickEventChoice(mPreference)
            } else {
                ClickEvents.getStreamClickEventChoice(mPreference)
            }
            mLongClickArray = if (mode == PreferenceManager.READER_MODE_PAGE) {
                ClickEvents.getPageLongClickEventChoice(mPreference)
            } else {
                ClickEvents.getStreamLongClickEventChoice(mPreference)
            }
            vm.events.collectOnStart(this) { event ->
                when (event) {
                    is ReaderViewModel.Event.ParseError -> onParseError()
                    is ReaderViewModel.Event.NextLoadNone -> onNextLoadNone()
                    is ReaderViewModel.Event.PrevLoadNone -> onPrevLoadNone()
                    is ReaderViewModel.Event.NextLoading -> onNextLoading()
                    is ReaderViewModel.Event.PrevLoading -> onPrevLoading()
                    is ReaderViewModel.Event.NextLoadSuccess ->
                        onNextLoadSuccess(event.list, event.silent)
                    is ReaderViewModel.Event.PrevLoadSuccess -> onPrevLoadSuccess(event.list)
                    is ReaderViewModel.Event.InitLoadSuccess ->
                        onInitLoadSuccess(event.list, event.progress, event.source, event.local)
                    is ReaderViewModel.Event.ChapterChange -> onChapterChange(event.chapter)
                    is ReaderViewModel.Event.ImageLoadSuccess ->
                        onImageLoadSuccess(event.id, event.url)
                    is ReaderViewModel.Event.ImageLoadFail -> onImageLoadFail(event.id)
                    is ReaderViewModel.Event.PictureSaveSuccess -> onPictureSaveSuccess(event.uri)
                    is ReaderViewModel.Event.PictureSaveFail -> onPictureSaveFail()
                    is ReaderViewModel.Event.PicturePaging -> onPicturePaging(event.image, event.tiles)
                    is ReaderViewModel.Event.ScrollToStart -> {
                        toFirst()
                        pageProgress = 1
                        updateProgress()
                    }
                }
            }
            val id = intent.getLongExtra(Extra.EXTRA_ID, -1)
            val list = ChapterListHolder.get(id)
                ?: ChapterListHolder.get()
                ?: intent.getParcelableArrayListExtra<Chapter>(Extra.EXTRA_CHAPTER)
            vm.loadInit(id, list?.toTypedArray() ?: emptyArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        vm.dispatch(ReaderViewModel.ReaderIntent.FlushProgress)
        unregisterReceiver(batteryReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoPage()
        autoHandler.removeCallbacksAndMessages(null)
    }

    fun onBackClick() {
        onBackPressed()
    }

    abstract override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean)

    override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {
    }

    override fun onLoad(imageUrl: ImageUrl) {
        vm.lazyLoad(imageUrl)
    }

    protected fun hideControl() {
        if (!mProgressLayout.isShown) {
            return
        }
        val upAction = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f
        )
        upAction.duration = 300
        val downAction = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 1.0f
        )
        downAction.duration = 300
        mProgressLayout.startAnimation(downAction)
        mProgressLayout.visibility = View.INVISIBLE
        mBackLayout.startAnimation(upAction)
        mBackLayout.visibility = View.INVISIBLE
        if (mHideInfo) {
            mInfoLayout.startAnimation(upAction)
            mInfoLayout.visibility = View.INVISIBLE
        }
        applyInfoPosition()
    }

    protected fun showControl() {
        val upAction = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        )
        upAction.duration = 300
        val downAction = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        )
        downAction.duration = 300
        if (mSeekBar.max != max) {
            mSeekBar.max = max
            mSeekBar.progress = max
        }
        mSeekBar.progress = pageProgress
        mProgressLayout.startAnimation(upAction)
        mProgressLayout.visibility = View.VISIBLE
        mBackLayout.startAnimation(downAction)
        mBackLayout.visibility = View.VISIBLE
        if (mHideInfo) {
            mInfoLayout.startAnimation(downAction)
            mInfoLayout.visibility = View.VISIBLE
        }
        applyInfoPosition()
    }

    protected fun updateProgress() {
        mChapterPage.text = StringUtils.getProgress(pageProgress, max)
        vm.dispatch(ReaderViewModel.ReaderIntent.PageChanged(pageProgress))
    }

    fun onPicturePaging(image: ImageUrl, tiles: Int) {
        val pos = mReaderAdapter.getPositionById(image.id)
        if (pos < 0) {
            return
        }
        val count = tiles.coerceAtLeast(2)
        val firstExtraId = image.id + 900L
        if (mReaderAdapter.getPositionById(firstExtraId) >= 0) {
            return
        }
        for (i in 2..count) {
            mReaderAdapter.add(
                pos + i - 1,
                ImageUrl(
                    image.id + 900L * (i - 1),
                    image.comicChapter,
                    image.num,
                    image.urls,
                    image.chapter,
                    i,
                    false
                )
            )
        }
    }

    fun onParseError() {
        mLoadingText.visibility = View.GONE
        HintUtils.showToast(this, R.string.common_parse_error)
    }

    protected fun setReaderAdapter(list: List<ImageUrl>) {
        mReaderAdapter.setImageLoader(imageLoader)
        mReaderAdapter.setHeaders(vm.parserHeader(list))
    }

    fun onNextLoadSuccess(list: List<ImageUrl>) {
        onNextLoadSuccess(list, false)
    }

    open fun onNextLoadSuccess(list: List<ImageUrl>, silent: Boolean) {
        setReaderAdapter(list)
        mReaderAdapter.addAll(list)
        if (!silent) {
            showLoadSuccess()
        }
    }

    open fun onPrevLoadSuccess(list: List<ImageUrl>) {
        setReaderAdapter(list)
        mReaderAdapter.addAll(0, list)
        showLoadSuccess()
    }

    open fun onInitLoadSuccess(list: List<ImageUrl>, progress: Int, source: Int, local: Boolean) {
        setReaderAdapter(list)
        mReaderAdapter.clear()
        mReaderAdapter.addAll(list)
        if (progress != 1) {
            mRecyclerView.scrollToPosition(progress - 1)
        } else {
            mRecyclerView.scrollToPosition(0)
        }
        mLoadingText.visibility = View.GONE
        mRecyclerView.visibility = View.VISIBLE
        this.pageProgress = progress.coerceAtLeast(1)
        updateProgress()
        mSourceBtn?.visibility = if (local) View.GONE else View.VISIBLE
        prefetchAround((progress - 1).coerceAtLeast(0))
    }

    fun onChapterChange(chapter: Chapter) {
        max = chapter.count
        val title = chapter.title
        val titleLengthMax = 15
        mChapterTitle.text = if (title.length > titleLengthMax) {
            title.substring(0, titleLengthMax).plus("...")
        } else {
            title
        }
    }

    fun onImageLoadSuccess(id: Long, url: String) {
        mReaderAdapter.update(id, url)
        val position = mReaderAdapter.getPositionById(id)
        if (position >= 0) {
            prefetchAround(position)
        }
    }

    fun onImageLoadFail(id: Long) {
        mReaderAdapter.update(id, null)
    }

    fun onPictureSaveSuccess(uri: Uri) {
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        isSavingPicture = false
        HintUtils.showToast(this, R.string.reader_picture_save_success)
    }

    fun onPictureSaveFail() {
        isSavingPicture = false
        HintUtils.showToast(this, R.string.reader_picture_save_fail)
    }

    fun onPrevLoading() {
        HintUtils.showToast(this, R.string.reader_load_prev)
    }

    fun onPrevLoadNone() {
        HintUtils.showToast(this, R.string.reader_prev_none)
    }

    fun onNextLoading() {
        HintUtils.showToast(this, R.string.reader_load_next)
    }

    fun onNextLoadNone() {
        stopAutoPage()
        HintUtils.showToast(this, R.string.reader_next_none)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (mReaderAdapter.itemCount != 0 && ::overlay.isInitialized) {
            val value = overlay.mapKey(keyCode, mClickArray)
            if (value != ClickEvents.EVENT_NULL) {
                doClickEvent(value)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val historySize = event.historySize
            for (i in 0 until historySize) {
                processJoystickInput(event)
            }
            processJoystickInput(event)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun checkKey(value: Float, joy: ClickEvents.JoyLocks) {
        if (joyLock[joy.ordinal] && value < mControllerTrigThreshold) {
            joyLock[joy.ordinal] = false
        }
        if (!joyLock[joy.ordinal] && value > mControllerTrigThreshold) {
            joyLock[joy.ordinal] = true
            doClickEvent(mClickArray[joyEvent[joy.ordinal]])
        }
    }

    private fun processJoystickInput(event: MotionEvent) {
        checkKey(event.getAxisValue(MotionEvent.AXIS_GAS), ClickEvents.JoyLocks.RT)
        checkKey(event.getAxisValue(MotionEvent.AXIS_BRAKE), ClickEvents.JoyLocks.LT)
    }

    override fun onSingleTap(x: Float, y: Float) {
        doClickEvent(getValue(x, y, false))
    }

    override fun onLongPress(x: Float, y: Float) {
        doClickEvent(getValue(x, y, true))
    }

    private fun getValue(x: Float, y: Float, isLong: Boolean): Int {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        var position = getCurPosition()
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition()
        }
        val holder = mRecyclerView.findViewHolderForAdapterPosition(position)
            as? ReaderAdapter.ImageHolder ?: return 0
        val limitX = point.x / 3.0f
        val limitY = point.y / 3.0f
        return when {
            x < limitX -> if (isLong) mLongClickArray[0] else mClickArray[0]
            x > 2 * limitX -> if (isLong) mLongClickArray[4] else mClickArray[4]
            y < limitY -> if (isLong) mLongClickArray[1] else mClickArray[1]
            y > 2 * limitY -> if (isLong) mLongClickArray[3] else mClickArray[3]
            !holder.retry() -> if (isLong) mLongClickArray[2] else mClickArray[2]
            else -> 0
        }
    }

    private fun doClickEvent(value: Int) {
        when (value) {
            ClickEvents.EVENT_PREV_PAGE -> prevPage()
            ClickEvents.EVENT_NEXT_PAGE -> nextPage()
            ClickEvents.EVENT_SAVE_PICTURE -> savePicture()
            ClickEvents.EVENT_LOAD_PREV -> loadPrev()
            ClickEvents.EVENT_LOAD_NEXT -> loadNext()
            ClickEvents.EVENT_EXIT_READER -> exitReader()
            ClickEvents.EVENT_TO_FIRST -> toFirst()
            ClickEvents.EVENT_TO_LAST -> toLast()
            ClickEvents.EVENT_SWITCH_SCREEN -> switchScreen()
            ClickEvents.EVENT_SWITCH_MODE -> switchMode()
            ClickEvents.EVENT_SWITCH_CONTROL -> switchControl()
            ClickEvents.EVENT_RELOAD_IMAGE -> reloadImage()
            ClickEvents.EVENT_SWITCH_NIGHT -> switchNight()
        }
    }

    protected abstract fun getCurPosition(): Int

    protected abstract fun prevPage()

    protected abstract fun nextPage()

    protected fun switchNight() {
        overlay.switchNight()
        vm.switchNight()
    }

    protected fun reloadImage() {
        var position = getCurPosition()
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition()
        }
        val image = mReaderAdapter.getItem(position)
        mReaderAdapter.evict(image.url)
        mReaderAdapter.notifyItemChanged(position)
    }

    @OptIn(ExperimentalCoilApi::class)
    protected fun savePicture() {
        if (isSavingPicture) {
            return
        }
        isSavingPicture = true
        var position = getCurPosition()
        if (position == -1) {
            position = mLayoutManager.findFirstVisibleItemPosition()
        }
        val imageUrl = mReaderAdapter.getItem(position)
        val urls = imageUrl.urls
        try {
            val title = mChapterTitle.text.toString()
            for (url in urls) {
                if (url.startsWith("file")) {
                    vm.savePicture(
                        FileInputStream(File(Uri.parse(url).path!!)),
                        url,
                        title,
                        pageProgress
                    )
                    return
                } else if (url.startsWith("content")) {
                    val stream = contentResolver.openInputStream(Uri.parse(url)) ?: continue
                    vm.savePicture(stream, url, title, pageProgress)
                    return
                } else {
                    val snapshot = imageLoader.diskCache?.openSnapshot(url)
                    if (snapshot != null) {
                        snapshot.use {
                            vm.savePicture(it.data.toFile().inputStream(), url, title, pageProgress)
                        }
                        return
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        onPictureSaveFail()
    }

    protected fun loadPrev() {
        vm.loadPrev()
    }

    protected fun loadNext() {
        vm.loadNext()
    }

    protected fun exitReader() {
        finish()
    }

    protected fun toFirst() {
        mRecyclerView.scrollToPosition(0)
    }

    protected fun toLast() {
        mRecyclerView.scrollToPosition(mReaderAdapter.itemCount - 1)
    }

    protected fun switchScreen() {
        val oArray = intArrayOf(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        )
        requestedOrientation = oArray[resources.configuration.orientation]
    }

    protected fun switchMode() {
        val targetMode = if (mode == PreferenceManager.READER_MODE_PAGE) {
            PreferenceManager.READER_MODE_STREAM
        } else {
            PreferenceManager.READER_MODE_PAGE
        }
        val targetTurn = if (targetMode == PreferenceManager.READER_MODE_PAGE) {
            PreferenceManager.READER_TURN_LTR
        } else {
            PreferenceManager.READER_TURN_ATB
        }
        applyReaderStyle(targetMode, targetTurn)
    }

    protected fun applyReaderStyle(targetMode: Int, targetTurn: Int) {
        if (targetMode == PreferenceManager.READER_MODE_STREAM) {
            if (mode == PreferenceManager.READER_MODE_STREAM && turn == targetTurn) {
                return
            }
        } else if (mode == PreferenceManager.READER_MODE_PAGE) {
            val turnKey = PreferenceManager.PREF_READER_PAGE_TURN
            if (mPreference.getInt(turnKey, PreferenceManager.READER_TURN_LTR) == targetTurn) {
                return
            }
        }
        mPreference.putInt(PreferenceManager.PREF_READER_MODE, targetMode)
        if (targetMode == PreferenceManager.READER_MODE_PAGE) {
            mPreference.putInt(PreferenceManager.PREF_READER_PAGE_TURN, targetTurn)
        } else {
            mPreference.putInt(PreferenceManager.PREF_READER_STREAM_TURN, targetTurn)
            mPreference.putBoolean(PreferenceManager.PREF_READER_STREAM_INTERVAL, false)
        }
        val next = intent
        if (targetMode == PreferenceManager.READER_MODE_PAGE) {
            next.setClass(this, PageReaderActivity::class.java)
        } else {
            next.setClass(this, StreamReaderActivity::class.java)
        }
        next.putExtra(Extra.EXTRA_MODE, targetMode)
        finish()
        startActivity(next)
    }

    protected fun switchControl() {
        if (mProgressLayout.isShown) {
            hideControl()
        } else {
            showControl()
        }
    }

    protected fun bindReaderViews(
        chapterTitle: TextView,
        chapterPage: TextView,
        battery: TextView,
        progressLayout: View,
        backLayout: View,
        infoLayout: View,
        seekBar: ReverseSeekBar,
        loading: TextView,
        recycler: RecyclerView,
        box: RelativeLayout,
        backBtn: View
    ) {
        mChapterTitle = chapterTitle
        mChapterPage = chapterPage
        mBatteryText = battery
        mProgressLayout = progressLayout
        mBackLayout = backLayout
        mInfoLayout = infoLayout
        mSeekBar = seekBar
        mLoadingText = loading
        mRecyclerView = recycler
        mReaderBox = box
        backBtn.setOnClickListener { onBackClick() }
        mProgressLayout.id = R.id.reader_progress_layout
    }

    private fun initReaderMenu() {
        mBrightnessBar = mProgressLayout.findViewById(R.id.reader_brightness_bar)
        mAutoBtn = mProgressLayout.findViewById(R.id.reader_menu_auto)
        val catalogBtn = mProgressLayout.findViewById<View>(R.id.reader_menu_catalog)
        val colorBtn = mProgressLayout.findViewById<View>(R.id.reader_menu_color)
        val settingsBtn = mProgressLayout.findViewById<View>(R.id.reader_menu_settings)
        mSourceBtn = mProgressLayout.findViewById(R.id.reader_menu_source)
        catalogBtn?.setOnClickListener { openCatalog() }
        mAutoBtn?.setOnClickListener { onAutoClick() }
        colorBtn?.setOnClickListener { openColor() }
        settingsBtn?.setOnClickListener { openSettings() }
        mSourceBtn?.setOnClickListener { openSourceSearch() }
        mBrightnessBar?.let { bar ->
            val brightness = mPreference.getInt(PreferenceManager.PREF_READER_BRIGHTNESS, 0)
            bar.progress = if (brightness == 0) 50 else brightness
            bar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
                override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        overlay.applyBrightness(value, mBrightnessBar)
                    }
                }

                override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {
                }
            })
        }
    }

    private fun openSourceSearch() {
        val title = vm.comicTitle()
        if (StringUtils.isEmpty(title)) {
            HintUtils.showToast(this, R.string.common_keyword_empty)
            return
        }
        startActivity(
            ResultActivity.createIntent(
                this,
                title,
                null,
                ResultActivity.LAUNCH_MODE_SEARCH
            )
        )
    }

    private fun openCatalog() {
        val chapters = vm.chapters()
        if (chapters.isEmpty()) {
            return
        }
        val current = vm.currentChapter()
        val path = current?.path
        ReaderChapterSheet(this, chapters, path) { selected ->
            hideControl()
            vm.jumpToChapter(selected)
        }.show()
    }

    private fun onAutoClick() {
        if (autoPaging) {
            stopAutoPage()
            return
        }
        val current = mPreference.getInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, 5)
        ReaderAutoDialog.show(this, current) { interval ->
            mPreference.putInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, interval)
            startAutoPage(interval)
        }
    }

    private fun startAutoPage(seconds: Int) {
        stopAutoPage()
        autoPaging = true
        updateAutoButton()
        hideControl()
        autoHandler.postDelayed(autoRunnable, seconds * 1000L)
    }

    private fun stopAutoPage() {
        autoPaging = false
        autoHandler.removeCallbacks(autoRunnable)
        updateAutoButton()
    }

    private val autoRunnable = object : Runnable {
        override fun run() {
            if (!autoPaging) {
                return
            }
            nextPage()
            val seconds = mPreference.getInt(PreferenceManager.PREF_READER_AUTO_INTERVAL, 5)
            autoHandler.postDelayed(this, seconds * 1000L)
        }
    }

    private fun updateAutoButton() {
        val btn = mAutoBtn ?: return
        btn.setTextColor(
            if (autoPaging) {
                ContextCompat.getColor(this, R.color.colorPrimaryBlue)
            } else {
                Color.WHITE
            }
        )
    }

    private fun openColor() {
        ReaderColorSheet(this, mPreference) { applyColorFilter() }.show()
    }

    private fun openSettings() {
        ReaderSettingsSheet(
            this,
            mPreference,
            mode,
            turn,
            { overlay.applyBrightness(it, mBrightnessBar) },
            { bottom ->
                mPreference.putBoolean(PreferenceManager.PREF_READER_INFO_BOTTOM, bottom)
                applyInfoPosition()
            },
            { targetMode, targetTurn -> applyReaderStyle(targetMode, targetTurn) },
            { applyWhiteEdge(it) },
            { applyStitch(it) },
            { applyPreload() }
        ).show()
    }

    protected fun applyWhiteEdge(enabled: Boolean) {
        mPreference.putBoolean(PreferenceManager.PREF_READER_WHITE_EDGE, enabled)
        mReaderAdapter.setWhiteEdge(enabled)
        mReaderAdapter.notifyDataSetChanged()
    }

    protected fun applyStitch(enabled: Boolean) {
        if (enabled) {
            applyReaderStyle(PreferenceManager.READER_MODE_STREAM, PreferenceManager.READER_TURN_ATB)
        } else {
            val pageTurn = mPreference.getInt(
                PreferenceManager.PREF_READER_PAGE_TURN,
                PreferenceManager.READER_TURN_LTR
            )
            applyReaderStyle(PreferenceManager.READER_MODE_PAGE, pageTurn)
        }
    }

    protected fun applyPreload() {
        vm.ensurePreload()
    }

    private fun applyColorFilter() {
        ReaderColorFilter.apply(mRecyclerView, mPreference)
    }

    @OptIn(ExperimentalCoilApi::class)
    protected fun prefetchAround(position: Int) {
        if (position < 0 || mReaderAdapter.itemCount == 0) {
            return
        }
        val last = (position + 3).coerceAtMost(mReaderAdapter.itemCount - 1)
        val urls = ArrayList<String>()
        for (i in (position + 1)..last) {
            val image = mReaderAdapter.getItem(i) ?: continue
            if (image.isLazy) {
                if (!image.isLoading) {
                    image.isLoading = true
                    vm.lazyLoad(image)
                }
                continue
            }
            val imageUrls = image.urls ?: continue
            for (url in imageUrls) {
                if (!url.isNullOrEmpty()) {
                    urls.add(url)
                }
            }
        }
        mReaderAdapter.prefetch(urls)
    }

    protected fun applyInfoPosition() {
        val lp = mInfoLayout.layoutParams as? RelativeLayout.LayoutParams ?: return
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        val bottom = mPreference.getBoolean(PreferenceManager.PREF_READER_INFO_BOTTOM, false)
        if (bottom) {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            if (mProgressLayout.visibility == View.VISIBLE) {
                lp.addRule(RelativeLayout.ABOVE, mProgressLayout.id)
                lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            } else {
                lp.removeRule(RelativeLayout.ABOVE)
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
        } else {
            lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            lp.removeRule(RelativeLayout.ABOVE)
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }
        mInfoLayout.layoutParams = lp
    }

    protected fun showLoadSuccess() {
        if (!mPreference.getBoolean(PreferenceManager.PREF_READER_HIDE_LOAD_TOAST, false)) {
            HintUtils.showToast(this, R.string.reader_load_success)
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, id: Long, list: List<Chapter>, mode: Int): Intent {
            ChapterListHolder.put(id, list)
            val intent = readerIntent(context, mode)
            intent.putExtra(Extra.EXTRA_ID, id)
            intent.putExtra(Extra.EXTRA_MODE, mode)
            return intent
        }

        private fun readerIntent(context: Context, mode: Int): Intent {
            return if (mode == PreferenceManager.READER_MODE_PAGE) {
                Intent(context, PageReaderActivity::class.java)
            } else {
                Intent(context, StreamReaderActivity::class.java)
            }
        }
    }
}
