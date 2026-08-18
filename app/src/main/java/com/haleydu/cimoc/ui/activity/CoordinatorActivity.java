package com.haleydu.cimoc.ui.activity;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivityCoordinatorBinding;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;


/**
 * Created by Hiroshi on 2016/12/1.
 */

public abstract class CoordinatorActivity extends BackActivity implements
        BaseAdapter.OnItemClickListener, BaseAdapter.OnItemLongClickListener {

    protected FloatingActionButton mActionButton;
    protected FloatingActionButton mActionButton2;
    protected RecyclerView mRecyclerView;
    protected CoordinatorLayout mLayoutView;
    private ActivityCoordinatorBinding binding;

    @Override
    protected void initView() {
        super.initView();
        mRecyclerView.setLayoutManager(initLayoutManager());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        BaseAdapter adapter = initAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        RecyclerView.ItemDecoration decoration = adapter.getItemDecoration();
        if (decoration != null) {
            mRecyclerView.addItemDecoration(adapter.getItemDecoration());
        }
        mRecyclerView.setAdapter(adapter);
        initActionButton();
    }

    protected abstract BaseAdapter initAdapter();

    protected void initActionButton() {
    }

    protected RecyclerView.LayoutManager initLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    public void onItemClick(View view, int position) {
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        return false;
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityCoordinatorBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_coordinator;
    }

    @Override
    protected View getLayoutView() {
        return mLayoutView;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        mActionButton = binding.coordinatorActionButton;
        mActionButton2 = binding.coordinatorActionButton2;
        mRecyclerView = binding.coordinatorRecyclerView;
        mLayoutView = binding.coordinatorLayout;
    }

}
