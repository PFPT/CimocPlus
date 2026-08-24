package com.haleydu.cimoc.ui.settings;
import com.haleydu.cimoc.ui.common.BackActivity;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.databinding.ActivityBackupBinding;
import com.haleydu.cimoc.data.PreferenceManager;
import com.haleydu.cimoc.ui.common.FlowExtKt;
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment;
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment;
import com.haleydu.cimoc.ui.widget.preference.CheckBoxPreference;
import com.haleydu.cimoc.utils.PermissionUtils;
import com.haleydu.cimoc.utils.StringUtils;
import dagger.hilt.android.AndroidEntryPoint;


/**
 * Created by Hiroshi on 2016/10/19.
 */

@AndroidEntryPoint
public class BackupActivity extends BackActivity implements DialogCaller {

    private static final int DIALOG_REQUEST_RESTORE_COMIC = 0;
    private static final int DIALOG_REQUEST_RESTORE_TAG = 1;
    private static final int DIALOG_REQUEST_RESTORE_SETTINGS = 2;
    private static final int DIALOG_REQUEST_RESTORE_CLEAR = 3;

    View mLayoutView;
    CheckBoxPreference mSaveComicAuto;

    private BackupViewModel vm;
    private ActivityBackupBinding binding;

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(BackupViewModel.class);
    }

    @Override
    protected void initData() {
        FlowExtKt.collectOnStart(vm.getEvents(), this, event -> {
            if (event instanceof BackupViewModel.Event.ComicFiles) {
                onComicFileLoadSuccess(((BackupViewModel.Event.ComicFiles) event).getFiles());
            } else if (event instanceof BackupViewModel.Event.TagFiles) {
                onTagFileLoadSuccess(((BackupViewModel.Event.TagFiles) event).getFiles());
            } else if (event instanceof BackupViewModel.Event.SettingsFiles) {
                onSettingsFileLoadSuccess(((BackupViewModel.Event.SettingsFiles) event).getFiles());
            } else if (event instanceof BackupViewModel.Event.ClearFiles) {
                onClearFileLoadSuccess(((BackupViewModel.Event.ClearFiles) event).getFiles());
            } else if (event instanceof BackupViewModel.Event.FileLoadFail) {
                onFileLoadFail();
            } else if (event instanceof BackupViewModel.Event.SaveSuccess) {
                onBackupSaveSuccess(((BackupViewModel.Event.SaveSuccess) event).getSize());
            } else if (event instanceof BackupViewModel.Event.SaveFail) {
                onBackupSaveFail();
            } else if (event instanceof BackupViewModel.Event.RestoreSuccess) {
                onBackupRestoreSuccess();
            } else if (event instanceof BackupViewModel.Event.RestoreFail) {
                onBackupRestoreFail();
            } else if (event instanceof BackupViewModel.Event.ClearSuccess) {
                onClearBackupSuccess();
            } else if (event instanceof BackupViewModel.Event.ClearFail) {
                onClearBackupFail();
            }
        });
    }

    @Override
    protected void initView() {
        super.initView();
        mSaveComicAuto.bindPreference(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true);
    }

    void onSaveFavoriteClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveComic();
        } else {
            onFileLoadFail();
        }
    }

    void onSaveTagClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveTag();
        } else {
            onFileLoadFail();
        }
    }

    void onSaveSettingsClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveSettings();
        } else {
            onFileLoadFail();
        }
    }

    void onRestoreFavoriteClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadComicFile();
        } else {
            onFileLoadFail();
        }
    }

    void onRestoreTagClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadTagFile();
        } else {
            onFileLoadFail();
        }
    }

    void onRestoreSettingsClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadSettingsFile();
        } else {
            onFileLoadFail();
        }
    }

    void onClearRecordClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadClearBackupFile();
        } else {
            onFileLoadFail();
        }
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_RESTORE_COMIC:
                showProgressDialog();
                vm.restoreComic(bundle.getString(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_RESTORE_TAG:
                showProgressDialog();
                vm.restoreTag(bundle.getString(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_RESTORE_SETTINGS:
                showProgressDialog();
                vm.restoreSetting(bundle.getString(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_RESTORE_CLEAR:
                showProgressDialog();
                vm.clearBackup();
                break;
        }
    }

    public void onComicFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_comic, file, DIALOG_REQUEST_RESTORE_COMIC);
    }

    public void onTagFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_tag, file, DIALOG_REQUEST_RESTORE_TAG);
    }

    public void onSettingsFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_settings, file, DIALOG_REQUEST_RESTORE_SETTINGS);
    }

    private void showChoiceDialog(int title, String[] item, int request) {
        hideProgressDialog();
        ChoiceDialogFragment fragment = ChoiceDialogFragment.newInstance(title, item, -1, request);
        fragment.show(getSupportFragmentManager(), null);
    }

    public void onClearFileLoadSuccess(String[] file) {
        hideProgressDialog();
        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.backup_clear_record,
                R.string.backup_clear_record_notice_summary, true, DIALOG_REQUEST_RESTORE_CLEAR);
        fragment.show(getSupportFragmentManager(), null);
    }

    public void onFileLoadFail() {
        hideProgressDialog();
        showSnackbar(R.string.backup_restore_not_found);
    }

    public void onBackupRestoreSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_success);
    }

    public void onClearBackupSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_clear_success);
    }

    public void onClearBackupFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_clear_fail);
    }

    public void onBackupRestoreFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    public void onBackupSaveSuccess(int size) {
        hideProgressDialog();
        showSnackbar(StringUtils.format(getString(R.string.backup_save_success), size));
    }

    public void onBackupSaveFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.drawer_backup);
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_backup;
    }

    @Override
    protected View getLayoutView() {
        return mLayoutView;
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mLayoutView = binding.backupLayout;
        mSaveComicAuto = binding.backupSaveComicAuto;
        binding.backupSaveComic.setOnClickListener(v -> onSaveFavoriteClick());
        binding.backupSaveTag.setOnClickListener(v -> onSaveTagClick());
        binding.backupSaveSettings.setOnClickListener(v -> onSaveSettingsClick());
        binding.backupRestoreComic.setOnClickListener(v -> onRestoreFavoriteClick());
        binding.backupRestoreTag.setOnClickListener(v -> onRestoreTagClick());
        binding.backupRestoreSettings.setOnClickListener(v -> onRestoreSettingsClick());
        binding.backupClearRecord.setOnClickListener(v -> onClearRecordClick());
    }

}
