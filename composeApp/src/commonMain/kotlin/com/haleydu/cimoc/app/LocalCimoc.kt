package com.haleydu.cimoc.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.haleydu.cimoc.shared.repo.CimocRepositories

val LocalCimocRepos = staticCompositionLocalOf<CimocRepositories> {
    error("CimocRepositories not provided")
}

data class CimocActions(
    val openSettings: () -> Unit = {},
    val openAbout: () -> Unit = {},
    val openBackup: () -> Unit = {},
    val openClassic: () -> Unit = {}
)

val LocalCimocActions = staticCompositionLocalOf { CimocActions() }
