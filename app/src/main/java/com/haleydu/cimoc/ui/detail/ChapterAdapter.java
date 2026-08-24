package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.haleydu.cimoc.databinding.ItemChapterBinding;
import com.haleydu.cimoc.databinding.ItemSelectBinding;
import com.haleydu.cimoc.misc.Switcher;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.ui.widget.ChapterButton;

import java.util.List;


/**
 * Created by Hiroshi on 2016/11/15.
 */

public class ChapterAdapter extends BaseAdapter<Switcher<Chapter>> {

    private static final int TYPE_ITEM = 2017030222;
    private static final int TYPE_BUTTON = 2017030223;

    private boolean isButtonMode = false;

    public ChapterAdapter(Context context, List<Switcher<Chapter>> list) {
        super(context, list);
    }

    @Override
    public int getItemViewType(int position) {
        return isButtonMode ? TYPE_BUTTON : TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new ItemHolder(ItemSelectBinding.inflate(mInflater, parent, false));
        }
        return new ButtonHolder(ItemChapterBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Switcher<Chapter> switcher = mDataSet.get(position);
        if (isButtonMode) {
            final ButtonHolder viewHolder = (ButtonHolder) holder;
            viewHolder.chapterButton.setText(switcher.getElement().getTitle());
            if (switcher.getElement().isDownload()) {
                viewHolder.chapterButton.setDownload(true);
                viewHolder.chapterButton.setSelected(false);
            } else {
                viewHolder.chapterButton.setDownload(false);
                viewHolder.chapterButton.setSelected(switcher.isEnable());
            }
        } else {
            ItemHolder viewHolder = (ItemHolder) holder;
            viewHolder.binding.itemSelectTitle.setText(switcher.getElement().getTitle());
            viewHolder.binding.itemSelectCheckbox.setEnabled(!switcher.getElement().isDownload());
            viewHolder.binding.itemSelectCheckbox.setChecked(switcher.isEnable());
        }
    }

    public void setButtonMode(boolean enable) {
        isButtonMode = enable;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int offset = parent.getWidth() / 40;
                outRect.set(offset, 0, offset, (int) (offset * 1.5));
            }
        };
    }

    @Override
    protected boolean isClickValid() {
        return true;
    }

    static class ItemHolder extends BaseAdapter.BaseViewHolder {
        final ItemSelectBinding binding;

        ItemHolder(ItemSelectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ButtonHolder extends BaseAdapter.BaseViewHolder {
        final ChapterButton chapterButton;

        ButtonHolder(ItemChapterBinding binding) {
            super(binding.getRoot());
            chapterButton = (ChapterButton) binding.getRoot();
        }
    }

}
