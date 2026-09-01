package com.haleydu.cimoc.ui.library
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivityPartFavoriteBinding
import com.haleydu.cimoc.data.TagManager
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.ui.common.BackActivity
import com.haleydu.cimoc.ui.common.BaseAdapter
import com.haleydu.cimoc.ui.common.GridAdapter
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.ui.common.dialog.MultiDialogFragment
import com.haleydu.cimoc.ui.detail.DetailActivity
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PartFavoriteActivity : BackActivity(), DialogCaller, BaseAdapter.OnItemClickListener,
    BaseAdapter.OnItemLongClickListener {

    lateinit var mRecyclerView: RecyclerView

    private lateinit var vm: PartFavoriteViewModel
    private lateinit var binding: ActivityPartFavoriteBinding
    private lateinit var mGridAdapter: GridAdapter

    private var mSavedComic: MiniComic? = null
    private var isDeletable = false

    override fun initViewModel() {
        vm = ViewModelProvider(this)[PartFavoriteViewModel::class.java]
    }

    override fun initView() {
        super.initView()
        mGridAdapter = GridAdapter(this)
        mGridAdapter.setSymbol(true)
        mGridAdapter.setProvider((application as App).builderProvider)
        mGridAdapter.setTitleGetter(vm.titleGetter())
        mGridAdapter.setOnItemClickListener(this)
        mGridAdapter.setOnItemLongClickListener(this)
        mRecyclerView.layoutManager = GridLayoutManager(this, 3)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(mGridAdapter.getItemDecoration())
        mRecyclerView.adapter = mGridAdapter
    }

    override fun initData() {
        val id = intent.getLongExtra(Extra.EXTRA_ID, -1)
        isDeletable = id != TagManager.TAG_CONTINUE && id != TagManager.TAG_FINISH
        vm.comics.collectOnStart(this) { onComicLoadSuccess(it) }
        vm.loadFail.collectOnStart(this) { onComicLoadFail() }
        vm.titles.collectOnStart(this) { onComicTitleLoadSuccess(it) }
        vm.titleFail.collectOnStart(this) { onComicTitleLoadFail() }
        vm.insertSuccess.collectOnStart(this) { onComicInsertSuccess(it) }
        vm.insertFail.collectOnStart(this) { onComicInsertFail() }
        AppEventBus.observe(AppEvent.EVENT_COMIC_UNFAVORITE).collectOnStart(this) { event ->
            onComicRemove(event.data as Long)
        }
        AppEventBus.observe(AppEvent.EVENT_TAG_UPDATE).collectOnStart(this) { event ->
            val comicId = event.data as Long
            @Suppress("UNCHECKED_CAST")
            val list = event.getData(1) as List<Long>
            if (list.contains(id)) {
                onComicAdd(MiniComic(vm.loadComic(comicId)))
            } else {
                onComicRemove(comicId)
            }
        }
        AppEventBus.observe(AppEvent.EVENT_COMIC_CANCEL_HIGHLIGHT).collectOnStart(this) { event ->
            onHighlightCancel(event.data as MiniComic)
        }
        AppEventBus.observe(AppEvent.EVENT_COMIC_READ).collectOnStart(this) { event ->
            onComicRead(event.data as MiniComic)
        }
        vm.load(id)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (intent.getLongExtra(Extra.EXTRA_ID, -1) >= 0) {
            menuInflater.inflate(R.menu.menu_part_favorite, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.part_favorite_add -> {
                showProgressDialog()
                vm.loadComicTitle(mGridAdapter.dateSet)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(view: View, position: Int) {
        val comic = mGridAdapter.comicAt(position) ?: return
        val intent = DetailActivity.createIntent(this, comic.id, comic.source, comic.cid)
        startActivity(intent)
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        if (isDeletable) {
            val comic = mGridAdapter.comicAt(position) ?: return true
            mSavedComic = comic
            val fragment = MessageDialogFragment.newInstance(
                R.string.dialog_confirm,
                R.string.part_favorite_delete_confirm,
                true,
                DIALOG_REQUEST_DELETE
            )
            fragment.show(supportFragmentManager, null)
            return true
        }
        return false
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_DELETE -> {
                val comic = mSavedComic ?: return
                val id = comic.id ?: return
                vm.delete(id)
                mGridAdapter.remove(comic)
                HintUtils.showToast(this, R.string.common_execute_success)
            }
            DIALOG_REQUEST_ADD -> {
                showProgressDialog()
                val check = bundle.getBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
                vm.insert(check)
            }
        }
    }

    fun onComicLoadFail() {
        hideProgressBar()
        HintUtils.showToast(this, R.string.common_data_load_fail)
    }

    fun onComicLoadSuccess(list: List<*>) {
        hideProgressBar()
        mGridAdapter.setData(list)
    }

    fun onComicTitleLoadSuccess(list: List<String>) {
        hideProgressDialog()
        val fragment = MultiDialogFragment.newInstance(
            R.string.part_favorite_select,
            list.toTypedArray(),
            null,
            DIALOG_REQUEST_ADD
        )
        fragment.show(supportFragmentManager, null)
    }

    fun onComicTitleLoadFail() {
        hideProgressDialog()
        HintUtils.showToast(this, R.string.common_data_load_fail)
    }

    fun onComicInsertSuccess(list: List<Any>) {
        hideProgressDialog()
        mGridAdapter.addAll(list)
        HintUtils.showToast(this, R.string.common_execute_success)
    }

    fun onComicInsertFail() {
        hideProgressDialog()
        HintUtils.showToast(this, R.string.common_execute_fail)
    }

    fun onHighlightCancel(comic: MiniComic) {
        mGridAdapter.moveItemTop(comic)
    }

    fun onComicRead(comic: MiniComic) {
        mGridAdapter.moveItemTop(comic)
    }

    fun onComicRemove(id: Long) {
        mGridAdapter.removeItemById(id)
    }

    fun onComicAdd(comic: MiniComic) {
        if (!mGridAdapter.contains(comic)) {
            mGridAdapter.add(0, comic)
        }
    }

    override fun getDefaultTitle(): String? {
        return intent.getStringExtra(Extra.EXTRA_KEYWORD)
    }

    override fun inflateContentView(): View {
        binding = ActivityPartFavoriteBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_part_favorite
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    override fun bindViews() {
        super.bindViews()
        mRecyclerView = binding.partFavoriteRecyclerView
    }

    companion object {
        private const val DIALOG_REQUEST_DELETE = 0
        private const val DIALOG_REQUEST_ADD = 1

        @JvmStatic
        fun createIntent(context: Context, id: Long, title: String?): Intent {
            val intent = Intent(context, PartFavoriteActivity::class.java)
            intent.putExtra(Extra.EXTRA_ID, id)
            intent.putExtra(Extra.EXTRA_KEYWORD, title)
            return intent
        }
    }
}
