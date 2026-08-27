package com.haleydu.cimoc.ui.settings
import com.haleydu.cimoc.ui.common.BackActivity
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.databinding.ActivitySettingsBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.ui.settings.ReaderConfigActivity
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.ui.common.dialog.SliderDialogFragment
import com.haleydu.cimoc.ui.common.dialog.StorageEditorDialogFragment
import com.haleydu.cimoc.ui.library.DirPickerActivity
import com.haleydu.cimoc.utils.ServiceUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class SettingsActivity : BackActivity(), DialogCaller, StorageEditorDialogFragment.Host {

    private val vm: SettingsViewModel by viewModels()
    private lateinit var storagePath: String
    private var tempStorage: String? = null
    private val resultArray = IntArray(6)
    private val resultIntent = Intent()
    private lateinit var binding: ActivitySettingsBinding
    private val storagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleStoragePickerResult(result.data)
        }
    }

    override fun inflateContentView(): View {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        storagePath = appInstance.documentFile.uri.toString()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    when (event) {
                        SettingsViewModel.Event.MoveSuccess -> onFileMoveSuccess()
                        SettingsViewModel.Event.ExecuteSuccess -> onExecuteSuccess()
                        SettingsViewModel.Event.ExecuteFail -> onExecuteFail()
                    }
                }
            }
        }
        binding.settingsCompose.setContent {
            GuofengComposeTheme {
                SettingsScreen(
                    preference = mPreference,
                    onReaderConfig = {
                        startActivity(Intent(this@SettingsActivity, ReaderConfigActivity::class.java))
                    },
                    onStorage = { onOtherStorageClick() },
                    onScan = { onDownloadScanClick() },
                    onClearCache = {
                        showProgressDialog()
                        vm.clearCache()
                        showSnackbar(R.string.common_execute_success)
                        hideProgressDialog()
                    },
                    onChoice = { title, items, value, request ->
                        ChoiceDialogFragment.newInstance(title, items, value, request)
                            .show(supportFragmentManager, null)
                    },
                    onSlider = { title, min, max, value, request ->
                        SliderDialogFragment.newInstance(title, min, max, value, request)
                            .show(supportFragmentManager, null)
                    }
                )
            }
        }
    }

    override fun launchStoragePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                storagePickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
            } catch (_: ActivityNotFoundException) {
                onDialogResult(DIALOG_REQUEST_OTHER_STORAGE, Bundle())
            }
        } else {
            storagePickerLauncher.launch(Intent(this, DirPickerActivity::class.java))
        }
    }

    private fun handleStoragePickerResult(data: Intent?) {
        showProgressDialog()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && data != null) {
            val uri: Uri? = data.data
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri!!, flags)
            tempStorage = uri.toString()
            vm.moveFiles(DocumentFile.fromTreeUri(this, uri))
        } else {
            val path = data?.getStringExtra(Extra.EXTRA_PICKER_PATH)
            if (path != null) {
                val file = DocumentFile.fromFile(File(path))
                vm.moveFiles(file)
            } else {
                onExecuteFail()
            }
        }
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_DOWNLOAD_SCAN -> {
                showProgressDialog()
                vm.scanTask()
            }
            DIALOG_REQUEST_OTHER_STORAGE -> showSnackbar(R.string.settings_other_storage_not_found)
            DIALOG_REQUEST_OTHER_NIGHT_ALPHA -> {
                val alpha = bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
                mPreference.putInt(PreferenceManager.PREF_OTHER_NIGHT_ALPHA, alpha)
                mNightMask?.setBackgroundColor(alpha shl 24)
                resultArray[4] = 1
                resultArray[5] = alpha
                resultIntent.putExtra(Extra.EXTRA_RESULT, resultArray)
                setResult(Activity.RESULT_OK, resultIntent)
            }
            DIALOG_REQUEST_OTHER_LAUNCH ->
                mPreference.putInt(PreferenceManager.PREF_OTHER_LAUNCH, bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX))
            DIALOG_REQUEST_READER_MODE ->
                mPreference.putInt(PreferenceManager.PREF_READER_MODE, bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX))
            DIALOG_REQUEST_DOWNLOAD_THREAD ->
                mPreference.putInt(PreferenceManager.PREF_DOWNLOAD_THREAD, bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE))
            DIALOG_REQUEST_READER_SCALE_FACTOR ->
                mPreference.putInt(PreferenceManager.PREF_READER_SCALE_FACTOR, bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE))
            DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD ->
                mPreference.putInt(
                    PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD,
                    bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_VALUE)
                )
        }
    }

    private fun onOtherStorageClick() {
        if (ServiceUtils.isServiceRunning(this, DownloadService::class.java)) {
            showSnackbar(R.string.download_ask_stop)
        } else {
            StorageEditorDialogFragment.newInstance(R.string.settings_other_storage, storagePath, DIALOG_REQUEST_OTHER_STORAGE)
                .show(supportFragmentManager, null)
        }
    }

    private fun onDownloadScanClick() {
        if (ServiceUtils.isServiceRunning(this, DownloadService::class.java)) {
            showSnackbar(R.string.download_ask_stop)
        } else {
            MessageDialogFragment.newInstance(R.string.dialog_confirm, R.string.settings_download_scan_confirm, true, DIALOG_REQUEST_DOWNLOAD_SCAN)
                .show(supportFragmentManager, null)
        }
    }

    private fun onFileMoveSuccess() {
        hideProgressDialog()
        mPreference.putString(PreferenceManager.PREF_OTHER_STORAGE, tempStorage)
        storagePath = tempStorage ?: storagePath
        (application as App).initRootDocumentFile()
        showSnackbar(R.string.common_execute_success)
    }

    private fun onExecuteSuccess() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_success)
    }

    private fun onExecuteFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_fail)
    }

    override fun getDefaultTitle(): String = getString(R.string.drawer_settings)

    override fun getLayoutView(): View = binding.settingsLayout

    override fun getLayoutRes(): Int = R.layout.activity_settings

    companion object {
        private const val DIALOG_REQUEST_OTHER_LAUNCH = 0
        private const val DIALOG_REQUEST_READER_MODE = 1
        private const val DIALOG_REQUEST_OTHER_STORAGE = 3
        private const val DIALOG_REQUEST_DOWNLOAD_THREAD = 4
        private const val DIALOG_REQUEST_DOWNLOAD_SCAN = 6
        private const val DIALOG_REQUEST_OTHER_NIGHT_ALPHA = 7
        private const val DIALOG_REQUEST_READER_SCALE_FACTOR = 8
        private const val DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD = 9
    }
}

