package com.haleydu.cimoc;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.multidex.MultiDex;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.haleydu.cimoc.utils.FrescoUtils;
import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.core.Storage;
import com.haleydu.cimoc.fresco.ControllerBuilderProvider;
import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.helper.UpdateHelper;
import com.haleydu.cimoc.data.PreferenceManager;
import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.data.SourceManager;
import com.haleydu.cimoc.misc.ActivityLifecycle;
import com.haleydu.cimoc.saf.DocumentFile;
import com.haleydu.cimoc.ui.common.GridAdapter;
import com.haleydu.cimoc.utils.DocumentUtils;
import com.haleydu.cimoc.utils.StringUtils;

import okhttp3.OkHttpClient;

import androidx.multidex.MultiDexApplication;

import dagger.hilt.android.HiltAndroidApp;

import javax.inject.Inject;

@HiltAndroidApp
public class App extends MultiDexApplication implements AppGetter, Thread.UncaughtExceptionHandler {

    public static int mWidthPixels;
    public static int mHeightPixels;
    public static int mCoverWidthPixels;
    public static int mCoverHeightPixels;
    public static int mLargePixels;

    private DocumentFile mDocumentFile;
    private static PreferenceManager mPreferenceManager;
    private ControllerBuilderProvider mBuilderProvider;
    
    private RecyclerView.RecycledViewPool mRecycledPool;
    private ActivityLifecycle mActivityLifecycle;


    private static WifiManager manager_wifi;
    private static App mApp;
    private static Activity sActivity;

    @Inject
    PreferenceManager preferenceManager;
    @Inject
    SourceManager sourceManager;
    @Inject
    SourceConfigManager sourceConfigManager;
    @Inject
    CimocDatabase database;
    @Inject
    OkHttpClient httpClient;

    // 默认Github源
    private static String UPDATE_CURRENT_URL = "https://api.github.com/repos/Haleydu/Cimoc/releases/latest";

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mActivityLifecycle = new ActivityLifecycle();
        registerActivityLifecycleCallbacks(mActivityLifecycle);
        mPreferenceManager = preferenceManager;
        boolean night = mPreferenceManager.getBoolean(PreferenceManager.PREF_NIGHT, false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                night ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        UpdateHelper.update(mPreferenceManager, sourceConfigManager, database.sourceDao());
        FrescoUtils.init(this, 250);
        initPixels();

        manager_wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //获取栈顶Activity以及当前App上下文
        mApp = this;
        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//                Log.d("ActivityLifecycle:",activity+"onActivityCreated");
            }

            @Override
            public void onActivityStarted(Activity activity) {
//                Log.d("ActivityLifecycle:",activity+"onActivityStarted");
                sActivity = activity;

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append("MODEL: ").append(Build.MODEL).append('\n');
        sb.append("SDK: ").append(Build.VERSION.SDK_INT).append('\n');
        sb.append("RELEASE: ").append(Build.VERSION.RELEASE).append('\n');
        sb.append('\n').append(e.getLocalizedMessage()).append('\n');
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append('\n');
            sb.append(element.toString());
        }
        try {
            DocumentFile doc = getDocumentFile();
            DocumentFile dir = DocumentUtils.getOrCreateSubDirectory(doc, "log");
            DocumentFile file = DocumentUtils.getOrCreateFile(dir, StringUtils.getDateStringWithSuffix("log"));
            DocumentUtils.writeStringToFile(getContentResolver(), file, sb.toString());
        } catch (Exception ex) {
        }
        mActivityLifecycle.clear();
        System.exit(1);
    }

    @Override
    public App getAppInstance() {
        return this;
    }

    public static Context getAppContext() {
        return mApp;
    }

    public static Resources getAppResources() {
        return mApp.getResources();
    }

    public static Activity getActivity() {
        return sActivity;
    }

    public static WifiManager getManager_wifi() {
        return manager_wifi;
    }

    private void initPixels() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        mWidthPixels = metrics.widthPixels;
        mHeightPixels = metrics.heightPixels;
        mCoverWidthPixels = mWidthPixels / 3;
        mCoverHeightPixels = mCoverWidthPixels * 4 / 3;
        mLargePixels = 3 * metrics.widthPixels * metrics.heightPixels;
    }

    public void initRootDocumentFile() {
        String uri = mPreferenceManager.getString(PreferenceManager.PREF_OTHER_STORAGE);
        mDocumentFile = Storage.initRoot(this, uri);
    }

    public DocumentFile getDocumentFile() {
        if (mDocumentFile == null) {
            initRootDocumentFile();
        }
        return mDocumentFile;
    }

    public RecyclerView.RecycledViewPool getGridRecycledPool() {
        if (mRecycledPool == null) {
            mRecycledPool = new RecyclerView.RecycledViewPool();
            mRecycledPool.setMaxRecycledViews(GridAdapter.TYPE_GRID, 20);
        }
        return mRecycledPool;
    }

    public ControllerBuilderProvider getBuilderProvider() {
        if (mBuilderProvider == null) {
            mBuilderProvider = new ControllerBuilderProvider(getApplicationContext(),
                    sourceManager.new HeaderGetter(), true, httpClient);
        }
        return mBuilderProvider;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static void setUpdateCurrentUrl(String updateCurrentUrl) {
        UPDATE_CURRENT_URL = updateCurrentUrl;
    }

    public static String getUpdateCurrentUrl() {
        return UPDATE_CURRENT_URL;
    }
}
