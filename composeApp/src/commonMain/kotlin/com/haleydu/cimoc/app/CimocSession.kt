package com.haleydu.cimoc.app

import com.haleydu.cimoc.shared.vm.DetailViewModel
import com.haleydu.cimoc.shared.vm.DownloadTasksViewModel
import com.haleydu.cimoc.shared.vm.ExploreViewModel
import com.haleydu.cimoc.shared.vm.LibraryViewModel
import com.haleydu.cimoc.shared.vm.ReaderViewModel
import com.haleydu.cimoc.shared.vm.SearchViewModel
import com.haleydu.cimoc.shared.vm.SettingsViewModel
import com.haleydu.cimoc.shared.vm.SourceHitsViewModel
import com.haleydu.cimoc.shared.vm.SourceListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class CimocSession : KoinComponent {
    val library: LibraryViewModel get() = get()
    val search: SearchViewModel get() = get()
    val sources: SourceListViewModel get() = get()
    val explore: ExploreViewModel get() = get()
    val settings: SettingsViewModel get() = get()
    val reader: ReaderViewModel get() = get()

    fun detail(): DetailViewModel = get()
    fun downloadTasks(): DownloadTasksViewModel = get()
    fun sourceHits(): SourceHitsViewModel = get()
}
