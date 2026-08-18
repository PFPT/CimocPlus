package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.haleydu.cimoc.App;
import com.haleydu.cimoc.databinding.ItemResultBinding;
import com.haleydu.cimoc.fresco.ControllerBuilderProvider;
import com.haleydu.cimoc.manager.SourceManager;
import com.haleydu.cimoc.model.Comic;

import java.util.List;


/**
 * Created by Hiroshi on 2016/7/3.
 */
public class ResultAdapter extends BaseAdapter<Comic> {

    private ControllerBuilderProvider mProvider;
    private SourceManager.TitleGetter mTitleGetter;

    public ResultAdapter(Context context, List<Comic> list) {
        super(context, list);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ResultViewHolder(ItemResultBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Comic comic = mDataSet.get(position);
        ResultViewHolder viewHolder = (ResultViewHolder) holder;
        viewHolder.binding.resultComicTitle.setText(comic.getTitle());
        viewHolder.binding.resultComicAuthor.setText(comic.getAuthor());
        viewHolder.binding.resultComicSource.setText(mTitleGetter.getTitle(comic.getSource()));
        viewHolder.binding.resultComicUpdate.setText(comic.getUpdate());
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(comic.getCover()))
                .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                .build();
        viewHolder.binding.resultComicImage.setController(mProvider.get(comic.getSource()).setImageRequest(request).build());
    }

    public void setProvider(ControllerBuilderProvider provider) {
        mProvider = provider;
    }

    public void setTitleGetter(SourceManager.TitleGetter getter) {
        mTitleGetter = getter;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int offset = parent.getWidth() / 90;
                outRect.set(0, 0, 0, offset);
            }
        };
    }

    static class ResultViewHolder extends BaseViewHolder {
        final ItemResultBinding binding;

        ResultViewHolder(ItemResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
