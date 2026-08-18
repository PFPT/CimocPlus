package com.haleydu.cimoc.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.google.common.collect.Lists;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.ui.reader.ReaderActivity;
import com.haleydu.cimoc.fresco.ControllerBuilderSupplierFactory;
import com.haleydu.cimoc.fresco.ImagePipelineFactoryBuilder;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.Task;
import com.haleydu.cimoc.event.AppEventBus;
import com.haleydu.cimoc.event.AppEvent;
import com.haleydu.cimoc.service.DownloadService;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.DetailAdapter;
import com.haleydu.cimoc.utils.StringUtils;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import okhttp3.OkHttpClient;


import static com.haleydu.cimoc.utils.interpretationUtils.isReverseOrder;

/**
 * Created by Hiroshi on 2016/7/2.
 */
@AndroidEntryPoint
public class DetailActivity extends CoordinatorActivity {

    public static final int REQUEST_CODE_DOWNLOAD = 0;

    private DetailAdapter mDetailAdapter;
    private DetailViewModel vm;
    private ImagePipelineFactory mImagePipelineFactory;

    @Inject
    OkHttpClient httpClient;

    private boolean mAutoBackup;
    private int mBackupCount;

    public static Intent createIntent(Context context, Long id, int source, String cid) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(Extra.EXTRA_ID, id);
        intent.putExtra(Extra.EXTRA_SOURCE, source);
        intent.putExtra(Extra.EXTRA_CID, cid);
        return intent;
    }

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(DetailViewModel.class);
    }

    @Override
    protected BaseAdapter initAdapter() {
        mDetailAdapter = new DetailAdapter(this, new ArrayList<Chapter>());
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return mDetailAdapter;
    }

    @Override
    protected RecyclerView.LayoutManager initLayoutManager() {
        return new GridLayoutManager(this, 3);
    }

    @Override
    protected void initData() {
        mAutoBackup = mPreference.getBoolean(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true);
        mBackupCount = mPreference.getInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, 0);
        long id = getIntent().getLongExtra(Extra.EXTRA_ID, -1);
        int source = getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1);
        String cid = getIntent().getStringExtra(Extra.EXTRA_CID);
        FlowExtKt.collectOnStart(vm.getEvents(), this, event -> {
            if (event instanceof DetailViewModel.Event.PreLoad) {
                DetailViewModel.Event.PreLoad preLoad = (DetailViewModel.Event.PreLoad) event;
                onPreLoadSuccess(preLoad.getList(), preLoad.getComic());
            } else if (event instanceof DetailViewModel.Event.ComicLoaded) {
                onComicLoadSuccess(((DetailViewModel.Event.ComicLoaded) event).getComic());
            } else if (event instanceof DetailViewModel.Event.ChapterLoaded) {
                onChapterLoadSuccess(((DetailViewModel.Event.ChapterLoaded) event).getList());
            } else if (event instanceof DetailViewModel.Event.ParseError) {
                onParseError();
            } else if (event instanceof DetailViewModel.Event.NetworkError) {
                onNetworkError();
            } else if (event instanceof DetailViewModel.Event.TaskAddSuccess) {
                onTaskAddSuccess(((DetailViewModel.Event.TaskAddSuccess) event).getList());
            } else if (event instanceof DetailViewModel.Event.TaskAddFail) {
                onTaskAddFail();
            } else if (event instanceof DetailViewModel.Event.LastChange) {
                onLastChange(((DetailViewModel.Event.LastChange) event).getLast());
            }
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE), this, event -> vm.refreshFromUpdate());
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE_INFO), this, event ->
                vm.applyUpdateInfo((Comic) event.getData()));
        vm.load(id, source, cid);


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAutoBackup) {
            mPreference.putInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, mBackupCount);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImagePipelineFactory != null) {
            mImagePipelineFactory.getImagePipeline().clearMemoryCaches();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        if (!isProgressBarShown()) {
            switch (item.getItemId()) {
//                case R.id.detail_history:
//                    if (!mDetailAdapter.getDateSet().isEmpty()) {
//                        String path = vm.getComic().getLast();
//                        if (path == null) {
//                            path = mDetailAdapter.getItem(mDetailAdapter.getDateSet().size() - 1).getPath();
//                        }
//                        startReader(path);
//                    }
//                    break;
                case R.id.detail_download:
                    if (!mDetailAdapter.getDateSet().isEmpty()) {
                        intent = ChapterActivity.createIntent(this, new ArrayList<>(mDetailAdapter.getDateSet()));
                        startActivityForResult(intent, REQUEST_CODE_DOWNLOAD);
                    }
                    break;
                case R.id.detail_tag:
                    if (vm.getComic().getFavorite() != null) {
                        intent = TagEditorActivity.createIntent(this, vm.getComic().getId());
                        startActivity(intent);
                    } else {
                        showSnackbar(R.string.detail_tag_favorite);
                    }
                    break;
                case R.id.detail_search_title:
                    if (!StringUtils.isEmpty(vm.getComic().getTitle())) {
                        if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.CONTENT, vm.getComic().getTitle());
                            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "byTitle");
                            bundle.putInt(FirebaseAnalytics.Param.SOURCE, vm.getComic().getSource());
                            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
                        }
                        intent = ResultActivity.createIntent(this, vm.getComic().getTitle(), null, ResultActivity.LAUNCH_MODE_SEARCH);
                        startActivity(intent);
                    } else {
                        showSnackbar(R.string.common_keyword_empty);
                    }
                    break;
                case R.id.detail_search_author:
                    if (!StringUtils.isEmpty(vm.getComic().getAuthor())) {
                        intent = ResultActivity.createIntent(this, vm.getComic().getAuthor(), null, ResultActivity.LAUNCH_MODE_SEARCH);
                        startActivity(intent);
                    } else {
                        showSnackbar(R.string.common_keyword_empty);
                    }
                    break;
                case R.id.detail_share_url:
                    String url = vm.getComic().getUrl();
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(intent, url));

                    // firebase analytics
                    if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.CONTENT, url);
                        bundle.putInt(FirebaseAnalytics.Param.SOURCE, vm.getComic().getSource());
                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
                    }
                    break;
                case R.id.detail_reverse_list:
                    mDetailAdapter.reverse();
                    break;
