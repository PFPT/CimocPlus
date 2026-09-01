package com.haleydu.cimoc.ui.settings
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivityBackupBinding
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.ui.common.BackActivity
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.ui.widget.preference.CheckBoxPreference
import com.haleydu.cimoc.utils.PermissionUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackupActivity : BackActivity(), DialogCaller {

    lateinit var mLayoutView: View
    lateinit var mSaveComicAuto: CheckBoxPreference

    private lateinit var vm: BackupViewModel
    private lateinit var binding: ActivityBackupBinding

    override fun initViewModel() {
        vm = ViewModelProvider(this)[BackupViewModel::class.java]
    }

    override fun initData() {
        vm.events.collectOnStart(this) { event ->
            when (event) {
                is BackupViewModel.Event.ComicFiles -> onComicFileLoadSuccess(event.files)
                is BackupViewModel.Event.TagFiles -> onTagFileLoadSuccess(event.files)
                is BackupViewModel.Event.SettingsFiles -> onSettingsFileLoadSuccess(event.files)
                is BackupViewModel.Event.ClearFiles -> onClearFileLoadSuccess(event.files)
                is BackupViewModel.Event.FileLoadFail -> onFileLoadFail()
                is BackupViewModel.Event.SaveSuccess -> onBackupSaveSuccess(event.size)
                is BackupViewModel.Event.SaveFail -> onBackupSaveFail()
                is BackupViewModel.Event.RestoreSuccess -> onBackupRestoreSuccess()
                is BackupViewModel.Event.RestoreFail -> onBackupRestoreFail()
                is BackupViewModel.Event.ClearSuccess -> onClearBackupSuccess()
                is BackupViewModel.Event.ClearFail -> onClearBackupFail()
            }
        }
    }

    override fun initView() {
        super.initView()
        mSaveComicAuto.bindPreference(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true)
    }

    fun onSaveFavoriteClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveComic()
        } else {
            onFileLoadFail()
        }
    }

    fun onSaveTagClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveTag()
        } else {
            onFileLoadFail()
        }
    }

    fun onSaveSettingsClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.saveSettings()
        } else {
            onFileLoadFail()
        }
    }

    fun onRestoreFavoriteClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadComicFile()
        } else {
            onFileLoadFail()
        }
    }

    fun onRestoreTagClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadTagFile()
        } else {
            onFileLoadFail()
        }
    }

    fun onRestoreSettingsClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadSettingsFile()
        } else {
            onFileLoadFail()
        }
    }

    fun onClearRecordClick() {
        showProgressDialog()
        if (PermissionUtils.hasStoragePermission(this)) {
            vm.loadClearBackupFile()
        } else {
            onFileLoadFail()
        }
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_RESTORE_COMIC -> {
                showProgressDialog()
                vm.restoreComic(bundle.getString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE))
            }
            DIALOG_REQUEST_RESTORE_TAG -> {
                showProgressDialog()
                vm.restoreTag(bundle.getString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE))
            }
            DIALOG_REQUEST_RESTORE_SETTINGS -> {
                showProgressDialog()
                vm.restoreSetting(bundle.getString(DialogCaller.EXTRA_DIALOG_RESULT_VALUE))
            }
            DIALOG_REQUEST_RESTORE_CLEAR -> {
                showProgressDialog()
                vm.clearBackup()
            }
        }
    }

    fun onComicFileLoadSuccess(file: Array<String>) {
        showChoiceDialog(R.string.backup_restore_comic, file, DIALOG_REQUEST_RESTORE_COMIC)
    }

    fun onTagFileLoadSuccess(file: Array<String>) {
        showChoiceDialog(R.string.backup_restore_tag, file, DIALOG_REQUEST_RESTORE_TAG)
    }

    fun onSettingsFileLoadSuccess(file: Array<String>) {
        showChoiceDialog(R.string.backup_restore_settings, file, DIALOG_REQUEST_RESTORE_SETTINGS)
    }

    private fun showChoiceDialog(title: Int, item: Array<String>, request: Int) {
        hideProgressDialog()
        val fragment = ChoiceDialogFragment.newInstance(title, item, -1, request)
        fragment.show(supportFragmentManager, null)
    }

    fun onClearFileLoadSuccess(file: Array<String>) {
        hideProgressDialog()
        val fragment = MessageDialogFragment.newInstance(
            R.string.backup_clear_record,
            R.string.backup_clear_record_notice_summary,
            true,
            DIALOG_REQUEST_RESTORE_CLEAR
        )
        fragment.show(supportFragmentManager, null)
    }

    fun onFileLoadFail() {
        hideProgressDialog()
        showSnackbar(R.string.backup_restore_not_found)
    }

    fun onBackupRestoreSuccess() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_success)
    }

    fun onClearBackupSuccess() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_clear_success)
    }

    fun onClearBackupFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_clear_fail)
    }

    fun onBackupRestoreFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_fail)
    }

    fun onBackupSaveSuccess(size: Int) {
        hideProgressDialog()
        showSnackbar(StringUtils.format(getString(R.string.backup_save_success), size))
    }

    fun onBackupSaveFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_fail)
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.drawer_backup)
    }

    override fun inflateContentView(): View {
        binding = ActivityBackupBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_backup
    }

    override fun getLayoutView(): View {
        return mLayoutView
    }

    override fun bindViews() {
        super.bindViews()
        mLayoutView = binding.backupLayout
        mSaveComicAuto = binding.backupSaveComicAuto
        binding.backupSaveComic.setOnClickListener { onSaveFavoriteClick() }
        binding.backupSaveTag.setOnClickListener { onSaveTagClick() }
        binding.backupSaveSettings.setOnClickListener { onSaveSettingsClick() }
        binding.backupRestoreComic.setOnClickListener { onRestoreFavoriteClick() }
        binding.backupRestoreTag.setOnClickListener { onRestoreTagClick() }
        binding.backupRestoreSettings.setOnClickListener { onRestoreSettingsClick() }
        binding.backupClearRecord.setOnClickListener { onClearRecordClick() }
    }

    companion object {
        private const val DIALOG_REQUEST_RESTORE_COMIC = 0
        private const val DIALOG_REQUEST_RESTORE_TAG = 1
        private const val DIALOG_REQUEST_RESTORE_SETTINGS = 2
        private const val DIALOG_REQUEST_RESTORE_CLEAR = 3
    }
}
