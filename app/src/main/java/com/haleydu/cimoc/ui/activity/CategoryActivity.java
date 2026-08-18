package com.haleydu.cimoc.ui.activity;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.widget.AppCompatSpinner;
import android.view.View;
import android.widget.AdapterView;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivityCategoryBinding;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.SourceManager;
import com.haleydu.cimoc.parser.Category;
import com.haleydu.cimoc.ui.adapter.CategoryAdapter;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


/**
 * Created by Hiroshi on 2016/12/11.
 */

@AndroidEntryPoint
public class CategoryActivity extends BackActivity implements AdapterView.OnItemSelectedListener {

    List<AppCompatSpinner> mSpinnerList;
    List<View> mCategoryView;
    private ActivityCategoryBinding binding;
    @Inject
    SourceManager sourceManager;

    private Category mCategory;

    public static Intent createIntent(Context context, int source, String title) {
        Intent intent = new Intent(context, CategoryActivity.class);
        intent.putExtra(Extra.EXTRA_SOURCE, source);
        intent.putExtra(Extra.EXTRA_KEYWORD, title);
        return intent;
    }

    @Override
    protected void initView() {
        int source = getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1);
        if (mToolbar != null) {
            mToolbar.setTitle(getIntent().getStringExtra(Extra.EXTRA_KEYWORD));
        }
        mCategory = sourceManager.getParser(source).getCategory();
        initSpinner();
    }

    private void initSpinner() {
        int[] type = new int[]{Category.CATEGORY_SUBJECT, Category.CATEGORY_AREA, Category.CATEGORY_READER,
                Category.CATEGORY_YEAR, Category.CATEGORY_PROGRESS, Category.CATEGORY_ORDER};
        for (int i = 0; i != type.length; ++i) {
            if (mCategory.hasAttribute(type[i])) {
                mCategoryView.get(i).setVisibility(View.VISIBLE);
                if (!mCategory.isComposite()) {
                    mSpinnerList.get(i).setOnItemSelectedListener(this);
                }
                mSpinnerList.get(i).setAdapter(new CategoryAdapter(this, mCategory.getAttrList(type[i])));
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        for (AppCompatSpinner spinner : mSpinnerList) {
            if (position == 0) {
                spinner.setEnabled(true);
            } else if (!parent.equals(spinner)) {
                spinner.setEnabled(false);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    void onActionButtonClick() {
        String[] args = new String[mSpinnerList.size()];
        for (int i = 0; i != args.length; ++i) {
            args[i] = getSpinnerValue(mSpinnerList.get(i));
        }
        int source = getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1);
        String format = mCategory.getFormat(args);
        Intent intent = ResultActivity.createIntent(this, format, source, ResultActivity.LAUNCH_MODE_CATEGORY);
        startActivity(intent);
    }

    private String getSpinnerValue(AppCompatSpinner spinner) {
        if (!spinner.isShown()) {
            return null;
        }
        return ((CategoryAdapter) spinner.getAdapter()).getValue(spinner.getSelectedItemPosition());
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.category);
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_category;
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mSpinnerList = Arrays.asList(binding.categorySpinnerSubject, binding.categorySpinnerArea, binding.categorySpinnerReader, binding.categorySpinnerYear, binding.categorySpinnerProgress, binding.categorySpinnerOrder);
        mCategoryView = Arrays.asList(binding.categorySubject, binding.categoryArea, binding.categoryReader, binding.categoryYear, binding.categoryProgress, binding.categoryOrder);
        binding.categoryActionButton.setOnClickListener(v -> onActionButtonClick());
    }

}
