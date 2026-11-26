package com.haleydu.cimoc.shared.model

fun sourcesForSearch(sources: List<SourceItem>): List<SourceItem> {
    return sources.filter { it.enable && !it.web }
}

fun selectedSearchTypes(sources: List<SourceItem>, previous: Set<Int>): Set<Int> {
    val enabled = sources.map { it.type }.toSet()
    val healthy = sources.filter { !it.invalid }.map { it.type }.toSet()
    return if (previous.isEmpty()) {
        healthy
    } else {
        previous.filter { it in enabled }.toSet().ifEmpty { healthy }
    }
}
