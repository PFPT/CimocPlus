package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.haleydu.cimoc.databinding.ItemSelectBinding;
import com.haleydu.cimoc.misc.Switcher;
import com.haleydu.cimoc.model.Tag;

import java.util.List;


/**
 * Created by Hiroshi on 2016/12/2.
 */

public class TagEditorAdapter extends BaseAdapter<Switcher<Tag>> {

    public TagEditorAdapter(Context context, List<Switcher<Tag>> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagHolder(ItemSelectBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        TagHolder viewHolder = (TagHolder) holder;
        Switcher<Tag> switcher = mDataSet.get(position);
        viewHolder.binding.itemSelectTitle.setText(switcher.getElement().getTitle());
        viewHolder.binding.itemSelectCheckbox.setChecked(switcher.isEnable());
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return null;
    }

    @Override
    protected boolean isClickValid() {
        return true;
    }

    static class TagHolder extends BaseAdapter.BaseViewHolder {
        final ItemSelectBinding binding;

        TagHolder(ItemSelectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
