package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "TAG")
class Tag {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "TITLE")
    var title: String = ""

    constructor()

    @Ignore
    constructor(id: Long?, title: String) {
        this.id = id
        this.title = title
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Tag) return false
        val otherId = other.id
        val thisId = id
        if (thisId == null) return otherId == null
        return thisId == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: super.hashCode()
}
