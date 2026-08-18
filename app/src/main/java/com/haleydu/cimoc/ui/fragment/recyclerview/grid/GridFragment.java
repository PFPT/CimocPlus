package com.haleydu.cimoc.ui.fragment.recyclerview.grid;

import android.content.Intent;
import androidx.annotation.ColorRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.component.ThemeResponsive;
import com.haleydu.cimoc.databinding.FragmentGridBinding;
import com.haleydu.cimoc.manager.SourceManager;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.MiniComic;
import com.haleydu.cimoc.ui.activity.DetailActivity;
import com.haleydu.cimoc.ui.activity.TaskActivity;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.GridAdapter;
import com.haleydu.cimoc.ui.fragment.dialog.ItemDialogFragment;
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment;
import com.haleydu.cimoc.ui.fragment.recyclerview.RecyclerViewFragment;
import com.haleydu.cimoc.utils.HintUtils;
import com.haleydu.cimoc.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;


/**
 * Created by Hiroshi on 2016/9/22.
 */

public abstract class GridFragment extends RecyclerViewFragment implements DialogCaller, ThemeResponsive {

    protected static final int DIALOG_REQUEST_OPERATION = 0;
    protected GridAdapter mGridAdapter;
    protected long mSavedId = -1;
    FloatingActionButton mActionButton;
    @Inject
    SourceManager sourceManager;

    @Override
    protected BaseAdapter initAdapter() {
        mGridAdapter = new GridAdapter(getActivity(), new LinkedList<>());
        mGridAdapter.setProvider(getAppInstance().getBuilderProvider());
        mGridAdapter.setTitleGetter(sourceManager.new TitleGetter());
        mRecyclerView.setRecycledViewPool(getAppInstance().getGridRecycledPool());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        getAppInstance().getBuilderProvider().pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        getAppInstance().getBuilderProvider().resume();
                        break;
                }
            }
        });
        mActionButton.setImageResource(getActionButtonRes());
        return mGridAdapter;
    }

    @Override
    protected RecyclerView.LayoutManager initLayoutManager() {
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 3);
        manager.setRecycleChildrenOnDetach(true);
        return manager;
    }

    void onActionButtonClick() {
        performActionButtonClick();
    }

    @Override
    public void onItemClick(View view, int position) {
        MiniComic comic = (MiniComic) mGridAdapter.getItem(position);
        Intent intent = comic.isLocal() ? TaskActivity.createIntent(getActivity(), comic.getId()) :
                DetailActivity.createIntent(getActivity(), comic.getId(), -1, null);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        mSavedId = ((MiniComic)mGridAdapter.getItem(position)).getId();
        ItemDialogFragment fragment = ItemDialogFragment.newInstance(R.string.common_operation_select,
                getOperationItems(), DIALOG_REQUEST_OPERATION);
        fragment.setTargetFragment(this, 0);
        fragment.show(requireActivity().getSupportFragmentManager(), null);
        return true;
    }

    public void onComicLoadSuccess(List<Object> list) {
        mGridAdapter.addAll(list);
    }

    public void onComicLoadFail() {
        HintUtils.showToast(getActivity(), R.string.common_data_load_fail);
    }

    public void onExecuteFail() {
        hideProgressDialog();
        HintUtils.showToast(getActivity(), R.string.common_execute_fail);
    }

    @Override
    public void onThemeChange(@ColorRes int primary, @ColorRes int accent) {
        mActionButton.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(), accent));
    }

    protected void showComicInfo(Comic comic, int request) {
        if (comic == null) {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.common_execute_fail,
                    R.string.comic_info_not_found, true, request);
            fragment.setTargetFragment(this, 0);
            fragment.show(requireActivity().getSupportFragmentManager(), null);
            return;
        }
        String content =
                StringUtils.format("%s  %s\n%s  %s\n%s  %s\n%s  %s\n%s  %s",
                        getString(R.string.comic_info_title),
                        comic.getTitle(),
                        getString(R.string.comic_info_source),
                        sourceManager.getParser(comic.getSource()).getTitle(),
                        getString(R.string.comic_info_status),
                        comic.getFinish() == null ? getString(R.string.comic_status_finish) :
                                getString(R.string.comic_status_continue),
                        getString(R.string.comic_info_chapter),
                        comic.getChapter() == null ? getString(R.string.common_null) : comic.getChapter(),
                        getString(R.string.comic_info_time),
                        comic.getHistory() == null ? getString(R.string.common_null) :
                                StringUtils.getFormatTime("yyyy-MM-dd HH:mm:ss", comic.getHistory()));
        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.comic_info,
                content, true, request);
        fragment.setTargetFragment(this, 0);
        fragment.show(requireActivity().getSupportFragmentManager(), null);
    }

    protected abstract void performActionButtonClick();

    protected abstract int getActionButtonRes();

    protected abstract String[] getOperationItems();

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void bindViews(View view) {
        super.bindViews(view);
        FragmentGridBinding binding = FragmentGridBinding.bind(view);
        mRecyclerView = binding.recyclerViewContent;
        mActionButton = binding.gridActionButton;
        binding.gridActionButton.setOnClickListener(v -> onActionButtonClick());
    }

}
