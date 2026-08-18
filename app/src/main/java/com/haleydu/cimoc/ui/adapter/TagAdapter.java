package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.haleydu.cimoc.databinding.ItemTagBinding;
import com.haleydu.cimoc.model.Tag;

import java.util.List;


/**
 * Created by Hiroshi on 2016/10/11.
 */

public class TagAdapter extends BaseAdapter<Tag> {

    private @ColorInt
    int color = -1;

    public TagAdapter(Context context, List<Tag> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagHolder(ItemTagBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Tag tag = mDataSet.get(position);
        TagHolder viewHolder = (TagHolder) holder;
        viewHolder.binding.itemTagTitle.setText(tag.getTitle());
        if (color != -1) {
            viewHolder.binding.itemTagTitle.setBackgroundColor(color);
        }
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int offset = parent.getWidth() / 90;
                outRect.set(offset, 0, offset, (int) (offset * 1.5));
            }
        };
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
    }

    static class TagHolder extends BaseViewHolder {
        final ItemTagBinding binding;

        TagHolder(ItemTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
