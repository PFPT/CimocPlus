package com.haleydu.cimoc.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "TAG_REF")
public class TagRef {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @ColumnInfo(name = "TID")
    private long tid;
    @ColumnInfo(name = "CID")
    private long cid;

    public TagRef(Long id, long tid, long cid) {
        this.id = id;
        this.tid = tid;
        this.cid = cid;
    }

    public TagRef() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTid() {
        return this.tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public long getCid() {
        return this.cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

}
