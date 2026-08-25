package com.haleydu.cimoc.ui.main
import com.haleydu.cimoc.ui.common.BaseActivity
import com.haleydu.cimoc.ui.settings.SettingsActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import android.os.Bundle
import androidx.core.os.bundleOf
import com.haleydu.cimoc.ui.detail.DetailFragment
import com.haleydu.cimoc.ui.search.ResultActivity
import com.haleydu.cimoc.ui.search.ResultFragment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.core.Update
import com.haleydu.cimoc.databinding.ActivityMainBinding
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.explore.ExploreFragment
import com.haleydu.cimoc.ui.library.LibraryFragment
import com.haleydu.cimoc.ui.main.ProfileFragment
import com.haleydu.cimoc.ui.search.SearchFragment
import com.haleydu.cimoc.ui.common.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import com.haleydu.cimoc.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity(), DialogCaller {

    private val vm: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val fragmentMap = LinkedHashMap<Int, BaseFragment>()
    private var currentFragment: BaseFragment? = null
    private var currentNavId = R.id.nav_library
    private var exitTime = 0L
    private val updater = Update()
    private var versionName: String? = null
    private var content: String? = null
    private var updateUrl: String? = null
    private var md5: String? = null
    private var versionCode = 0

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val extra = data.getIntArrayExtra(Extra.EXTRA_RESULT) ?: return@registerForActivityResult
        if (extra[0] == 1) {
            changeTheme(extra[1], extra[2], extra[3])
        }
        if (extra[4] == 1 && mNightMask != null) {
            mNightMask!!.setBackgroundColor(extra[5] shl 24)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        currentNavId = savedInstanceState?.getInt(STATE_NAV, R.id.nav_library) ?: R.id.nav_library
        super.onCreate(savedInstanceState)
    }

    override fun inflateContentView(): View {
        binding = ActivityMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initToolbar() {
        super.initToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
    }

    override fun applyWindowInsets() {
        val toolbar = mToolbar
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
                val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
                insets
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainBottomNav) { v, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainFragmentContainer) { v, insets ->
            val ime: Insets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav: Insets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                (ime.bottom - nav.bottom).coerceAtLeast(0)
            )
            insets
        }
    }

    override fun initView() {
        initFragments()
        binding.mainBottomNav.selectedItemId = currentNavId
        mToolbar?.title = titleFor(currentNavId)
        binding.mainBottomNav.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }
    }

    override fun initData() {
        vm.update.collectOnStart(this) { event ->
            when (event) {
                is MainViewModel.UpdateEvent.Ready -> onUpdateReady()
                is MainViewModel.UpdateEvent.GiteeReady -> onUpdateReady(
                    event.versionName,
                    event.content,
                    event.url,
                    event.versionCode,
                    event.md5
                )
            }
        }
        AppEventBus.observe(AppEvent.EVENT_SWITCH_NIGHT).collectOnStart(this) {
            onNightSwitch()
        }
        if (mPreference.getBoolean(PreferenceManager.PREF_UPDATE_APP_AUTO, true)) {
            val url = mPreference.getString(PreferenceManager.PREF_UPDATE_CURRENT_URL)
            if (url != null) {
                App.setUpdateCurrentUrl(url)
            }
            checkUpdate()
        }
        vm.getSourceBaseUrl()
        showAuthorNotice()
        showPermission()
        getMh50KeyIv()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_NAV, currentNavId)
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as App).builderProvider.clear()
        (application as App).gridRecycledPool.clear()
    }

    override fun onBackPressed() {
        if (binding.mainNavHost.visibility == View.VISIBLE) {
            val nav = navController()
            if (!nav.popBackStack() || nav.currentDestination?.id == R.id.navPlaceholder) {
                hideOverlay()
            }
            return
        }
        if (System.currentTimeMillis() - exitTime > 2000) {
            HintUtils.showToast(this, R.string.main_double_click)
            exitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_NOTICE -> mPreference.putBoolean(PreferenceManager.PREF_MAIN_NOTICE, true)
            DIALOG_REQUEST_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        0
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                (application as App).initRootDocumentFile()
                HintUtils.showToast(this, R.string.main_permission_success)
            } else {
                HintUtils.showToast(this, R.string.main_permission_fail)
            }
        }
    }

    fun openSettings() {
        settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    fun openSearch() {
        binding.mainBottomNav.selectedItemId = R.id.nav_search
    }

    fun openLibraryDownload() {
        binding.mainBottomNav.selectedItemId = R.id.nav_library
        (fragmentMap[R.id.nav_library] as? LibraryFragment)?.showDownload()
    }

    @JvmOverloads
    fun openDetail(
        id: Long?,
        source: Int,
        cid: String?,
        sharedView: View?,
        title: String? = null,
        cover: String? = null,
        author: String? = null
    ) {
        showOverlay()
        val args = Bundle()
        args.putLong(Extra.EXTRA_ID, id ?: -1L)
        args.putInt(Extra.EXTRA_SOURCE, source)
        args.putString(Extra.EXTRA_CID, cid)
        args.putString(Extra.EXTRA_TITLE, title)
        args.putString(Extra.EXTRA_COVER, cover)
        args.putString(Extra.EXTRA_AUTHOR, author)
        val dest = if (navController().currentDestination?.id == R.id.resultFragment) {
            R.id.action_result_to_detail
        } else {
            R.id.detailFragment
        }
        val coverView = sharedView?.findViewById<View>(R.id.item_grid_image)
        if (coverView != null) {
            val extras = FragmentNavigatorExtras(coverView to "comic_cover")
            navController().navigate(dest, args, null, extras)
        } else {
            navController().navigate(dest, args)
        }
    }

    fun openResult(keyword: String?, source: IntArray?, strict: Boolean, type: Int) {
        showOverlay()
        val args = Bundle()
        args.putString(Extra.EXTRA_KEYWORD, keyword)
        args.putIntArray(Extra.EXTRA_SOURCE_LIST, source)
        args.putBoolean(Extra.EXTRA_STRICT, strict)
        args.putInt(Extra.EXTRA_MODE, type)
        navController().navigate(R.id.resultFragment, args)
    }

    fun openExploreSource(source: Int) {
        showOverlay()
        val args = Bundle()
        args.putInt(Extra.EXTRA_SOURCE, source)
        navController().navigate(R.id.exploreSourceFragment, args)
    }

    private fun navController() =
        (supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment).navController

    private fun showOverlay() {
        binding.mainNavHost.visibility = View.VISIBLE
        binding.mainFragmentContainer.visibility = View.GONE
        binding.mainBottomNav.visibility = View.GONE
        mToolbar?.visibility = View.GONE
    }

    private fun hideOverlay() {
        binding.mainNavHost.visibility = View.GONE
        binding.mainFragmentContainer.visibility = View.VISIBLE
        binding.mainBottomNav.visibility = View.VISIBLE
        mToolbar?.visibility = View.VISIBLE
        mToolbar?.title = titleFor(currentNavId)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.nav_library)
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_main
    }

    override fun getLayoutView(): View {
        return binding.mainRoot
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    override fun useNightMask(): Boolean {
        return false
    }

    private fun initFragments() {
        val fm = supportFragmentManager
        val library = fm.findFragmentByTag(TAG_LIBRARY) as? BaseFragment ?: LibraryFragment()
        val discover = fm.findFragmentByTag(TAG_DISCOVER) as? BaseFragment ?: ExploreFragment()
        val search = fm.findFragmentByTag(TAG_SEARCH) as? BaseFragment ?: SearchFragment()
        val profile = fm.findFragmentByTag(TAG_PROFILE) as? BaseFragment ?: ProfileFragment()
        fragmentMap[R.id.nav_library] = library
        fragmentMap[R.id.nav_discover] = discover
        fragmentMap[R.id.nav_search] = search
        fragmentMap[R.id.nav_profile] = profile
        if (fm.findFragmentByTag(TAG_LIBRARY) == null) {
            val tx = fm.beginTransaction()
                .add(R.id.main_fragment_container, library, TAG_LIBRARY)
                .add(R.id.main_fragment_container, discover, TAG_DISCOVER)
                .hide(discover)
                .add(R.id.main_fragment_container, search, TAG_SEARCH)
                .hide(search)
                .add(R.id.main_fragment_container, profile, TAG_PROFILE)
                .hide(profile)
            if (currentNavId != R.id.nav_library) {
                tx.hide(library)
                tx.show(fragmentMap.getValue(currentNavId))
            }
            tx.commit()
        } else {
            val tx = fm.beginTransaction()
            fragmentMap.forEach { (id, fragment) ->
                if (id == currentNavId) tx.show(fragment) else tx.hide(fragment)
            }
            tx.commit()
        }
        currentFragment = fragmentMap[currentNavId]
    }

    private fun switchTab(itemId: Int) {
        val next = fragmentMap[itemId] ?: return
        if (currentFragment !== next) {
            val tx = supportFragmentManager.beginTransaction()
            currentFragment?.let { tx.hide(it) }
            tx.show(next)
            tx.commit()
            currentFragment = next
        }
        currentNavId = itemId
        mToolbar?.title = titleFor(itemId)
        invalidateOptionsMenu()
    }

    private fun titleFor(itemId: Int): String {
        return when (itemId) {
            R.id.nav_discover -> getString(R.string.nav_discover)
            R.id.nav_search -> getString(R.string.comic_search)
            R.id.nav_profile -> getString(R.string.nav_profile)
            else -> getString(R.string.nav_library)
        }
    }

    private fun changeTheme(@StyleRes theme: Int, @ColorRes primary: Int, @ColorRes accent: Int) {
        setTheme(theme)
        val primaryColor = ContextCompat.getColor(this, primary)
        mToolbar?.setBackgroundColor(primaryColor)
        binding.mainBottomNav.setBackgroundColor(primaryColor)
        fragmentMap.values.forEach { fragment ->
            (fragment as? ThemeResponsive)?.onThemeChange(primary, accent)
        }
    }

    private fun onUpdateReady() {
        HintUtils.showToast(this, R.string.main_ready_update)
    }

    private fun onUpdateReady(
        versionName: String,
        content: String,
        url: String,
        versionCode: Int,
        md5: String
    ) {
        this.versionName = versionName
        this.content = content
        this.updateUrl = url
        this.md5 = md5
        this.versionCode = versionCode
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true)) {
            updater.startUpdate(versionName, content, url, versionCode, md5)
        } else {
            HintUtils.showToast(this, R.string.main_ready_update)
        }
    }

    private fun showAuthorNotice() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FireBase_FirstOpenMsg", "Config params updated: ${task.result}")
                } else {
                    Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ")
                }
                val showMsg = remoteConfig.getString("first_open_msg")
                if (!mPreference.getBoolean(PreferenceManager.PREF_MAIN_NOTICE, false) ||
                    showMsg != mPreference.getString(PreferenceManager.PREF_MAIN_NOTICE_LAST, "")
                ) {
                    mPreference.putString(PreferenceManager.PREF_MAIN_NOTICE_LAST, showMsg)
                    MessageDialogFragment.newInstance(
                        R.string.main_notice,
                        showMsg,
                        false,
                        DIALOG_REQUEST_NOTICE
                    ).show(supportFragmentManager, null)
                }
            })
    }

    private fun getMh50KeyIv() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60 * 60)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FireBase_FirstOpenMsg", "Config params updated: ${task.result}")
                } else {
                    Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ")
                }
                val mh50Key = remoteConfig.getString("mh50_key_msg")
                val mh50Iv = remoteConfig.getString("mh50_iv_msg")
                if (mh50Key != mPreference.getString(
                        PreferenceManager.PREFERENCES_MH50_KEY_MSG,
                        "KA58ZAQ321oobbG8"
                    )
                ) {
                    mPreference.putString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, mh50Key)
                    Toast.makeText(this, "漫画堆key已更新", Toast.LENGTH_LONG).show()
                }
                if (mh50Iv != mPreference.getString(
                        PreferenceManager.PREFERENCES_MH50_IV_MSG,
                        "A1B2C3DEF1G321o8"
                    )
                ) {
                    mPreference.putString(PreferenceManager.PREFERENCES_MH50_IV_MSG, mh50Iv)
                    Toast.makeText(this, "漫画堆iv已更新", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun showPermission() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            MessageDialogFragment.newInstance(
                R.string.main_permission,
                R.string.main_permission_content,
                false,
                DIALOG_REQUEST_PERMISSION
            ).show(supportFragmentManager, null)
        }
    }

    private fun checkUpdate() {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            vm.checkGiteeUpdate(PackageInfoCompat.getLongVersionCode(info).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DIALOG_REQUEST_NOTICE = 0
        private const val DIALOG_REQUEST_PERMISSION = 1
        private const val STATE_NAV = "cimoc.state.NAV"
        private const val TAG_LIBRARY = "main_library"
        private const val TAG_DISCOVER = "main_discover"
        private const val TAG_SEARCH = "main_search"
        private const val TAG_PROFILE = "main_profile"
    }
}
