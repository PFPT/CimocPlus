package com.haleydu.cimoc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "COMIC", indices = [Index(value = ["SOURCE", "CID"])])
class Comic {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "SOURCE")
    var source: Int = 0

    @ColumnInfo(name = "CID")
    var cid: String = ""

    @ColumnInfo(name = "TITLE")
    var title: String = ""

    @ColumnInfo(name = "COVER")
    var cover: String = ""

    @ColumnInfo(name = "HIGHLIGHT")
    var highlight: Boolean = false

    @ColumnInfo(name = "LOCAL")
    var local: Boolean = false

    @ColumnInfo(name = "UPDATE")
    var update: String? = null

    @ColumnInfo(name = "FINISH")
    var finish: Boolean? = null

    @ColumnInfo(name = "FAVORITE")
    var favorite: Long? = null

    @ColumnInfo(name = "HISTORY")
    var history: Long? = null

    @ColumnInfo(name = "DOWNLOAD")
    var download: Long? = null

    @ColumnInfo(name = "LAST")
    var last: String? = null

    @ColumnInfo(name = "PAGE")
    var page: Int? = null

    @ColumnInfo(name = "CHAPTER")
    var chapter: String? = null

    @ColumnInfo(name = "URL")
    var url: String? = null

    @Ignore
    @JvmField
    var note: Any? = null

    @ColumnInfo(name = "INTRO")
    var intro: String? = null

    @ColumnInfo(name = "AUTHOR")
    var author: String? = null

    constructor()

    @Ignore
    constructor(source: Int, cid: String?, title: String?, cover: String?, update: String?, author: String?) : this(
        null, source, cid ?: "", title ?: "", cover ?: "", false, false, update,
        null, null, null, null, null, null, null, null, null, author
    )

    @Ignore
    constructor(source: Int, cid: String?) {
        this.source = source
        this.cid = cid ?: ""
    }

    @Ignore
    constructor(source: Int, cid: String?, title: String?, cover: String?, download: Long) : this(
        null, source, cid ?: "", title ?: "", cover ?: "", false, false, null,
        null, null, null, download, null, null, null, null, null, null
    )

    @Ignore
    constructor(
        id: Long?,
        source: Int,
        cid: String,
        title: String,
        cover: String,
        highlight: Boolean,
        local: Boolean,
        update: String?,
        finish: Boolean?,
        favorite: Long?,
        history: Long?,
        download: Long?,
        last: String?,
        page: Int?,
        chapter: String?,
        url: String?,
        intro: String?,
        author: String?
    ) {
        this.id = id
        this.source = source
        this.cid = cid
        this.title = title
        this.cover = cover
        this.highlight = highlight
        this.local = local
        this.update = update
        this.finish = finish
        this.favorite = favorite
        this.history = history
        this.download = download
        this.last = last
        this.page = page
        this.chapter = chapter
        this.url = url
        this.intro = intro
        this.author = author
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Comic) return false
        val otherId = other.id
        val thisId = id
        if (thisId == null) return otherId == null
        return thisId == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: super.hashCode()

    fun setInfo(title: String?, cover: String?, update: String?, intro: String?, author: String?, finish: Boolean) {
        if (title != null) this.title = title
        if (cover != null) this.cover = cover
        if (update != null) this.update = update
        this.intro = intro
        if (author != null) this.author = author
        this.finish = finish
        this.highlight = false
    }
}
