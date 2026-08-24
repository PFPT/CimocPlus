package com.haleydu.cimoc.ui.explore;
import com.haleydu.cimoc.ui.common.BaseAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.haleydu.cimoc.databinding.ItemSourceBinding;
import com.haleydu.cimoc.model.Source;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Hiroshi on 2016/10/10.
 */

public class SourceAdapter extends BaseAdapter<Source> {

    private OnItemCheckedListener mOnItemCheckedListener;
    private @ColorInt
    int color = -1;
    private final Set<Integer> invalidTypes = new HashSet<>();

    public SourceAdapter(Context context, List<Source> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SourceHolder(ItemSourceBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Source source = mDataSet.get(position);
        final SourceHolder viewHolder = (SourceHolder) holder;
        viewHolder.binding.itemSourceTitle.setText(source.getTitle());
        viewHolder.binding.itemSourceInvalid.setVisibility(
                invalidTypes.contains(source.getType()) ? View.VISIBLE : View.GONE);
        viewHolder.binding.itemSourceSwitch.setChecked(source.getEnable());
        viewHolder.binding.itemSourceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mOnItemCheckedListener != null) {
                    mOnItemCheckedListener.onItemCheckedListener(isChecked, viewHolder.getAdapterPosition());
                }
            }
        });
        if (color != -1) {
            ColorStateList thumbList = new ColorStateList(new int[][]{{-android.R.attr.state_checked}, {android.R.attr.state_checked}},
                    new int[]{Color.WHITE, color});
            viewHolder.binding.itemSourceSwitch.setThumbTintList(thumbList);
            ColorStateList trackList = new ColorStateList(new int[][]{{-android.R.attr.state_checked}, {android.R.attr.state_checked}},
                    new int[]{0x4C000000, (0x00FFFFFF & color | 0x4C000000)});
            viewHolder.binding.itemSourceSwitch.setTrackTintList(trackList);
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

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        mOnItemCheckedListener = listener;
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
    }

    public void setInvalidTypes(Set<Integer> types) {
        invalidTypes.clear();
        if (types != null) {
            invalidTypes.addAll(types);
        }
        notifyDataSetChanged();
    }

    public interface OnItemCheckedListener {
        void onItemCheckedListener(boolean isChecked, int position);
    }

    static class SourceHolder extends BaseViewHolder {
        final ItemSourceBinding binding;

        SourceHolder(ItemSourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
