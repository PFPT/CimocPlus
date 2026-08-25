package com.haleydu.cimoc.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Created by Hiroshi on 2016/7/2.
 * fixed by Haleydu on 2020/8/25.
 * Modified by lx200916 on 2021/2/7
 */
@Entity(tableName = "CHAPTER", indices = {@Index("SOURCE_COMIC")})
public class Chapter implements Parcelable {

    public final static Parcelable.Creator<Chapter> CREATOR = new Parcelable.Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel source) {
            return new Chapter(source);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @NonNull
    @ColumnInfo(name = "SOURCE_COMIC")
    private Long sourceComic;
    @ColumnInfo(name = "TITLE")
    private String title;
    @ColumnInfo(name = "PATH")
    private String path;
    @ColumnInfo(name = "COUNT")
    private int count;
    @ColumnInfo(name = "COMPLETE")
    private boolean complete;
    @ColumnInfo(name = "DOWNLOAD")
    private boolean download;
    @ColumnInfo(name = "TID")
    private long tid;
    @ColumnInfo(name = "SOURCE_GROUP")
    private String sourceGroup;

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, long tid) {
        this(id, sourceComic, title, path, 0, false, false, tid, "");
    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, String sourceGroup) {
        this(id, sourceComic, title, path, 0, false, false, -1, sourceGroup);

    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path) {
        this(id, sourceComic, title, path, 0, false, false, -1, "");
    }

    @Ignore
    public Chapter(String title, String path) {
        this.title = title;
        this.path = path;
        this.count = 0;
        this.complete = false;
        this.download = false;
        this.tid = -1;
    }

    @Ignore
    public Chapter(Parcel source) {
        this(source.readLong(), source.readLong(), source.readString(), source.readString(), source.readInt(), source.readByte() == 1, source.readByte() == 1, source.readLong(), "");
    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, int progress, boolean b, boolean b1, Long id1) {
        this(id, sourceComic, title, path, progress, b, b1, id1, "");

    }

    @Ignore
    public Chapter(Long id, @NonNull Long sourceComic, String title, String path, int count, boolean complete, boolean download, long tid, String sourceGroup) {
        this.id = id;
        this.sourceComic = sourceComic;
        this.title = title;
        this.path = path;
        this.count = count;
        this.complete = complete;
        this.download = download;
        this.tid = tid;
        this.sourceGroup = sourceGroup;
    }

    public String getSourceGroup() {
        return sourceGroup == null ? "" : sourceGroup;
    }

    public void setSourceGroup(String sourceGroup) {
        this.sourceGroup = sourceGroup;
    }

    public Chapter() {
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public long getTid() {
        return tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceComic() {
        return sourceComic;
    }

    public void setSourceComic(Long sourceComic) {
        this.sourceComic = sourceComic;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Chapter && ((Chapter) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id !=null) {
            dest.writeLong(id);
        }else {
            dest.writeLong(0L);
        }
        if (sourceComic !=null){
            dest.writeLong(sourceComic);
        } else {
            dest.writeLong(0L);
        }
        dest.writeString(title);
        dest.writeString(path);
        dest.writeInt(count);
        dest.writeByte((byte) (complete ? 1 : 0));
        dest.writeByte((byte) (download ? 1 : 0));
        dest.writeLong(tid);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Ignore
    public boolean getComplete() {
        return this.complete;
    }

    @Ignore
    public boolean getDownload() {
        return this.download;
    }

}
