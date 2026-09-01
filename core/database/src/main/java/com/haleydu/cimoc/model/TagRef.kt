package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "TAG_REF")
class TagRef {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "TID")
    var tid: Long = 0

    @ColumnInfo(name = "CID")
    var cid: Long = 0

    constructor()

    @Ignore
    constructor(id: Long?, tid: Long, cid: Long) {
        this.id = id
        this.tid = tid
        this.cid = cid
    }
}