@Composable
private fun SettingsScreen(
    preference: PreferenceManager,
    onReaderConfig: () -> Unit,
    onStorage: () -> Unit,
    onScan: () -> Unit,
    onClearCache: () -> Unit,
    onChoice: (Int, Array<String>, Int, Int) -> Unit,
    onSlider: (Int, Int, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val readerItems = context.resources.getStringArray(R.array.reader_mode_items)
    val launchItems = context.resources.getStringArray(R.array.launch_items)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        PrefGroup(R.string.settings_reader) {
            ActionPref(R.string.settings_reader_mode) {
                onChoice(
                    R.string.settings_reader_mode,
                    readerItems,
                    preference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE),
                    1
                )
            }
            ActionPref(R.string.settings_reader_config, onClick = onReaderConfig)
            SwitchPref(R.string.settings_reader_keep_bright, PreferenceManager.PREF_READER_KEEP_BRIGHT, false, preference)
            SwitchPref(R.string.settings_reader_show_topbar, PreferenceManager.PREF_OTHER_SHOW_TOPBAR, false, preference)
            SwitchPref(R.string.settings_reader_hide_info, PreferenceManager.PREF_READER_HIDE_INFO, false, preference)
            SwitchPref(R.string.settings_reader_info_bottom, PreferenceManager.PREF_READER_INFO_BOTTOM, false, preference)
            SwitchPref(R.string.settings_reader_hide_load_toast, PreferenceManager.PREF_READER_HIDE_LOAD_TOAST, false, preference)
            SwitchPref(R.string.settings_reader_hide_nav, PreferenceManager.PREF_READER_HIDE_NAV, false, preference)
            SwitchPref(R.string.settings_reader_ban_double_click, PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false, preference)
            SwitchPref(R.string.settings_reader_paging, PreferenceManager.PREF_READER_PAGING, false, preference)
            SwitchPref(R.string.settings_reader_closeautoresizeimage, PreferenceManager.PREF_READER_CLOSEAUTORESIZEIMAGE, false, preference)
            SwitchPref(R.string.settings_reader_paging_reverse, PreferenceManager.PREF_READER_PAGING_REVERSE, false, preference)
            SwitchPref(R.string.settings_reader_white_edge, PreferenceManager.PREF_READER_WHITE_EDGE, false, preference)
            SwitchPref(R.string.settings_reader_white_background, PreferenceManager.PREF_READER_WHITE_BACKGROUND, false, preference)
            SwitchPref(R.string.settings_reader_volume_key_controls, PreferenceManager.PREF_READER_VOLUME_KEY_CONTROLS_PAGE_TURNING, false, preference)
            ActionPref(R.string.settings_reader_scale_factor) {
                onSlider(
                    R.string.settings_reader_scale_factor,
                    100,
                    300,
                    preference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200),
                    8
                )
            }
            ActionPref(R.string.settings_reader_controller_trig_threshold, showDivider = false) {
                onSlider(
                    R.string.settings_reader_controller_trig_threshold,
                    1,
                    100,
                    preference.getInt(PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD, 30),
                    9
                )
            }
        }

        PrefGroup(R.string.settings_download) {
            ActionPref(R.string.settings_download_thread) {
                onSlider(
                    R.string.settings_download_thread,
                    1,
                    10,
                    preference.getInt(PreferenceManager.PREF_DOWNLOAD_THREAD, 2),
                    4
                )
            }
            ActionPref(R.string.settings_download_scan, showDivider = false, onClick = onScan)
        }

        PrefGroup(R.string.settings_search) {
            SwitchPref(
                R.string.settings_search_auto_complete,
                PreferenceManager.PREF_SEARCH_AUTO_COMPLETE,
                false,
                preference,
                showDivider = false
            )
        }

        PrefGroup(R.string.settings_other) {
            SwitchPref(R.string.settings_other_connect_only_wifi, PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false, preference)
            SwitchPref(R.string.settings_other_loadcover_only_wifi, PreferenceManager.PREF_OTHER_LOADCOVER_ONLY_WIFI, false, preference)
            SwitchPref(R.string.settings_other_check_update, PreferenceManager.PREF_OTHER_CHECK_UPDATE, false, preference)
            SwitchPref(R.string.settings_check_update, PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true, preference)
            SwitchPref(R.string.settings_other_firebase_event, PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true, preference)
            ActionPref(R.string.settings_other_launch) {
                onChoice(
                    R.string.settings_other_launch,
                    launchItems,
                    preference.getInt(PreferenceManager.PREF_OTHER_LAUNCH, PreferenceManager.HOME_EXPLORE),
                    0
                )
            }
            ActionPref(R.string.settings_other_night_alpha) {
                onSlider(
                    R.string.settings_other_night_alpha,
                    100,
                    200,
                    preference.getInt(PreferenceManager.PREF_OTHER_NIGHT_ALPHA, 0xB0),
                    7
                )
            }
            ActionPref(R.string.settings_other_storage, onClick = onStorage)
            ActionPref(R.string.settings_other_clear_cache, showDivider = false, onClick = onClearCache)
        }
    }
}
