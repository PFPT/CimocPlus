package com.haleydu.cimoc.ui.detail
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityChapterBinding
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.misc.Switcher
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.ui.common.BackActivity
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.reader.ChapterListHolder
import com.haleydu.cimoc.ui.widget.ViewUtils
import com.haleydu.cimoc.utils.PermissionUtils

class ChapterActivity : BackActivity(), BaseAdapter.OnItemClickListener {

    lateinit var mRecyclerView: RecyclerView
    private lateinit var binding: ActivityChapterBinding

    private lateinit var mChapterAdapter: ChapterAdapter
    private var isAscendMode = false
    private var isButtonMode = false
    private val mHandler = Handler(Looper.getMainLooper())
    private val mListener: RecyclerView.OnItemTouchListener = CustomTouchListener()
    private lateinit var mDecoration: RecyclerView.ItemDecoration

    override fun initView() {
        isButtonMode = mPreference.getBoolean(PreferenceManager.PREF_CHAPTER_BUTTON_MODE, false)
        mChapterAdapter = ChapterAdapter(this, getAdapterList())
        mDecoration = mChapterAdapter.getItemDecoration()
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.itemAnimator = null
        mRecyclerView.adapter = mChapterAdapter
        switchMode()
    }

    private fun switchMode() {
        mChapterAdapter.setButtonMode(isButtonMode)
        if (isButtonMode) {
            mChapterAdapter.setOnItemClickListener(null)
            mRecyclerView.layoutManager = GridLayoutManager(this, 4)
            mRecyclerView.addItemDecoration(mDecoration)
            mRecyclerView.addOnItemTouchListener(mListener)
            mRecyclerView.setPadding(
                ViewUtils.dpToPixel(4f, this).toInt(),
                ViewUtils.dpToPixel(10f, this).toInt(),
                ViewUtils.dpToPixel(4f, this).toInt(),
                0
            )
        } else {
            mChapterAdapter.setOnItemClickListener(this)
            mRecyclerView.layoutManager = LinearLayoutManager(this)
            mRecyclerView.removeItemDecoration(mDecoration)
            mRecyclerView.removeOnItemTouchListener(mListener)
            mRecyclerView.setPadding(0, 0, 0, 0)
        }
    }

    private fun getAdapterList(): MutableList<Switcher<Chapter>> {
        isAscendMode = mPreference.getBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, false)
        var list: List<Chapter>? = ChapterListHolder.get()
        if (list == null) {
            list = IntentCompat.getParcelableArrayListExtra(intent, Extra.EXTRA_CHAPTER, Chapter::class.java)
        }
        if (list == null) {
            list = ArrayList()
        }
        val result = ArrayList<Switcher<Chapter>>(list.size)
        for (i in list.indices) {
            result.add(Switcher(list[i], list[i].download))
        }
        return result
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chapter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isProgressBarShown()) {
            when (item.itemId) {
                R.id.chapter_all -> {
                    for (switcher in mChapterAdapter.getDateSet()) {
                        switcher.isEnable = true
                    }
                    mChapterAdapter.notifyDataSetChanged()
                }
                R.id.chapter_sort -> {
                    mChapterAdapter.reverse()
                    isAscendMode = !isAscendMode
                    mPreference.putBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, isAscendMode)
                }
                R.id.chapter_switch_view -> {
                    isButtonMode = !isButtonMode
                    switchMode()
                    mPreference.putBoolean(PreferenceManager.PREF_CHAPTER_BUTTON_MODE, isButtonMode)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val switcher = mChapterAdapter.getItem(position)
        if (!switcher.element.download) {
            switcher.switchEnable()
            mChapterAdapter.notifyItemChanged(position)
        }
    }

    fun onActionButtonClick() {
        val list = ArrayList<Chapter>()
        for (switcher in mChapterAdapter.getDateSet()) {
            if (!switcher.element.download && switcher.isEnable) {
                list.add(switcher.element)
            }
        }

        if (list.isEmpty()) {
            showSnackbar(R.string.chapter_download_empty)
        } else if (PermissionUtils.hasStoragePermission(this)) {
            ChapterListHolder.put(list)
            setResult(Activity.RESULT_OK, Intent())
            finish()
        } else {
            showSnackbar(R.string.chapter_download_perm_fail)
        }
    }

    override fun inflateContentView(): View {
        binding = ActivityChapterBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_chapter
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.chapter)
    }

    inner class CustomTouchListener : RecyclerView.OnItemTouchListener {
        private var mLastPosition = -1
        private var isLongPress = false

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            if (isLongPress) {
                return true
            }

            val child = rv.findChildViewUnder(e.x, e.y)
            val pos = if (child == null) RecyclerView.NO_POSITION else rv.getChildAdapterPosition(child)
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    mLastPosition = pos
                    if (mLastPosition != -1) {
                        mHandler.postDelayed({
                            isLongPress = true
                            update(mLastPosition)
                        }, 500)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mLastPosition != pos) {
                        mHandler.removeCallbacksAndMessages(null)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mHandler.removeCallbacksAndMessages(null)
                    if (pos != -1 && mLastPosition == pos) {
                        update(pos)
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            val child = rv.findChildViewUnder(e.x, e.y)
            val pos = if (child == null) RecyclerView.NO_POSITION else rv.getChildAdapterPosition(child)
            when (e.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (pos != -1 && mLastPosition != pos) {
                        update(pos)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isLongPress = false
                }
            }
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        }

        private fun update(pos: Int) {
            mLastPosition = pos
            mChapterAdapter.getItem(pos).switchEnable()
            mChapterAdapter.notifyItemChanged(pos)
        }
    }

    override fun bindViews() {
        super.bindViews()
        mRecyclerView = binding.chapterRecyclerView
        binding.chapterActionButton.setOnClickListener { onActionButtonClick() }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, list: ArrayList<Chapter>): Intent {
            ChapterListHolder.put(list)
            return Intent(context, ChapterActivity::class.java)
        }
    }
}
