package com.haleydu.cimoc.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.global.Extra
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CimocComposeActivity : AppCompatActivity() {

    @Inject lateinit var comicManager: ComicManager
    @Inject lateinit var chapterManager: ChapterManager
    @Inject lateinit var imageUrlManager: ImageUrlManager
    @Inject lateinit var sourceManager: SourceManager
    @Inject lateinit var mangaService: MangaService
    @Inject lateinit var sourceHealthManager: SourceHealthManager
    @Inject lateinit var sourceCatalogRefresher: SourceCatalogRefresher
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var taskManager: TaskManager
    @Inject lateinit var imageLoader: ImageLoader

    private var backHandler: () -> Boolean = { false }
    private var readerVolume: ((Boolean) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SingletonImageLoader.setSafe { imageLoader }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestStorageIfNeeded()
        val repos = createCimocRepositories(
            this,
            comicManager,
            chapterManager,
            imageUrlManager,
            sourceManager,
            mangaService,
            sourceHealthManager,
            sourceCatalogRefresher,
            taskManager,
            preferenceManager
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!backHandler()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        val start = startScreen()
        val dark = repos.settings.bool(com.haleydu.cimoc.shared.model.PrefKeys.NIGHT)
        setContent {
            CimocApp(
                repos = repos,
                actions = CimocActions(
                    bindBack = { backHandler = it },
                    bindVolume = { readerVolume = it },
                    onNightChanged = { night ->
                        repos.settings.setBool(com.haleydu.cimoc.shared.model.PrefKeys.NIGHT, night)
                        AppCompatDelegate.setDefaultNightMode(
                            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    },
                    openUrl = { url ->
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    setImmersive = { applyImmersive(it) },
                    setKeepScreenOn = { applyKeepScreenOn(it) }
                ),
                dark = dark,
                start = start
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val handled = readerVolume?.invoke(keyCode == KeyEvent.KEYCODE_VOLUME_UP) ?: false
            if (handled) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun applyImmersive(on: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applyKeepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startScreen(): Screen {
        val source = intent.getIntExtra(Extra.EXTRA_SOURCE, -1)
        val cid = intent.getStringExtra(Extra.EXTRA_CID)
        return if (source >= 0 && !cid.isNullOrEmpty()) {
            Screen.Detail(
                id = intent.getLongExtra(Extra.EXTRA_ID, -1L),
                source = source,
                cid = cid,
                title = intent.getStringExtra(Extra.EXTRA_TITLE),
                cover = intent.getStringExtra(Extra.EXTRA_COVER),
                author = intent.getStringExtra(Extra.EXTRA_AUTHOR)
            )
        } else {
            Screen.Library
        }
    }

    private fun requestStorageIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

    companion object {
        fun detailIntent(from: android.content.Context, source: Int, cid: String): Intent {
            return Intent(from, CimocComposeActivity::class.java).apply {
                putExtra(Extra.EXTRA_SOURCE, source)
                putExtra(Extra.EXTRA_CID, cid)
            }
        }
    }
}
