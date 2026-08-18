package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.haleydu.cimoc.databinding.ItemDirBinding;

import java.util.List;


/**
 * Created by Hiroshi on 2016/12/6.
 */

public class DirAdapter extends BaseAdapter<String> {

    public DirAdapter(Context context, List<String> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DirHolder(ItemDirBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        DirHolder viewHolder = (DirHolder) holder;
        viewHolder.binding.getRoot().setText(mDataSet.get(position));
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return null;
    }

    static class DirHolder extends BaseAdapter.BaseViewHolder {
        final ItemDirBinding binding;

        DirHolder(ItemDirBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
