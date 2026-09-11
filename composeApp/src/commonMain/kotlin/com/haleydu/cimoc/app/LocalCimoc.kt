package com.haleydu.cimoc.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.haleydu.cimoc.shared.repo.CimocRepositories

val LocalCimocRepos = staticCompositionLocalOf<CimocRepositories> {
    error("CimocRepositories not provided")
}

val LocalCimocSession = staticCompositionLocalOf<CimocSession> {
    error("CimocSession not provided")
}

data class CimocActions(
    val bindBack: ((() -> Boolean) -> Unit) = {},
    val bindVolume: (((Boolean) -> Boolean) -> Unit) = {},
    val onNightChanged: (Boolean) -> Unit = {},
    val openUrl: (String) -> Unit = {},
    val setImmersive: (Boolean) -> Unit = {},
    val setKeepScreenOn: (Boolean) -> Unit = {},
    val setBrightness: (Float?) -> Unit = {}
)

val LocalCimocActions = staticCompositionLocalOf { CimocActions() }
