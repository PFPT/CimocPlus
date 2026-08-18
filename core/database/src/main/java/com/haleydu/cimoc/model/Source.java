package com.haleydu.cimoc.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "SOURCE", indices = {@Index(value = {"TYPE"}, unique = true)})
public class Source {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @NonNull
    @ColumnInfo(name = "TITLE")
    private String title;
    @ColumnInfo(name = "TYPE")
    private int type;
    @ColumnInfo(name = "ENABLE")
    private boolean enable;

    public Source() {
    }

    public Source(Long id, @NonNull String title, int type, boolean enable) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.enable = enable;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Source && ((Source) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean getEnable() {
        return this.enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

}
