package com.haleydu.cimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import java.util.List;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.haleydu.cimoc.App;
import com.haleydu.cimoc.databinding.ItemGridBinding;
import com.haleydu.cimoc.fresco.ControllerBuilderProvider;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.manager.SourceManager;
import com.haleydu.cimoc.model.MiniComic;
import com.haleydu.cimoc.utils.FrescoUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Hiroshi on 2016/7/1.
 */
public class GridAdapter extends BaseAdapter<Object> {

    public static final int TYPE_GRID = 2016101213;

    private ControllerBuilderProvider mProvider;
    private SourceManager.TitleGetter mTitleGetter;
    private boolean symbol = false;

    public GridAdapter(Context context, List<Object> list) {
        super(context, list);
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_GRID;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GridHolder(ItemGridBinding.inflate(mInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_GRID:
            default:
                MiniComic comic = (MiniComic) mDataSet.get(position);
                GridHolder gridHolder = (GridHolder) holder;
                gridHolder.binding.itemGridTitle.setText(comic.getTitle());
                gridHolder.binding.itemGridSubtitle.setText(mTitleGetter.getTitle(comic.getSource()));
                if (mProvider != null) {
                    //            ImageRequest request = ImageRequestBuilder
                    //                    .newBuilderWithSource(Uri.parse(comic.getCover()))
                    //                    .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                    //                    .build();
                    ImageRequest request = null;
                    try {
                        if (!App.getManager_wifi().isWifiEnabled() && App.getPreferenceManager().getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)) {
                            //                    request = null;
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        } else if (!App.getManager_wifi().isWifiEnabled() && App.getPreferenceManager().getBoolean(PreferenceManager.PREF_OTHER_LOADCOVER_ONLY_WIFI, false)) {
                            //                    request = null;
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        } else {
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            } else {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.parse(comic.getCover()))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DraweeController controller = mProvider.get(comic.getSource())
                            .setOldController(gridHolder.binding.itemGridImage.getController())
                            .setImageRequest(request)
                            .build();
                    gridHolder.binding.itemGridImage.setController(controller);
                }
                gridHolder.binding.itemGridSymbol.setVisibility(symbol && comic.isHighlight() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setProvider(ControllerBuilderProvider provider) {
        mProvider = provider;
    }

    public void setTitleGetter(SourceManager.TitleGetter getter) {
        mTitleGetter = getter;
    }

    public void setSymbol(boolean symbol) {
        this.symbol = symbol;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NotNull Rect outRect, @NotNull View view,
                                       @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
                int offset = parent.getWidth() / 90;
                outRect.set(offset, 0, offset, (int) (2.8 * offset));
            }
        };
    }

    public void removeItemById(long id) {
        for (Object O_comic : mDataSet) {
            MiniComic comic = (MiniComic) O_comic;
            if (id == comic.getId()) {
                remove(comic);
                break;
            }
        }
    }

    public int findFirstNotHighlight() {
        int count = 0;
        if (symbol) {
            for (Object O_comic : mDataSet) {
                MiniComic comic = (MiniComic) O_comic;
                if (!comic.isHighlight()) {
                    break;
                }
                ++count;
            }
        }
        return count;
    }

    public void cancelAllHighlight() {
        int count = 0;
        for (Object O_comic : mDataSet) {
            MiniComic comic = (MiniComic) O_comic;
            if (!comic.isHighlight()) {
                break;
            }
            ++count;
            comic.setHighlight(false);
        }
        notifyItemRangeChanged(0, count);
    }

    public void moveItemTop(MiniComic comic) {
        if (remove(comic)) {
            add(findFirstNotHighlight(), comic);
        }
    }

    static class GridHolder extends BaseViewHolder {
        final ItemGridBinding binding;

        GridHolder(ItemGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
