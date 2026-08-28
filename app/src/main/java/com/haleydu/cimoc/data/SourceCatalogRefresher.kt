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
        val json = preferenceManager.getString(SourceConfigManager.PREF_SOURCE_BASE_URL_JSON, "")
        val empty = json.isNullOrEmpty()
        val version = preferenceManager.getInt(PreferenceManager.PREF_SOURCE_CATALOG_VERSION, 0)
        val staleVersion = version < CATALOG_VERSION
        if (invalid.isEmpty() && !empty && !staleVersion && now - last < PERIODIC_MS) return
        refresh(invalid, force = empty || staleVersion || last == 0L)
    }

    @Synchronized
    fun refreshAfterFailure(type: Int) {
        if (!sourceHealthManager.isInvalid(type)) return
        refresh(sourceHealthManager.invalidTypes(), force = false)
    }

    @Synchronized
    private fun refresh(invalidTypes: Set<Int>, force: Boolean) {
        val now = System.currentTimeMillis()
        val last = preferenceManager.getLong(PreferenceManager.PREF_SOURCE_CATALOG_REFRESH_AT, 0L)
        if (!force && now - last < COOLDOWN_MS) return
        preferenceManager.putLong(PreferenceManager.PREF_SOURCE_CATALOG_REFRESH_AT, now)
        try {
            val fetched = sourceConfigManager.fetchRemote()
            sourceConfigManager.applyToDatabase()
            sourceRuleManager.refreshRemote()
            sourceManager.clearParserCache()
            if (fetched) {
                sourceHealthManager.resetAll()
                preferenceManager.putInt(PreferenceManager.PREF_SOURCE_CATALOG_VERSION, CATALOG_VERSION)
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val COOLDOWN_MS = 15 * 60 * 1000L
        const val PERIODIC_MS = 24 * 60 * 60 * 1000L
        const val CATALOG_VERSION = 3
    }
}
