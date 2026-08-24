package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SOURCE_HEALTH")
class SourceHealth {
    @PrimaryKey
    @ColumnInfo(name = "TYPE")
    var type: Int = 0

    @ColumnInfo(name = "FAIL_COUNT")
    var consecutiveFailures: Int = 0

    @ColumnInfo(name = "LATENCY_MS")
    var lastLatencyMs: Long = -1

    @ColumnInfo(name = "SUCCESS_AT")
    var lastSuccessAt: Long = 0

    @ColumnInfo(name = "FAILURE_AT")
    var lastFailureAt: Long = 0

    @ColumnInfo(name = "ERROR_KIND")
    var lastErrorKind: String? = null
}
