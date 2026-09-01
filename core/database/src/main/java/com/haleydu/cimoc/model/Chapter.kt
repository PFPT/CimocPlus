package com.haleydu.cimoc.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "CHAPTER", indices = [Index("SOURCE_COMIC")])
class Chapter : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "SOURCE_COMIC")
    var sourceComic: Long = 0L

    @ColumnInfo(name = "TITLE")
    var title: String? = null

    @ColumnInfo(name = "PATH")
    var path: String? = null

    @ColumnInfo(name = "COUNT")
    var count: Int = 0

    @ColumnInfo(name = "COMPLETE")
    var complete: Boolean = false

    @ColumnInfo(name = "DOWNLOAD")
    var download: Boolean = false

    @ColumnInfo(name = "TID")
    var tid: Long = -1

    @ColumnInfo(name = "SOURCE_GROUP")
    var sourceGroup: String? = null
        get() = field ?: ""

    constructor()

    @Ignore
    constructor(id: Long?, sourceComic: Long?, title: String?, path: String?, tid: Long) :
        this(id, sourceComic ?: 0L, title, path, 0, false, false, tid, "")

    @Ignore
    constructor(id: Long?, sourceComic: Long?, title: String?, path: String?, sourceGroup: String?) :
        this(id, sourceComic ?: 0L, title, path, 0, false, false, -1, sourceGroup)

    @Ignore
    constructor(id: Long?, sourceComic: Long?, title: String?, path: String?) :
        this(id, sourceComic ?: 0L, title, path, 0, false, false, -1, "")

    @Ignore
    constructor(title: String?, path: String?) {
        this.title = title
        this.path = path
        this.count = 0
        this.complete = false
        this.download = false
        this.tid = -1
    }

    @Ignore
    constructor(source: Parcel) : this(
        source.readLong(),
        source.readLong(),
        source.readString(),
        source.readString(),
        source.readInt(),
        source.readByte().toInt() == 1,
        source.readByte().toInt() == 1,
        source.readLong(),
        ""
    )

    @Ignore
    constructor(
        id: Long?,
        sourceComic: Long?,
        title: String?,
        path: String?,
        progress: Int,
        complete: Boolean,
        download: Boolean,
        tid: Long?
    ) : this(id, sourceComic ?: 0L, title, path, progress, complete, download, tid ?: -1L, "")

    @Ignore
    constructor(
        id: Long?,
        sourceComic: Long,
        title: String?,
        path: String?,
        count: Int,
        complete: Boolean,
        download: Boolean,
        tid: Long,
        sourceGroup: String?
    ) {
        this.id = id
        this.sourceComic = sourceComic
        this.title = title
        this.path = path
        this.count = count
        this.complete = complete
        this.download = download
        this.tid = tid
        this.sourceGroup = sourceGroup
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Chapter) return false
        val otherPath = other.path
        val thisPath = path
        if (thisPath == null) return otherPath == null
        return thisPath == otherPath
    }

    override fun hashCode(): Int = path?.hashCode() ?: 0

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id ?: 0L)
        dest.writeLong(sourceComic)
        dest.writeString(title)
        dest.writeString(path)
        dest.writeInt(count)
        dest.writeByte(if (complete) 1 else 0)
        dest.writeByte(if (download) 1 else 0)
        dest.writeLong(tid)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Chapter> = object : Parcelable.Creator<Chapter> {
            override fun createFromParcel(source: Parcel): Chapter = Chapter(source)
            override fun newArray(size: Int): Array<Chapter?> = arrayOfNulls(size)
        }
    }
}
