package com.haleydu.cimoc.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.databinding.ActivityMainBinding;
import com.haleydu.cimoc.databinding.CustomDrawerHeaderBinding;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.component.ThemeResponsive;
import com.haleydu.cimoc.core.Update;
import com.haleydu.cimoc.fresco.ControllerBuilderProvider;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.MiniComic;
import com.haleydu.cimoc.event.AppEventBus;
import com.haleydu.cimoc.event.AppEvent;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.fragment.BaseFragment;
import com.haleydu.cimoc.ui.fragment.ComicFragment;
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment;
import com.haleydu.cimoc.ui.fragment.recyclerview.SourceFragment;
import com.haleydu.cimoc.utils.HintUtils;
import dagger.hilt.android.AndroidEntryPoint;
import com.haleydu.cimoc.utils.PermissionUtils;

import javax.inject.Inject;

import okhttp3.OkHttpClient;



/**
 * Created by Hiroshi on 2016/7/1.
 * fixed by Haleydu on 2020/8/8.
 */
@AndroidEntryPoint
public class MainActivity extends BaseActivity implements DialogCaller, NavigationView.OnNavigationItemSelectedListener {

    private static final int DIALOG_REQUEST_NOTICE = 0;
    private static final int DIALOG_REQUEST_PERMISSION = 1;
    //private static final int DIALOG_REQUEST_LOGOUT = 2;

    private static final int REQUEST_ACTIVITY_SETTINGS = 0;

    private static final int FRAGMENT_NUM = 3;

    DrawerLayout mDrawerLayout;
    NavigationView mNavigationView;
    FrameLayout mFrameLayout;
    private ActivityMainBinding binding;

    private TextView mLastText;
    private SimpleDraweeView mDraweeView;
    private ControllerBuilderProvider mControllerBuilderProvider;

    @Inject
    OkHttpClient httpClient;

    private MainViewModel vm;
    private ActionBarDrawerToggle mDrawerToggle;
    private long mExitTime = 0;
    private long mLastId = -1;
    private int mLastSource = -1;
    private String mLastCid;

    private int mCheckItem;
    private SparseArray<BaseFragment> mFragmentArray;
    private BaseFragment mCurrentFragment;
    private boolean night;

    private Update update = new Update();
    private String versionName,content,mUrl,md5;
    private int versionCode;
    //auth0
//    private Auth0 auth0;

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(MainViewModel.class);
    }

    @Override
    protected void initView() {
        initDrawerToggle();
        initNavigation();
        initFragment();
    }

//    private void login() {
//        HintUtils.showToast(MainActivity.this, R.string.user_login_tips);
//        WebAuthProvider.init(auth0)
//            .withScheme("demo")
//            .withScope("openid profile email")
//            .withAudience(String.format("https://%s/userinfo", getString(R.string.com_auth0_domain)))
//            .start(MainActivity.this, new AuthCallback() {
//                @Override
//                public void onFailure(@NonNull final Dialog dialog) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            dialog.show();
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailure(final AuthenticationException exception) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
//                            HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//                        }
//                    });
//                }
//
//                @Override
//                public void onSuccess(@NonNull final Credentials credentials) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            Toast.makeText(MainActivity.this, "Logged in: " + credentials.getAccessToken(), Toast.LENGTH_LONG).show();
//                            HintUtils.showToast(MainActivity.this, R.string.user_login_sucess);
//                            mPreference.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, credentials.getAccessToken());
//                            getUesrInfo();
//                        }
//                    });
//                }
//            });
//    }
//
//    private void logoutShowDialog(){
//        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.user_login_logout,
//            R.string.user_login_logout_tips, true, DIALOG_REQUEST_LOGOUT);
//        fragment.show(getSupportFragmentManager(), null);
//    }
//
//    private void logout() {
//        HintUtils.showToast(MainActivity.this, R.string.user_login_logout_sucess);
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_EMAIL, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_NAME, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_ID, "");
//    }
//
//    private void loginout() {
//        if (mPreference.getString(PreferenceManager.PREFERENCES_USER_ID, "") == "") {
//            login();
//        } else {
//            logoutShowDialog();
//        }
//    }

    @Override
    protected void initData() {
        FlowExtKt.collectOnStart(vm.getLast(), this, last ->
                onLastLoadSuccess(last.getId(), last.getSource(), last.getCid(), last.getTitle(), last.getCover()));
        FlowExtKt.collectOnStart(vm.getLastFail(), this, unit -> onLastLoadFail());
        FlowExtKt.collectOnStart(vm.getUpdate(), this, event -> {
            if (event instanceof MainViewModel.UpdateEvent.Ready) {
                onUpdateReady();
            } else if (event instanceof MainViewModel.UpdateEvent.GiteeReady) {
                MainViewModel.UpdateEvent.GiteeReady gitee = (MainViewModel.UpdateEvent.GiteeReady) event;
                onUpdateReady(gitee.getVersionName(), gitee.getContent(), gitee.getUrl(),
                        gitee.getVersionCode(), gitee.getMd5());
            }
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_READ), this, event -> {
            MiniComic comic = (MiniComic) event.getData();
            onLastChange(comic.getId(), comic.getSource(), comic.getCid(), comic.getTitle(), comic.getCover());
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_SWITCH_NIGHT), this, event -> onNightSwitch());
        vm.loadLast();

        //检查App更新
        String updateUrl;
        if (mPreference.getBoolean(PreferenceManager.PREF_UPDATE_APP_AUTO, true)) {
            if ((updateUrl = mPreference.getString(PreferenceManager.PREF_UPDATE_CURRENT_URL)) != null) {
                App.setUpdateCurrentUrl(updateUrl);
            }
            checkUpdate();
        }
        vm.getSourceBaseUrl();

        showAuthorNotice();
        showPermission();
        getMh50KeyIv();

    }


