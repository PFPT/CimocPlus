package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "IMAGE_URL")
class ImageUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "COMIC_CHAPTER")
    var comicChapter: Long = 0L

    @ColumnInfo(name = "NUM")
    var num: Int = 0

    @TypeConverters(StringConverter::class)
    @ColumnInfo(name = "URLS")
    var urls: Array<String>? = null

    @ColumnInfo(name = "CHAPTER")
    var chapter: String? = null

    @ColumnInfo(name = "STATE")
    var state: Int = 0

    @ColumnInfo(name = "HEIGHT")
    var height: Int = 0

    @ColumnInfo(name = "WIDTH")
    var width: Int = 0

    @ColumnInfo(name = "LAZY")
    var lazy: Boolean = false

    @ColumnInfo(name = "LOADING")
    var loading: Boolean = false

    @ColumnInfo(name = "SUCCESS")
    var success: Boolean = false

    @ColumnInfo(name = "DOWNLOAD")
    var download: Boolean = false

    @Ignore
    var preview: Boolean = false

    constructor()

    @Ignore
    constructor(id: Long?, comicChapter: Long?, num: Int, urls: Array<String>?, chapter: String?, state: Int, lazy: Boolean) :
        this(id, comicChapter ?: 0L, num, urls, chapter, state, 0, 0, lazy, false, false, false)

    @Ignore
    constructor(id: Long?, comicChapter: Long?, num: Int, url: String?, lazy: Boolean) :
        this(id, comicChapter ?: 0L, num, arrayOf(url ?: ""), null, STATE_NULL, 0, 0, lazy, false, false, false)

    @Ignore
    constructor(
        id: Long?,
        comicChapter: Long,
        num: Int,
        urls: Array<String>?,
        chapter: String?,
        state: Int,
        height: Int,
        width: Int,
        lazy: Boolean,
        loading: Boolean,
        success: Boolean,
        download: Boolean
    ) {
        this.id = id
        this.comicChapter = comicChapter
        this.num = num
        this.urls = urls
        this.chapter = chapter
        this.state = state
        this.height = height
        this.width = width
        this.lazy = lazy
        this.loading = loading
        this.success = success
        this.download = download
    }

    var url: String
        get() {
            val list = urls
            if (list.isNullOrEmpty()) return ""
            return list[0]
        }
        set(value) {
            urls = arrayOf(value)
        }

    fun getSize(): Long = height.toLong() * width.toLong()

    override fun equals(other: Any?): Boolean {
        if (other !is ImageUrl) return false
        return other.id == id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    class StringConverter {
        @TypeConverter
        fun convertToEntityProperty(databaseValue: String?): Array<String>? {
            return databaseValue?.split(SPLIT)?.toTypedArray()
        }

        @TypeConverter
        fun convertToDatabaseValue(entityProperty: Array<String>?): String? {
            if (entityProperty == null) return null
            val sb = StringBuilder()
            for (str in entityProperty) {
                sb.append(str).append(SPLIT)
            }
            return sb.toString()
        }

        companion object {
            private const val SPLIT = "##Cimoc##"
        }
    }

    companion object {
        const val STATE_NULL = 0
        const val STATE_PAGE_1 = 1
        const val STATE_PAGE_2 = 2
    }
}
