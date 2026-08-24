package com.haleydu.cimoc.data
import com.haleydu.cimoc.db.SourceHealthDao
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceHealth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHealthManager @Inject constructor(
    private val sourceHealthDao: SourceHealthDao
) {

    @Synchronized
    fun recordSuccess(type: Int, elapsedMs: Long) {
        val health = sourceHealthDao.load(type) ?: SourceHealth().also { it.type = type }
        health.consecutiveFailures = 0
        health.lastLatencyMs = elapsedMs
        health.lastSuccessAt = System.currentTimeMillis()
        health.lastErrorKind = null
        sourceHealthDao.insertOrReplace(health)
    }

    @Synchronized
    fun recordFailure(type: Int, kind: String) {
        val health = sourceHealthDao.load(type) ?: SourceHealth().also { it.type = type }
        health.consecutiveFailures = health.consecutiveFailures + 1
        health.lastFailureAt = System.currentTimeMillis()
        health.lastErrorKind = kind
        sourceHealthDao.insertOrReplace(health)
    }

    fun isInvalid(type: Int): Boolean {
        val count = sourceHealthDao.load(type)?.consecutiveFailures ?: 0
        return count >= INVALID_THRESHOLD
    }

    fun invalidTypes(): Set<Int> {
        return sourceHealthDao.list()
            .filter { it.consecutiveFailures >= INVALID_THRESHOLD }
            .map { it.type }
            .toSet()
    }

    fun sortTypes(types: Collection<Int>): List<Int> {
        val map = sourceHealthDao.list().associateBy { it.type }
        return types.sortedWith(healthComparator(map))
    }

    fun sortSources(sources: List<Source>): List<Source> {
        val map = sourceHealthDao.list().associateBy { it.type }
        return sources.sortedWith(compareBy({ healthRank(it.type, map) }, { latency(it.type, map) }, { it.type }))
    }

    private fun healthComparator(map: Map<Int, SourceHealth>): Comparator<Int> {
        return compareBy({ healthRank(it, map) }, { latency(it, map) }, { it })
    }

    private fun healthRank(type: Int, map: Map<Int, SourceHealth>): Int {
        val count = map[type]?.consecutiveFailures ?: 0
        return if (count >= INVALID_THRESHOLD) 1 else 0
    }

    private fun latency(type: Int, map: Map<Int, SourceHealth>): Long {
        val value = map[type]?.lastLatencyMs ?: -1L
        return if (value < 0) Long.MAX_VALUE else value
    }

    companion object {
        const val INVALID_THRESHOLD = 3
        const val KIND_NETWORK = "NETWORK"
        const val KIND_PARSE = "PARSE"
        const val KIND_EMPTY = "EMPTY"
    }
}
