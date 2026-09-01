package com.haleydu.cimoc.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.ui.settings.AboutActivity
import com.haleydu.cimoc.ui.settings.BackupActivity
import com.haleydu.cimoc.ui.settings.SettingsActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestStorageIfNeeded()
        val repos = createCimocRepositories(
            comicManager,
            chapterManager,
            imageUrlManager,
            sourceManager,
            mangaService,
            sourceHealthManager,
            sourceCatalogRefresher
        )
        val dark = preferenceManager.getBoolean(PreferenceManager.PREF_NIGHT, false)
        setContent {
            CimocApp(
                repos = repos,
                actions = CimocActions(
                    openSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    openAbout = { startActivity(Intent(this, AboutActivity::class.java)) },
                    openBackup = { startActivity(Intent(this, BackupActivity::class.java)) },
                    openClassic = { startActivity(Intent(this, MainActivity::class.java)) }
                ),
                dark = dark
            )
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
}