//    public void getUesrInfo() {
//        String accessTocken = mPreference.getString(PreferenceManager.PREFERENCES_USER_TOCKEN, null);
//        if (accessTocken != null) {
//            AuthenticationAPIClient authentication = new AuthenticationAPIClient(auth0);
//            authentication
//                .userInfo(accessTocken)
//                .start(new BaseCallback<UserProfile, AuthenticationException>() {
//                    @Override
//                    public void onSuccess(UserProfile information) {
//                        //user information received
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_EMAIL, information.getEmail());
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_NAME, information.getName());
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_ID, (String) information.getExtraInfo().get("sub"));
//                    }
//
//                    @Override
//                    public void onFailure(AuthenticationException error) {
//                        //user information request failed
//                        HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//                    }
//                });
//        } else {
//            HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//        }
//    }

//    @Override
//    protected void initUser() {
//        //auth0
//        auth0 = new Auth0(this);
//        auth0.setOIDCConformant(true);
//    }

    private void initDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, 0, 0) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (refreshCurrentFragment()) {
                    getSupportFragmentManager().beginTransaction().show(mCurrentFragment).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, mCurrentFragment).commit();
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void initNavigation() {
        night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false);
        mNavigationView.getMenu().findItem(R.id.drawer_night).setTitle(night ? R.string.drawer_light : R.string.drawer_night);
        mNavigationView.setNavigationItemSelectedListener(this);
        CustomDrawerHeaderBinding header = CustomDrawerHeaderBinding.bind(mNavigationView.getHeaderView(0));
        mLastText = header.drawerLastTitle;
        mDraweeView = header.drawerLastCover;

        mLastText.setOnClickListener(v -> {
            if (vm.checkLocal(mLastId)) {
                Intent intent = TaskActivity.createIntent(MainActivity.this, mLastId);
                startActivity(intent);
            } else if (mLastSource != -1 && mLastCid != null) {
                Intent intent = DetailActivity.createIntent(MainActivity.this, null, mLastSource, mLastCid);
                startActivity(intent);
            } else {
                HintUtils.showToast(MainActivity.this, R.string.common_execute_fail);
            }
        });
        mControllerBuilderProvider = new ControllerBuilderProvider(this,
                vm.headerGetter(), false, httpClient);
    }

    private void initFragment() {
        int home = mPreference.getInt(PreferenceManager.PREF_OTHER_LAUNCH, PreferenceManager.HOME_FAVORITE);
        switch (home) {
            default:
            case PreferenceManager.HOME_FAVORITE:
            case PreferenceManager.HOME_HISTORY:
            case PreferenceManager.HOME_DOWNLOAD:
                mCheckItem = R.id.drawer_comic;
                break;
            case PreferenceManager.HOME_SOURCE:
                mCheckItem = R.id.drawer_source;
                break;
//            case PreferenceManager.HOME_TAG:
//                mCheckItem = R.id.drawer_tag;
//                break;
        }
        mNavigationView.setCheckedItem(mCheckItem);
        mFragmentArray = new SparseArray<>(FRAGMENT_NUM);
        refreshCurrentFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, mCurrentFragment).commit();
    }

    private boolean refreshCurrentFragment() {
        mCurrentFragment = mFragmentArray.get(mCheckItem);
        if (mCurrentFragment == null) {
            switch (mCheckItem) {
                case R.id.drawer_comic:
                    mCurrentFragment = new ComicFragment();
                    break;
                case R.id.drawer_source:
                    mCurrentFragment = new SourceFragment();
                    break;
//                case R.id.drawer_tag:
//                    mCurrentFragment = new TagFragment();
//                    break;
            }
            mFragmentArray.put(mCheckItem, mCurrentFragment);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mControllerBuilderProvider.clear();
        ((App) getApplication()).getBuilderProvider().clear();
        ((App) getApplication()).getGridRecycledPool().clear();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (System.currentTimeMillis() - mExitTime > 2000) {
            HintUtils.showToast(this, R.string.main_double_click);
            mExitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != mCheckItem) {
            switch (itemId) {
                case R.id.drawer_comic:
                case R.id.drawer_source:
//                case R.id.drawer_tag:
                    mCheckItem = itemId;
                    getSupportFragmentManager().beginTransaction().hide(mCurrentFragment).commit();
                    if (mToolbar != null) {
                        mToolbar.setTitle(item.getTitle().toString());
                    }
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    break;
                case R.id.drawer_comiclist:
                    Intent intentBaidu = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_comiclist_url)));
                    try {
                        startActivity(intentBaidu);
                    } catch (Exception e) {
                        showSnackbar(R.string.about_resource_fail);
                    }
                    break;
                case R.id.drawer_comicUpdate:
                    update.startUpdate(versionName, content, mUrl, versionCode, md5);
                    break;
                case R.id.drawer_night:
                    onNightSwitch();
                    mPreference.putBoolean(PreferenceManager.PREF_NIGHT, night);
                    break;
                case R.id.drawer_settings:
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), REQUEST_ACTIVITY_SETTINGS);
                    break;
                case R.id.drawer_about:
                    startActivity(new Intent(MainActivity.this, AboutActivity.class));
                    break;
                case R.id.drawer_backup:
                    startActivity(new Intent(MainActivity.this, BackupActivity.class));
                    break;
