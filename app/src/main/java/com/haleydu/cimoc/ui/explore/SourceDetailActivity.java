package com.haleydu.cimoc.ui.explore;
import com.haleydu.cimoc.ui.common.BackActivity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivitySourceDetailBinding;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.ui.widget.Option;
import dagger.hilt.android.AndroidEntryPoint;


/**
 * Created by Hiroshi on 2017/1/18.
 */

@AndroidEntryPoint
public class SourceDetailActivity extends BackActivity {

    Option mSourceType;
    Option mSourceTitle;
    Option mSourceFavorite;
    private SourceDetailViewModel vm;
    private ActivitySourceDetailBinding binding;

    public static Intent createIntent(Context context, int type) {
        Intent intent = new Intent(context, SourceDetailActivity.class);
        intent.putExtra(Extra.EXTRA_SOURCE, type);
        return intent;
    }

    @Override
    protected void initData() {
        vm = new ViewModelProvider(this).get(SourceDetailViewModel.class);
        SourceDetailViewModel.Detail detail = vm.load(getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1));
        onSourceLoadSuccess(detail.getType(), detail.getTitle(), detail.getCount());
    }

    void onSourceFavoriteClick() {
        // TODO 显示这个图源的漫画
    }

    public void onSourceLoadSuccess(int type, String title, long count) {
        mSourceType.setSummary(String.valueOf(type));
        mSourceTitle.setSummary(title);
        mSourceFavorite.setSummary(String.valueOf(count));
    }

    @Override
    protected View inflateContentView() {
        binding = ActivitySourceDetailBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_source_detail;
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.source_detail);
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mSourceType = binding.sourceDetailType;
        mSourceTitle = binding.sourceDetailTitle;
        mSourceFavorite = binding.sourceDetailFavorite;
        binding.sourceDetailFavorite.setOnClickListener(v -> onSourceFavoriteClick());
    }

}
