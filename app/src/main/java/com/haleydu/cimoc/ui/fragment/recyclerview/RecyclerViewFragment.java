package com.haleydu.cimoc.ui.fragment.recyclerview;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.FragmentRecyclerViewBinding;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.fragment.BaseFragment;


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
        BaseAdapter adapter = initAdapter();
        if (adapter != null) {
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            mRecyclerView.addItemDecoration(adapter.getItemDecoration());
            mRecyclerView.setAdapter(adapter);
        }
    }

    abstract protected BaseAdapter initAdapter();

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
