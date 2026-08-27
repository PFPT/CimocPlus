package com.haleydu.cimoc.data
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceCatalogRefresher @Inject constructor(
    private val sourceConfigManager: SourceConfigManager,
    private val sourceRuleManager: SourceRuleManager,
    private val sourceManager: SourceManager,
    private val sourceHealthManager: SourceHealthManager,
    private val preferenceManager: PreferenceManager
) {

    @Synchronized
    fun refreshIfNeeded() {
        val invalid = sourceHealthManager.invalidTypes()
        val last = preferenceManager.getLong(PreferenceManager.PREF_SOURCE_CATALOG_REFRESH_AT, 0L)
        val now = System.currentTimeMillis()
        if (invalid.isEmpty() && now - last < PERIODIC_MS) return
        refresh(invalid)
    }

    @Synchronized
    fun refreshAfterFailure(type: Int) {
        if (!sourceHealthManager.isInvalid(type)) return
        refresh(sourceHealthManager.invalidTypes())
    }

    @Synchronized
    private fun refresh(invalidTypes: Set<Int>) {
        val now = System.currentTimeMillis()
        val last = preferenceManager.getLong(PreferenceManager.PREF_SOURCE_CATALOG_REFRESH_AT, 0L)
        if (now - last < COOLDOWN_MS) return
        preferenceManager.putLong(PreferenceManager.PREF_SOURCE_CATALOG_REFRESH_AT, now)
        try {
            val fetched = sourceConfigManager.fetchRemote()
            sourceConfigManager.applyToDatabase()
            sourceRuleManager.refreshRemote()
            sourceManager.clearParserCache()
            if (fetched) {
                for (t in invalidTypes) {
                    sourceHealthManager.reset(t)
                }
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val COOLDOWN_MS = 15 * 60 * 1000L
        const val PERIODIC_MS = 24 * 60 * 60 * 1000L
    }
}
