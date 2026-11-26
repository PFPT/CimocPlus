package com.haleydu.cimoc.di

import android.content.Context
import com.haleydu.cimoc.app.createCimocRepositories
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.SourceProbe
import com.haleydu.cimoc.data.SourceRuleManager
import com.haleydu.cimoc.data.TagManager
import com.haleydu.cimoc.data.TagRefManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.network.NetworkPolicy
import com.haleydu.cimoc.shared.repo.CimocRepositories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CimocModule {

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager =
        PreferenceManager(context)

    @Provides
    @Singleton
    fun provideNetworkPolicy(preferenceManager: PreferenceManager): NetworkPolicy =
        NetworkPolicy {
            preferenceManager.getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)
        }

    @Provides
    @Singleton
    fun provideRepositories(
        @ApplicationContext context: Context,
        comicManager: ComicManager,
        chapterManager: ChapterManager,
        imageUrlManager: ImageUrlManager,
        sourceManager: SourceManager,
        mangaService: MangaService,
        sourceHealthManager: SourceHealthManager,
        sourceProbe: SourceProbe,
        sourceRuleManager: SourceRuleManager,
        sourceCatalogRefresher: SourceCatalogRefresher,
        taskManager: TaskManager,
        preferenceManager: PreferenceManager,
        tagManager: TagManager,
        tagRefManager: TagRefManager
    ): CimocRepositories = createCimocRepositories(
        context,
        comicManager,
        chapterManager,
        imageUrlManager,
        sourceManager,
        mangaService,
        sourceHealthManager,
        sourceProbe,
        sourceRuleManager,
        sourceCatalogRefresher,
        taskManager,
        preferenceManager,
        tagManager,
        tagRefManager
    )
}