//                case R.id.user_info:
//                    loginout();
//                    break;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCurrentFragment.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ACTIVITY_SETTINGS:
                    int[] result = data.getIntArrayExtra(Extra.EXTRA_RESULT);
                    if (result[0] == 1) {
                        changeTheme(result[1], result[2], result[3]);
                    }
                    if (result[4] == 1 && mNightMask != null) {
                        mNightMask.setBackgroundColor(result[5] << 24);
                    }
                    break;
            }
        }
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_NOTICE:
                mPreference.putBoolean(PreferenceManager.PREF_MAIN_NOTICE, true);
                //showPermission();
                break;
            case DIALOG_REQUEST_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 0);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                }
                break;
//            case DIALOG_REQUEST_LOGOUT:
//                logout();
//                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ((App) getApplication()).initRootDocumentFile();
                    HintUtils.showToast(this, R.string.main_permission_success);
                } else {
                    HintUtils.showToast(this, R.string.main_permission_fail);
                }
                break;
        }
    }

    @Override
    public void onNightSwitch() {
        night = !night;
        mNavigationView.getMenu().findItem(R.id.drawer_night).setTitle(night ? R.string.drawer_light : R.string.drawer_night);
        if (mNightMask != null) {
            mNightMask.setVisibility(night ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void onUpdateReady() {
        HintUtils.showToast(this, R.string.main_ready_update);
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true)){
            mNavigationView.getMenu().findItem(R.id.drawer_comicUpdate).setVisible(true);
        }
//        Update.update(this);
    }

    public void onUpdateReady(String versionName, String content, String mUrl, int versionCode, String md5) {
        this.versionName = versionName;
        this.content = content;
        this.mUrl = mUrl;
        this.md5 = md5;
        this.versionCode = versionCode;
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true)) {
            mNavigationView.getMenu().findItem(R.id.drawer_comicUpdate).setVisible(true);
            update.startUpdate(versionName, content, mUrl, versionCode, md5);
        }else {
            HintUtils.showToast(this, R.string.main_ready_update);
        }
    }

    public void onLastLoadSuccess(long id, int source, String cid, String title, String cover) {
        onLastChange(id, source, cid, title, cover);
    }

    public void onLastLoadFail() {
        HintUtils.showToast(this, R.string.main_last_read_fail);
    }

    public void onLastChange(long id, int source, String cid, String title, String cover) {
        mLastId = id;
        mLastSource = source;
        mLastCid = cid;
        mLastText.setText(title);
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(cover))
                .setResizeOptions(new ResizeOptions(App.mWidthPixels, App.mHeightPixels))
                .build();
        DraweeController controller = mControllerBuilderProvider.get(source)
                .setOldController(mDraweeView.getController())
                .setImageRequest(request)
                .build();
        mDraweeView.setController(controller);
    }

    private void changeTheme(@StyleRes int theme, @ColorRes int primary, @ColorRes int accent) {
        setTheme(theme);
        ColorStateList itemList = new ColorStateList(new int[][]{{-android.R.attr.state_checked},
                {android.R.attr.state_checked}},
                new int[]{Color.BLACK, ContextCompat.getColor(this, accent)});
        mNavigationView.setItemTextColor(itemList);
        ColorStateList iconList = new ColorStateList(new int[][]{{-android.R.attr.state_checked},
                {android.R.attr.state_checked}},
                new int[]{0x8A000000, ContextCompat.getColor(this, accent)});
        mNavigationView.setItemIconTintList(iconList);
        mNavigationView.getHeaderView(0).setBackgroundColor(ContextCompat.getColor(this, primary));
        if (mToolbar != null) {
            mToolbar.setBackgroundColor(ContextCompat.getColor(this, primary));
        }

        for (int i = 0; i < mFragmentArray.size(); ++i) {
            ((ThemeResponsive) mFragmentArray.valueAt(i)).onThemeChange(primary, accent);
        }
    }

    private void showAuthorNotice() {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("FireBase_FirstOpenMsg", "Config params updated: " + updated);
                        } else {
                            Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ");
                        }

                        String showMsg = mFirebaseRemoteConfig.getString("first_open_msg");
                        if (!mPreference.getBoolean(PreferenceManager.PREF_MAIN_NOTICE, false)
                                || showMsg.compareTo(mPreference.getString(PreferenceManager.PREF_MAIN_NOTICE_LAST, "")) != 0) {
                            mPreference.putString(PreferenceManager.PREF_MAIN_NOTICE_LAST, showMsg);
                            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.main_notice,
                                    showMsg, false, DIALOG_REQUEST_NOTICE);
                            fragment.show(getSupportFragmentManager(), null);
                        }
                    }
                });
    }

    private void getMh50KeyIv() {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60*60)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("FireBase_FirstOpenMsg", "Config params updated: " + updated);
                        } else {
                            Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ");
                        }

                        String mh50_key = mFirebaseRemoteConfig.getString("mh50_key_msg");
                        String mh50_iv = mFirebaseRemoteConfig.getString("mh50_iv_msg");

                        if (!mh50_key.equals(mPreference.getString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, "KA58ZAQ321oobbG8"))){
                            mPreference.putString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, mh50_key);
                            Toast.makeText(MainActivity.this,"漫画堆key已更新",Toast.LENGTH_LONG).show();
                        }
                        if (!mh50_iv.equals(mPreference.getString(PreferenceManager.PREFERENCES_MH50_IV_MSG, "A1B2C3DEF1G321o8"))){
                            mPreference.putString(PreferenceManager.PREFERENCES_MH50_IV_MSG, mh50_iv);
                            Toast.makeText(MainActivity.this,"漫画堆iv已更新",Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showPermission() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.main_permission,
                    R.string.main_permission_content, false, DIALOG_REQUEST_PERMISSION);
            fragment.show(getSupportFragmentManager(), null);
        }
    }

    private void checkUpdate() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            vm.checkGiteeUpdate(info.versionCode);
            //vm.checkUpdate(info.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getDefaultTitle() {
        int home = mPreference.getInt(PreferenceManager.PREF_OTHER_LAUNCH, PreferenceManager.HOME_FAVORITE);
        switch (home) {
            default:
            case PreferenceManager.HOME_FAVORITE:
            case PreferenceManager.HOME_HISTORY:
            case PreferenceManager.HOME_DOWNLOAD:
            case PreferenceManager.HOME_LOCAL:
                return getString(R.string.drawer_comic);
            case PreferenceManager.HOME_SOURCE:
                return getString(R.string.drawer_source);
//            case PreferenceManager.HOME_TAG:
//                return getString(R.string.drawer_tag);
        }
    }

    @Override
    protected View inflateContentView() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected View getLayoutView() {
        return mDrawerLayout;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        mDrawerLayout = binding.mainLayout;
        mNavigationView = binding.mainNavigationView;
        mFrameLayout = binding.mainFragmentContainer;
    }

}
