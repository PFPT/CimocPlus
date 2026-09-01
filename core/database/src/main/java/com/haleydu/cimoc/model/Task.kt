package com.haleydu.cimoc.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "TASK")
class Task : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "KEY")
    var key: Long = 0

    @ColumnInfo(name = "PATH")
    var path: String = ""

    @ColumnInfo(name = "TITLE")
    var title: String = ""

    @ColumnInfo(name = "PROGRESS")
    var progress: Int = 0

    @ColumnInfo(name = "MAX")
    var max: Int = 0

    @Ignore
    var source: Int = 0

    @Ignore
    var cid: String? = null

    @Ignore
    var state: Int = 0

    constructor()

    @Ignore
    constructor(source: Parcel) {
        this.id = source.readLong()
        this.key = source.readLong()
        this.path = source.readString() ?: ""
        this.title = source.readString() ?: ""
        this.progress = source.readInt()
        this.max = source.readInt()
        this.source = source.readInt()
        this.cid = source.readString()
        this.state = source.readInt()
    }

    @Ignore
    constructor(id: Long?, key: Long, path: String?, title: String?, progress: Int, max: Int) {
        this.id = id
        this.key = key
        this.path = path ?: ""
        this.title = title ?: ""
        this.progress = progress
        this.max = max
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Task) return false
        val otherId = other.id
        val thisId = id
        if (thisId == null) return otherId == null
        return thisId == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: super.hashCode()

    val isFinish: Boolean
        get() = max != 0 && progress == max

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id ?: 0L)
        dest.writeLong(key)
        dest.writeString(path)
        dest.writeString(title)
        dest.writeInt(progress)
        dest.writeInt(max)
        dest.writeInt(source)
        dest.writeString(cid)
        dest.writeInt(state)
    }

    companion object {
        const val STATE_FINISH = 0
        const val STATE_PAUSE = 1
        const val STATE_PARSE = 2
        const val STATE_DOING = 3
        const val STATE_WAIT = 4
        const val STATE_ERROR = 5

        @JvmField
        val CREATOR: Parcelable.Creator<Task> = object : Parcelable.Creator<Task> {
            override fun createFromParcel(source: Parcel): Task = Task(source)
            override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
        }
    }
}
