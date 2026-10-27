package com.haleydu.cimoc.app.di

import com.haleydu.cimoc.shared.repo.CimocRepositories
import com.haleydu.cimoc.shared.vm.DetailViewModel
import com.haleydu.cimoc.shared.vm.DownloadTasksViewModel
import com.haleydu.cimoc.shared.vm.ExploreViewModel
import com.haleydu.cimoc.shared.vm.LibraryViewModel
import com.haleydu.cimoc.shared.vm.RankViewModel
import com.haleydu.cimoc.shared.vm.ReaderViewModel
import com.haleydu.cimoc.shared.vm.SearchViewModel
import com.haleydu.cimoc.shared.vm.SettingsViewModel
import com.haleydu.cimoc.shared.vm.SourceHitsViewModel
import com.haleydu.cimoc.shared.vm.SourceListViewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

fun startCimocKoin(repos: CimocRepositories) {
    if (GlobalContext.getOrNull() != null) {
        stopKoin()
    }
    startKoin {
        modules(
            module {
                single { repos }
                single { LibraryViewModel(repos.comics, repos.sources, repos.manga, repos.tags) }
                single { SearchViewModel(repos.sources, repos.manga, repos.settings) }
                single { SourceListViewModel(repos.sources) }
                single { ExploreViewModel(repos.explore, repos.sources) }
                single { SettingsViewModel(repos.settings) }
                single { ReaderViewModel(repos.comics, repos.manga, repos.settings) }
                factory { DetailViewModel(repos.comics, repos.manga, repos.sources, repos.downloads, repos.settings, repos.tags) }
                factory { DownloadTasksViewModel(repos.downloads) }
                factory { SourceHitsViewModel(repos.sources, repos.manga) }
                factory { RankViewModel(repos.sources, repos.manga, repos.settings) }
            }
        )
    }
}
