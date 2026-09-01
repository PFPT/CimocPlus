package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "SOURCE", indices = [Index(value = ["TYPE"], unique = true)])
class Source {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "TITLE")
    var title: String = ""

    @ColumnInfo(name = "TYPE")
    var type: Int = 0

    @ColumnInfo(name = "ENABLE")
    var enable: Boolean = false

    constructor()

    @Ignore
    constructor(id: Long?, title: String, type: Int, enable: Boolean) {
        this.id = id
        this.title = title
        this.type = type
        this.enable = enable
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Source) return false
        val otherId = other.id
        val thisId = id
        if (thisId == null) return otherId == null
        return thisId == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: super.hashCode()
}
