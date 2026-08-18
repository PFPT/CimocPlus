package com.haleydu.cimoc.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.misc.Switcher;
import com.haleydu.cimoc.model.Tag;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.TagEditorAdapter;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Hiroshi on 2016/12/2.
 */

@AndroidEntryPoint
public class TagEditorActivity extends CoordinatorActivity {

    private TagEditorViewModel vm;
    private TagEditorAdapter mTagAdapter;

    public static Intent createIntent(Context context, long id) {
        Intent intent = new Intent(context, TagEditorActivity.class);
        intent.putExtra(Extra.EXTRA_ID, id);
        return intent;
    }

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(TagEditorViewModel.class);
    }

    @Override
    protected BaseAdapter initAdapter() {
        mTagAdapter = new TagEditorAdapter(this, new ArrayList<Switcher<Tag>>());
        return mTagAdapter;
    }

    @Override
    protected void initActionButton() {
        mActionButton.setImageResource(R.drawable.ic_done_white_24dp);
        mActionButton.show();
        hideProgressBar();
    }

    @Override
    protected void initData() {
        long id = getIntent().getLongExtra(Extra.EXTRA_ID, -1);
        FlowExtKt.collectOnStart(vm.getTags(), this, this::onTagLoadSuccess);
        FlowExtKt.collectOnStart(vm.getLoadFail(), this, unit -> onTagLoadFail());
        FlowExtKt.collectOnStart(vm.getUpdateSuccess(), this, unit -> onTagUpdateSuccess());
        FlowExtKt.collectOnStart(vm.getUpdateFail(), this, unit -> onTagUpdateFail());
        vm.load(id);
    }

    public void onTagLoadSuccess(List<Switcher<Tag>> list) {
        hideProgressBar();
        mTagAdapter.addAll(list);
    }

    public void onTagLoadFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_data_load_fail);
    }

    public void onTagUpdateSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_success);
    }

    public void onTagUpdateFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    @Override
    public void onItemClick(View view, int position) {
        Switcher<Tag> switcher = mTagAdapter.getItem(position);
        switcher.switchEnable();
        mTagAdapter.notifyItemChanged(position);
    }

    void onActionButtonClick() {
        showProgressDialog();
        List<Long> list = new ArrayList<>();
        for (Switcher<Tag> switcher : mTagAdapter.getDateSet()) {
            if (switcher.isEnable()) {
                list.add(switcher.getElement().getId());
            }
        }
        vm.updateRef(list);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.tag_editor);
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mActionButton.setOnClickListener(v -> onActionButtonClick());
    }

}
