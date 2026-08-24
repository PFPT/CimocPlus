package com.haleydu.cimoc.ui.common;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.FragmentRecyclerViewBinding;
import com.haleydu.cimoc.ui.common.BaseAdapter;
import com.haleydu.cimoc.ui.common.GridAdapter;
import com.haleydu.cimoc.ui.common.BaseFragment;


/**
 * Created by Hiroshi on 2016/10/11.
 */

public abstract class RecyclerViewFragment extends BaseFragment implements BaseAdapter.OnItemClickListener,
        BaseAdapter.OnItemLongClickListener {

    protected RecyclerView mRecyclerView;

    @Override
    protected void initView() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setLayoutManager(initLayoutManager());
        RecyclerView.Adapter adapter = initAdapter();
        if (adapter instanceof BaseAdapter) {
            BaseAdapter base = (BaseAdapter) adapter;
            base.setOnItemClickListener(this);
            base.setOnItemLongClickListener(this);
            mRecyclerView.addItemDecoration(base.getItemDecoration());
        } else if (adapter instanceof GridAdapter) {
            GridAdapter grid = (GridAdapter) adapter;
            grid.setOnItemClickListener(this);
            grid.setOnItemLongClickListener(this);
            mRecyclerView.addItemDecoration(grid.getItemDecoration());
        }
        if (adapter != null) {
            mRecyclerView.setAdapter(adapter);
        }
    }

    abstract protected RecyclerView.Adapter initAdapter();

    protected abstract RecyclerView.LayoutManager initLayoutManager();

    @Override
    public void onItemClick(View view, int position) {
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        return false;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_recycler_view;
    }


    @Override
    protected void bindViews(View view) {
        super.bindViews(view);
        if (getLayoutRes() == R.layout.fragment_recycler_view) {
            FragmentRecyclerViewBinding binding = FragmentRecyclerViewBinding.bind(view);
            mRecyclerView = binding.recyclerViewContent;
        } else {
            mRecyclerView = view.findViewById(R.id.recycler_view_content);
        }
    }

}
