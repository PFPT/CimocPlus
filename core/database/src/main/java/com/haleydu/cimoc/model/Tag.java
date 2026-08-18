package com.haleydu.cimoc.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "TAG")
public class Tag {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @NonNull
    @ColumnInfo(name = "TITLE")
    private String title;

    public Tag(Long id, @NonNull String title) {
        this.id = id;
        this.title = title;
    }

    public Tag() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tag && ((Tag) o).id.equals(id);
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

}
