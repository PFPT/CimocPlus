package com.haleydu.cimoc.ui.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PreCacheLayoutManager extends LinearLayoutManager {

    private int mExtraSpace = 0;

    public PreCacheLayoutManager(Context context) {
        super(context);
    }

    public PreCacheLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public void setExtraSpace(int extraSpace) {
        mExtraSpace = extraSpace;
    }

    @Override
    protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {
        int extra = 0;
        if (mExtraSpace > 0) {
            extra = getOrientation() == LinearLayoutManager.HORIZONTAL
                    ? mExtraSpace * getWidth()
                    : mExtraSpace * getHeight();
        }
        extraLayoutSpace[0] = extra;
        extraLayoutSpace[1] = extra;
    }

}
