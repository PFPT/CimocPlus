package com.haleydu.cimoc.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.databinding.ActivityPartFavoriteBinding;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.TagManager;
import com.haleydu.cimoc.model.MiniComic;
import com.haleydu.cimoc.event.AppEventBus;
import com.haleydu.cimoc.event.AppEvent;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.GridAdapter;
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment;
import com.haleydu.cimoc.ui.fragment.dialog.MultiDialogFragment;
import com.haleydu.cimoc.utils.HintUtils;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Hiroshi on 2016/10/11.
 */

@AndroidEntryPoint
public class PartFavoriteActivity extends BackActivity implements DialogCaller, BaseAdapter.OnItemClickListener,
        BaseAdapter.OnItemLongClickListener {

    private static final int DIALOG_REQUEST_DELETE = 0;
    private static final int DIALOG_REQUEST_ADD = 1;

    RecyclerView mRecyclerView;

    private PartFavoriteViewModel vm;
    private ActivityPartFavoriteBinding binding;
    private GridAdapter mGridAdapter;

    private MiniComic mSavedComic;
    private boolean isDeletable;

    public static Intent createIntent(Context context, long id, String title) {
        Intent intent = new Intent(context, PartFavoriteActivity.class);
        intent.putExtra(Extra.EXTRA_ID, id);
        intent.putExtra(Extra.EXTRA_KEYWORD, title);
        return intent;
    }

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(PartFavoriteViewModel.class);
    }

    @Override
    protected void initView() {
        super.initView();
        mGridAdapter = new GridAdapter(this, new LinkedList<Object>());
        mGridAdapter.setSymbol(true);
        mGridAdapter.setProvider(((App) getApplication()).getBuilderProvider());
        mGridAdapter.setTitleGetter(vm.titleGetter());
        mGridAdapter.setOnItemClickListener(this);
        mGridAdapter.setOnItemLongClickListener(this);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.addItemDecoration(mGridAdapter.getItemDecoration());
        mRecyclerView.setAdapter(mGridAdapter);
    }

    @Override
    protected void initData() {
        long id = getIntent().getLongExtra(Extra.EXTRA_ID, -1);
        isDeletable = id != TagManager.TAG_CONTINUE && id != TagManager.TAG_FINISH;
        FlowExtKt.collectOnStart(vm.getComics(), this, this::onComicLoadSuccess);
        FlowExtKt.collectOnStart(vm.getLoadFail(), this, unit -> onComicLoadFail());
        FlowExtKt.collectOnStart(vm.getTitles(), this, this::onComicTitleLoadSuccess);
        FlowExtKt.collectOnStart(vm.getTitleFail(), this, unit -> onComicTitleLoadFail());
        FlowExtKt.collectOnStart(vm.getInsertSuccess(), this, this::onComicInsertSuccess);
        FlowExtKt.collectOnStart(vm.getInsertFail(), this, unit -> onComicInsertFail());
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_UNFAVORITE), this, event ->
                onComicRemove((long) event.getData()));
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_TAG_UPDATE), this, event -> {
            long comicId = (long) event.getData();
            @SuppressWarnings("unchecked")
            List<Long> list = (List<Long>) event.getData(1);
            if (list.contains(id)) {
                onComicAdd(new MiniComic(vm.loadComic(comicId)));
            } else {
                onComicRemove(comicId);
            }
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_CANCEL_HIGHLIGHT), this, event ->
                onHighlightCancel((MiniComic) event.getData()));
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_READ), this, event ->
                onComicRead((MiniComic) event.getData()));
        vm.load(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getIntent().getLongExtra(Extra.EXTRA_ID, -1) >= 0) {
            getMenuInflater().inflate(R.menu.menu_part_favorite, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.part_favorite_add:
                showProgressDialog();
                vm.loadComicTitle(mGridAdapter.getDateSet());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View view, int position) {
        MiniComic comic = (MiniComic) mGridAdapter.getItem(position);
        Intent intent = DetailActivity.createIntent(this, comic.getId(), -1, null);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        if (isDeletable) {
            mSavedComic = (MiniComic) mGridAdapter.getItem(position);
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.dialog_confirm,
                    R.string.part_favorite_delete_confirm, true, DIALOG_REQUEST_DELETE);
            fragment.show(getSupportFragmentManager(), null);
            return true;
        }
        return false;
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_DELETE:
                long id = mSavedComic.getId();
                vm.delete(id);
                mGridAdapter.remove(mSavedComic);
                HintUtils.showToast(this, R.string.common_execute_success);
                break;
            case DIALOG_REQUEST_ADD:
                showProgressDialog();
                boolean[] check = bundle.getBooleanArray(EXTRA_DIALOG_RESULT_VALUE);
                vm.insert(check);
                break;
        }
    }

    public void onComicLoadFail() {
        hideProgressBar();
        HintUtils.showToast(this, R.string.common_data_load_fail);
    }

    public void onComicLoadSuccess(List<Object> list) {
        hideProgressBar();
        mGridAdapter.addAll(list);

    }

    public void onComicTitleLoadSuccess(List<String> list) {
        hideProgressDialog();
        MultiDialogFragment fragment = MultiDialogFragment.newInstance(R.string.part_favorite_select,
                list.toArray(new String[list.size()]), null, DIALOG_REQUEST_ADD);
        fragment.show(getSupportFragmentManager(), null);
    }

    public void onComicTitleLoadFail() {
        hideProgressDialog();
        HintUtils.showToast(this, R.string.common_data_load_fail);
    }

    public void onComicInsertSuccess(List<Object> list) {
        hideProgressDialog();
        mGridAdapter.addAll(list);
        HintUtils.showToast(this, R.string.common_execute_success);
    }

    public void onComicInsertFail() {
        hideProgressDialog();
        HintUtils.showToast(this, R.string.common_execute_fail);
    }

    public void onHighlightCancel(MiniComic comic) {
        mGridAdapter.moveItemTop(comic);
    }

    public void onComicRead(MiniComic comic) {
        mGridAdapter.moveItemTop(comic);
    }

    public void onComicRemove(long id) {
        mGridAdapter.removeItemById(id);
    }

    public void onComicAdd(MiniComic comic) {
        if (!mGridAdapter.contains(comic)) {
            mGridAdapter.add(0, comic);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getIntent().getStringExtra(Extra.EXTRA_KEYWORD);
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityPartFavoriteBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_part_favorite;
    }

    @Override
    protected boolean isNavTranslation() {
        return true;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        mRecyclerView = binding.partFavoriteRecyclerView;
    }

}
