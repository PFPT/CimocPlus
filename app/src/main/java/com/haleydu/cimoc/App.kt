package com.haleydu.cimoc

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.component.AppGetter
import com.haleydu.cimoc.core.Storage
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.fresco.ControllerBuilderProvider
import com.haleydu.cimoc.helper.UpdateHelper
import com.haleydu.cimoc.misc.ActivityLifecycle
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.ui.common.GridAdapter
import com.haleydu.cimoc.utils.DocumentUtils
import com.haleydu.cimoc.utils.FrescoUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class App : MultiDexApplication(), AppGetter, Thread.UncaughtExceptionHandler {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var sourceConfigManager: SourceConfigManager

    @Inject
    lateinit var database: CimocDatabase

    @Inject
    lateinit var httpClient: OkHttpClient

    private var mDocumentFile: DocumentFile? = null
    private var mBuilderProvider: ControllerBuilderProvider? = null
    private var mRecycledPool: RecyclerView.RecycledViewPool? = null
    private lateinit var mActivityLifecycle: ActivityLifecycle

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(this)
        mActivityLifecycle = ActivityLifecycle()
        registerActivityLifecycleCallbacks(mActivityLifecycle)
        mPreferenceManager = preferenceManager
        val night = mPreferenceManager!!.getBoolean(PreferenceManager.PREF_NIGHT, false)
        AppCompatDelegate.setDefaultNightMode(
            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        UpdateHelper.update(mPreferenceManager!!, sourceConfigManager, database.sourceDao())
        FrescoUtils.init(this, 250)
        initPixels()

        manager_wifi = getSystemService(WIFI_SERVICE) as WifiManager
        mApp = this
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
                sActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val sb = StringBuilder()
        sb.append("MODEL: ").append(Build.MODEL).append('\n')
        sb.append("SDK: ").append(Build.VERSION.SDK_INT).append('\n')
        sb.append("RELEASE: ").append(Build.VERSION.RELEASE).append('\n')
        sb.append('\n').append(e.localizedMessage).append('\n')
        for (element in e.stackTrace) {
            sb.append('\n')
            sb.append(element.toString())
        }
        try {
            val doc = documentFile
            val dir = DocumentUtils.getOrCreateSubDirectory(doc, "log")
            val file = DocumentUtils.getOrCreateFile(dir, StringUtils.getDateStringWithSuffix("log"))
            DocumentUtils.writeStringToFile(contentResolver, file, sb.toString())
        } catch (_: Exception) {
        }
        mActivityLifecycle.clear()
        System.exit(1)
    }

    override val appInstance: App
        get() = this

    private fun initPixels() {
        val metrics: DisplayMetrics = resources.displayMetrics
        mWidthPixels = metrics.widthPixels
        mHeightPixels = metrics.heightPixels
        mCoverWidthPixels = mWidthPixels / 3
        mCoverHeightPixels = mCoverWidthPixels * 4 / 3
        mLargePixels = 3 * metrics.widthPixels * metrics.heightPixels
    }

    fun initRootDocumentFile() {
        val uri = mPreferenceManager!!.getString(PreferenceManager.PREF_OTHER_STORAGE)
        mDocumentFile = Storage.initRoot(this, uri)
    }

    val documentFile: DocumentFile?
        get() {
            if (mDocumentFile == null) {
                initRootDocumentFile()
            }
            return mDocumentFile
        }

    val gridRecycledPool: RecyclerView.RecycledViewPool
        get() {
            if (mRecycledPool == null) {
                mRecycledPool = RecyclerView.RecycledViewPool()
                mRecycledPool!!.setMaxRecycledViews(GridAdapter.TYPE_GRID, 20)
            }
            return mRecycledPool!!
        }

    val builderProvider: ControllerBuilderProvider
        get() {
            if (mBuilderProvider == null) {
                mBuilderProvider = ControllerBuilderProvider(
                    applicationContext,
                    sourceManager.HeaderGetter(), true, httpClient
                )
            }
            return mBuilderProvider!!
        }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        @JvmField
        var mWidthPixels: Int = 0

        @JvmField
        var mHeightPixels: Int = 0

        @JvmField
        var mCoverWidthPixels: Int = 0

        @JvmField
        var mCoverHeightPixels: Int = 0

        @JvmField
        var mLargePixels: Int = 0

        private var mPreferenceManager: PreferenceManager? = null
        private var manager_wifi: WifiManager? = null
        private var mApp: App? = null
        private var sActivity: Activity? = null
        private var UPDATE_CURRENT_URL = "https://api.github.com/repos/Haleydu/Cimoc/releases/latest"

        @JvmStatic
        fun getApp(): App = mApp!!

        @JvmStatic
        fun getAppContext(): Context = mApp!!

        @JvmStatic
        fun getAppResources(): Resources = mApp!!.resources

        @JvmStatic
        fun getActivity(): Activity = sActivity!!

        @JvmStatic
        fun getManager_wifi(): WifiManager = manager_wifi!!

        @JvmStatic
        fun setUpdateCurrentUrl(updateCurrentUrl: String) {
            UPDATE_CURRENT_URL = updateCurrentUrl
        }

        @JvmStatic
        fun getUpdateCurrentUrl(): String = UPDATE_CURRENT_URL
    }
}
