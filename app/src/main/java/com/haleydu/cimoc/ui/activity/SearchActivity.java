package com.haleydu.cimoc.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.databinding.ActivitySearchBinding;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.misc.Switcher;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.AutoCompleteAdapter;
import com.haleydu.cimoc.ui.fragment.dialog.MultiAdpaterDialogFragment;
import com.haleydu.cimoc.utils.CollectionUtils;
import com.haleydu.cimoc.utils.HintUtils;
import com.haleydu.cimoc.utils.StringUtils;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Hiroshi on 2016/10/11.
 */

@AndroidEntryPoint
public class SearchActivity extends BackActivity implements DialogCaller, TextView.OnEditorActionListener {

    private final static int DIALOG_REQUEST_SOURCE = 0;

    TextInputLayout mInputLayout;
    AppCompatAutoCompleteTextView mEditText;
    FloatingActionButton mActionButton;
    AppCompatCheckBox mCheckBox;

    private ArrayAdapter<String> mArrayAdapter;

    private SearchViewModel vm;
    private ActivitySearchBinding binding;
    private List<Switcher<Source>> mSourceList;
    private boolean mAutoComplete;
    private int mFilterSource = -1;

    public static Intent createIntent(Context context, int sourceType, String title) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(Extra.EXTRA_SOURCE, sourceType);
        intent.putExtra(Extra.EXTRA_KEYWORD, title);
        return intent;
    }

    @Override
    protected void initView() {
        mAutoComplete = mPreference.getBoolean(PreferenceManager.PREF_SEARCH_AUTO_COMPLETE, false);
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mActionButton != null && !mActionButton.isShown()) {
                    mActionButton.show();
                }
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mInputLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAutoComplete) {
                    String keyword = mEditText.getText().toString();
                    if (!StringUtils.isEmpty(keyword)) {
                        vm.loadAutoComplete(keyword);
                    }
                }
            }
        });
        mEditText.setOnEditorActionListener(this);
        if (mAutoComplete) {
            mArrayAdapter = new AutoCompleteAdapter(this);
            mEditText.setAdapter(mArrayAdapter);
        }
    }

    @Override
    protected void initData() {
        vm = new ViewModelProvider(this).get(SearchViewModel.class);
        mSourceList = new ArrayList<>();
        mFilterSource = getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1);
        FlowExtKt.collectOnStart(vm.getSources(), this, this::onSourceLoadSuccess);
        FlowExtKt.collectOnStart(vm.getSourceFail(), this, unit -> onSourceLoadFail());
        FlowExtKt.collectOnStart(vm.getAutoComplete(), this, this::onAutoCompleteLoadSuccess);
        vm.loadSource();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_menu_source:
                if (!mSourceList.isEmpty()) {
                    int size = mSourceList.size();
                    String[] arr1 = new String[size];
                    boolean[] arr2 = new boolean[size];
                    for (int i = 0; i < size; ++i) {
                        arr1[i] = mSourceList.get(i).getElement().getTitle();
                        arr2[i] = mSourceList.get(i).isEnable();
                    }
                    MultiAdpaterDialogFragment fragment =
                            MultiAdpaterDialogFragment.newInstance(R.string.search_source_select, arr1, arr2, DIALOG_REQUEST_SOURCE);
                    fragment.show(getSupportFragmentManager(), null);
                    break;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_SOURCE:
                boolean[] check = bundle.getBooleanArray(EXTRA_DIALOG_RESULT_VALUE);
                if (check != null) {
                    int size = mSourceList.size();
                    for (int i = 0; i < size; ++i) {
                        mSourceList.get(i).setEnable(check[i]);
                    }
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            mActionButton.performClick();
            return true;
        }
        return false;
    }

    void onSearchButtonClick() {
        String keyword = mEditText.getText().toString();
        Boolean strictSearch = mCheckBox.isChecked();
        if (StringUtils.isEmpty(keyword)) {
            mInputLayout.setError(getString(R.string.search_keyword_empty));
        } else {
            ArrayList<Integer> list = new ArrayList<>();
            for (Switcher<Source> switcher : mSourceList) {
                if (switcher.isEnable()) {
                    list.add(switcher.getElement().getType());
                }
            }
            if (list.isEmpty()) {
                HintUtils.showToast(this, R.string.search_source_none);
            } else {
                startActivity(ResultActivity.createIntent(this, keyword, strictSearch,
                        CollectionUtils.unbox(list), ResultActivity.LAUNCH_MODE_SEARCH));
            }
        }
    }

    public void onAutoCompleteLoadSuccess(List<String> list) {
        mArrayAdapter.clear();
        mArrayAdapter.addAll(list);
    }

    public void onSourceLoadSuccess(List<Source> list) {
        hideProgressBar();
        boolean found = false;
        for (Source source : list) {
            boolean enable = mFilterSource < 0 || source.getType() == mFilterSource;
            if (enable && source.getType() == mFilterSource) {
                found = true;
            }
            mSourceList.add(new Switcher<>(source, enable));
        }
        if (mFilterSource >= 0 && !found) {
            Source source = vm.loadSource(mFilterSource);
            if (source != null) {
                mSourceList.add(new Switcher<>(source, true));
            }
        }
        if (mFilterSource >= 0 && mToolbar != null) {
            String title = getIntent().getStringExtra(Extra.EXTRA_KEYWORD);
            if (!StringUtils.isEmpty(title)) {
                mToolbar.setTitle(title);
            }
        }
    }

    public void onSourceLoadFail() {
        hideProgressBar();
        HintUtils.showToast(this, R.string.search_source_load_fail);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.comic_search);
    }

    @Override
    protected View inflateContentView() {
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_search;
    }

    @Override
    protected boolean isNavTranslation() {
        return true;
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mInputLayout = binding.searchTextLayout;
        mEditText = binding.searchKeywordInput;
        mActionButton = binding.searchActionButton;
        mCheckBox = binding.searchStrictCheckbox;
        binding.searchActionButton.setOnClickListener(v -> onSearchButtonClick());
    }

}