//                case R.id.detail_disqus:
//                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_cimqus_url) + "/cimoc/" + vm.getComic().getTitle()));
//                    try {
//                        startActivity(intent);
//                    } catch (Exception e) {
//                        showSnackbar(R.string.about_resource_fail);
//                    }
//                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_DOWNLOAD:
                    showProgressDialog();
                    List<Chapter> list = data.getParcelableArrayListExtra(Extra.EXTRA_CHAPTER);
                    vm.addTask(mDetailAdapter.getDateSet(), list);
                    break;
            }
        }
    }

    void onActionButtonClick() {
        //todo: add comic to mangodb
        if (vm.getComic().getFavorite() != null) {
            vm.unfavoriteComic();
            increment();
            mActionButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
            showSnackbar(R.string.detail_unfavorite);
        } else {
            vm.favoriteComic();
            increment();
            mActionButton.setImageResource(R.drawable.ic_favorite_white_24dp);
            showSnackbar(R.string.detail_favorite);
        }
    }

    void onActionButton2Click() {
        if (!mDetailAdapter.getDateSet().isEmpty()) {
            String path = vm.getComic().getLast();
            if (path == null) {
                path = mDetailAdapter.getItem(mDetailAdapter.getDateSet().size() - 1).getPath();
            }
            startReader(path);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (position != 0) {
            String path = mDetailAdapter.getItem(position - 1).getPath();
            startReader(path);
        }
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        if (position == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(mDetailAdapter.title)
                    .setMessage(mDetailAdapter.intro)
                    .setPositiveButton(R.string.dialog_close, null)
                    .show();
        }
        return false;
    }


    private void startReader(String path) {
        long id = vm.updateLast(path);
        mDetailAdapter.setLast(path);
        int mode = mPreference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE);
        Intent intent = ReaderActivity.createIntent(DetailActivity.this, id, mDetailAdapter.getDateSet(), mode);
        startActivity(intent);
    }

    public void onLastChange(String last) {
        mDetailAdapter.setLast(last);
    }


    public void onTaskAddSuccess(ArrayList<Task> list) {
        Intent intent = DownloadService.createIntent(this, list);
        DownloadService.start(this, intent);
        updateChapterList(list);
        showSnackbar(R.string.detail_download_queue_success);
        hideProgressDialog();
    }

    private void updateChapterList(List<Task> list) {
        Set<String> set = new HashSet<>();
        for (Task task : list) {
            set.add(task.getPath());
        }
        for (Chapter chapter : mDetailAdapter.getDateSet()) {
            if (set.contains(chapter.getPath())) {
                chapter.setDownload(true);
            }
        }
    }

    public void onTaskAddFail() {
        hideProgressDialog();
        showSnackbar(R.string.detail_download_queue_fail);
    }

    public void onComicLoadSuccess(Comic comic) {
        mDetailAdapter.setInfo(comic.getCover(), comic.getTitle(), comic.getAuthor(),
                comic.getIntro(), comic.getFinish(), comic.getUpdate(), comic.getLast(), isReverseOrder(comic));

        if (comic.getTitle() != null && comic.getCover() != null) {
            mImagePipelineFactory = ImagePipelineFactoryBuilder.build(this, vm.parserHeader(), false, httpClient);
            mDetailAdapter.setControllerSupplier(ControllerBuilderSupplierFactory.get(this, mImagePipelineFactory));

            int resId = comic.getFavorite() != null ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_border_white_24dp;
            mActionButton.setImageResource(resId);
            mActionButton.setVisibility(View.VISIBLE);
            mActionButton2.setVisibility(View.VISIBLE);
        }
    }

    public void onChapterLoadSuccess(List<Chapter> list) {
        hideProgressBar();
        if (vm.getComic().getTitle() != null && vm.getComic().getCover() != null) {
            mDetailAdapter.clear();
            mDetailAdapter.addAll(list);
            mDetailAdapter.notifyDataSetChanged();
        }
        if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CONTENT, vm.getComic().getTitle());
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Title");
            bundle.putInt(FirebaseAnalytics.Param.SOURCE, vm.getComic().getSource());
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, true);
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
        }
    }

    public void onPreLoadSuccess(List<Chapter> list, Comic comic) {
        hideProgressBar();
        if (isReverseOrder(comic)){
            mDetailAdapter.addAll(Lists.reverse(list));
        }else {
            mDetailAdapter.addAll(list);
        }
        mDetailAdapter.setInfo(comic.getCover(), comic.getTitle(), comic.getAuthor(),
                comic.getIntro(), comic.getFinish(), comic.getUpdate(), comic.getLast(), isReverseOrder(comic));

        if (comic.getTitle() != null && comic.getCover() != null) {
            mImagePipelineFactory = ImagePipelineFactoryBuilder.build(this, vm.parserHeader(), false, httpClient);
            mDetailAdapter.setControllerSupplier(ControllerBuilderSupplierFactory.get(this, mImagePipelineFactory));

            int resId = comic.getFavorite() != null ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_border_white_24dp;
            mActionButton.setImageResource(resId);
            mActionButton.setVisibility(View.VISIBLE);
            mActionButton2.setVisibility(View.VISIBLE);
        }

    }

    public void onParseError() {
        if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CONTENT, vm.getComic().getTitle());
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Title");
            bundle.putInt(FirebaseAnalytics.Param.SOURCE, vm.getComic().getSource());
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false);
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
        }
        hideProgressBar();
        showSnackbar(R.string.common_parse_error);
    }

    public void onNetworkError() {
        hideProgressBar();
        showSnackbar(R.string.common_network_error);
    }

    private void increment() {
        if (mAutoBackup && ++mBackupCount == 10) {
            mBackupCount = 0;
            mPreference.putInt(PreferenceManager.PREF_BACKUP_SAVE_COMIC_COUNT, 0);
            vm.backup();
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.detail);
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mActionButton.setOnClickListener(v -> onActionButtonClick());
        mActionButton2.setOnClickListener(v -> onActionButton2Click());
    }

}
