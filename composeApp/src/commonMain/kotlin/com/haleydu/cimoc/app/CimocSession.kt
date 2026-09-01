package com.haleydu.cimoc.app

import com.haleydu.cimoc.shared.repo.CimocRepositories
import com.haleydu.cimoc.shared.vm.DetailViewModel
import com.haleydu.cimoc.shared.vm.ExploreViewModel
import com.haleydu.cimoc.shared.vm.LibraryViewModel
import com.haleydu.cimoc.shared.vm.ReaderViewModel
import com.haleydu.cimoc.shared.vm.SearchViewModel
import com.haleydu.cimoc.shared.vm.SettingsViewModel
import com.haleydu.cimoc.shared.vm.SourceListViewModel

class CimocSession(val repos: CimocRepositories) {
    val library by lazy { LibraryViewModel(repos.comics, repos.sources) }
    val search by lazy { SearchViewModel(repos.sources, repos.manga) }
    val sources by lazy { SourceListViewModel(repos.sources) }
    val explore by lazy { ExploreViewModel(repos.explore, repos.sources) }
    val settings by lazy { SettingsViewModel(repos.settings) }
    val reader by lazy { ReaderViewModel(repos.comics, repos.manga, repos.settings) }

    fun detail(): DetailViewModel = DetailViewModel(repos.comics, repos.manga, repos.sources, repos.downloads)
}
