package com.haleydu.cimoc.ui.activity;

import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivityResultBinding;
import com.haleydu.cimoc.fresco.ControllerBuilderProvider;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.ResultAdapter;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.OkHttpClient;


/**
 * Created by Hiroshi on 2016/7/3.
 */
@AndroidEntryPoint
public class ResultActivity extends BackActivity implements BaseAdapter.OnItemClickListener {

    /**
     * 根据用户输入的关键词搜索
     * Extra: 关键词 图源列表
     */
    public static final int LAUNCH_MODE_SEARCH = 0;
    /**
     * 根据分类搜索，关键词字段存放 url 格式
     * Extra: 格式 图源
     */
    public static final int LAUNCH_MODE_CATEGORY = 1;
    RecyclerView mRecyclerView;
    FrameLayout mLayoutView;
    private ResultAdapter mResultAdapter;
    private LinearLayoutManager mLayoutManager;
    private ResultViewModel vm;
    private ActivityResultBinding binding;
    private ControllerBuilderProvider mProvider;
    private int type;

    @Inject
    OkHttpClient httpClient;

    public static Intent createIntent(Context context, String keyword, int source, int type) {
        return createIntent(context, keyword, new int[]{source}, type);
    }

    public static Intent createIntent(Context context, String keyword, int[] array, int type) {
        Intent intent = new Intent(context, ResultActivity.class);
        intent.putExtra(Extra.EXTRA_MODE, type);
        intent.putExtra(Extra.EXTRA_SOURCE, array);
        intent.putExtra(Extra.EXTRA_KEYWORD, keyword);
//        intent.putExtra(Extra.EXTRA_STRICT, true);
        return intent;
    }

    public static Intent createIntent(Context context, String keyword, boolean strictSearch, int[] array, int type) {
        Intent intent = createIntent(context, keyword, array, type);
//        intent.putExtra(Extra.EXTRA_MODE, type);
//        intent.putExtra(Extra.EXTRA_SOURCE, array);
//        intent.putExtra(Extra.EXTRA_KEYWORD, keyword);
        intent.putExtra(Extra.EXTRA_STRICT, strictSearch);
        return intent;
    }

    // 建个Map把漫源搜索的上一个请求的url存下来，最后利用Activity生命周期清掉
    //
    // 解决重复加载列表问题思路：
    // 在新的一次请求（上拉加载）前检查新Url与上一次请求的是否一致。
    // 一致则返回空请求，达到阻断请求的目的；不一致则更新Map中存的Url，Map中不存在则新建
    public static SparseArray<String> searchUrls = new SparseArray<>();

    @Override
    protected void initViewModel() {
        String keyword = getIntent().getStringExtra(Extra.EXTRA_KEYWORD);
        int[] source = getIntent().getIntArrayExtra(Extra.EXTRA_SOURCE);
        boolean strictSearch = getIntent().getBooleanExtra(Extra.EXTRA_STRICT, true);
        vm = new ViewModelProvider(this).get(ResultViewModel.class);
        vm.setup(source, keyword, strictSearch);
    }

    @Override
    protected void initView() {
        super.initView();
        mLayoutManager = new LinearLayoutManager(this);
        mResultAdapter = new ResultAdapter(this, new LinkedList<Comic>());
        mResultAdapter.setOnItemClickListener(this);
        mProvider = new ControllerBuilderProvider(this, vm.headerGetter(), true, httpClient);
        mResultAdapter.setProvider(mProvider);
        mResultAdapter.setTitleGetter(vm.titleGetter());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mResultAdapter.getItemDecoration());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mLayoutManager.findLastVisibleItemPosition() >= mResultAdapter.getItemCount() - 4 && dy > 0) {
                    load();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        mProvider.pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        mProvider.resume();
                        break;
                }
            }
        });
        mRecyclerView.setAdapter(mResultAdapter);
    }

    @Override
    protected void initData() {
        type = getIntent().getIntExtra(Extra.EXTRA_MODE, -1);
        FlowExtKt.collectOnStart(vm.getSearchSuccess(), this, this::onSearchSuccess);
        FlowExtKt.collectOnStart(vm.getSearchError(), this, unit -> onSearchError());
        FlowExtKt.collectOnStart(vm.getLoadSuccess(), this, this::onLoadSuccess);
        FlowExtKt.collectOnStart(vm.getLoadFail(), this, unit -> onLoadFail());
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProvider != null) {
            mProvider.clear();
        }
    }

    private void load() {
        switch (type) {
            case LAUNCH_MODE_SEARCH:
                vm.loadSearch();
                break;
            case LAUNCH_MODE_CATEGORY:
                vm.loadCategory();
                break;
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        Comic comic = mResultAdapter.getItem(position);
        Intent intent = DetailActivity.createIntent(this, null, comic.getSource(), comic.getCid());
        startActivity(intent);
    }

    public void onSearchSuccess(Comic comic) {
        hideProgressBar();
        mResultAdapter.add(comic);
        if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CHARACTER, getIntent().getStringExtra(Extra.EXTRA_KEYWORD));
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "bySearch");
            bundle.putString(FirebaseAnalytics.Param.CONTENT, comic.getTitle());
            bundle.putInt(FirebaseAnalytics.Param.SOURCE, comic.getSource());
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, true);
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
        }
    }

    public void onLoadSuccess(List<Comic> list) {
        hideProgressBar();
        mResultAdapter.addAll(list);
    }

    public void onLoadFail() {
        hideProgressBar();
        showSnackbar(R.string.common_parse_error);
    }

    public void onSearchError() {
        hideProgressBar();
        showSnackbar(R.string.result_empty);
        if(mPreference.getBoolean(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true)) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CHARACTER, getIntent().getStringExtra(Extra.EXTRA_KEYWORD));
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "bySearch");
            bundle.putString(FirebaseAnalytics.Param.CONTENT, getString(R.string.result_empty));
            bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false);
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.result);
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_result;
    }

    @Override
    protected View getLayoutView() {
        return mLayoutView;
    }

    @Override
    protected boolean isNavTranslation() {
        return true;
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mRecyclerView = binding.resultRecyclerView;
        mLayoutView = binding.resultLayout;
    }

}
