package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "SOURCE_RULE", indices = [Index(value = ["TYPE"], unique = true)])
class SourceRule {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    @ColumnInfo(name = "TYPE")
    var type: Int = 0

    @ColumnInfo(name = "SCRIPT")
    var scriptContent: String = ""

    @ColumnInfo(name = "VERSION")
    var version: String = ""

    @ColumnInfo(name = "REMOTE_URL")
    var remoteUrl: String? = null

    @ColumnInfo(name = "UPDATED_AT")
    var updatedAt: Long = 0
}
