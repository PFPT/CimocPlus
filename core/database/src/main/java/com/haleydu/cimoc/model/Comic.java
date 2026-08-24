package com.haleydu.cimoc.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Created by Hiroshi on 2016/7/20.
 */
@Entity(tableName = "COMIC", indices = {@Index(value = {"SOURCE", "CID"})})
public class Comic {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @ColumnInfo(name = "SOURCE")
    private int source;
    @NonNull
    @ColumnInfo(name = "CID")
    private String cid;
    @NonNull
    @ColumnInfo(name = "TITLE")
    private String title;
    @NonNull
    @ColumnInfo(name = "COVER")
    private String cover;
    @ColumnInfo(name = "HIGHLIGHT")
    private boolean highlight;
    @ColumnInfo(name = "LOCAL")
    private boolean local;
    @ColumnInfo(name = "UPDATE")
    private String update;
    @ColumnInfo(name = "FINISH")
    private Boolean finish;
    @ColumnInfo(name = "FAVORITE")
    private Long favorite;
    @ColumnInfo(name = "HISTORY")
    private Long history;
    @ColumnInfo(name = "DOWNLOAD")
    private Long download;
    @ColumnInfo(name = "LAST")
    private String last;
    @ColumnInfo(name = "PAGE")
    private Integer page;
    @ColumnInfo(name = "CHAPTER")
    private String chapter;
    @ColumnInfo(name = "URL")
    private String url;
    @Ignore
    public Object note;

    @ColumnInfo(name = "INTRO")
    private String intro;

    @ColumnInfo(name = "AUTHOR")
    private String author;

    @Ignore
    public Comic(int source, String cid, String title, String cover, String update, String author) {
        this(null, source, cid, title, cover == null ? "" : cover, false, false, update,
                null, null, null, null, null, null, null, null, null, null);
        this.author = author;
    }

    @Ignore
    public Comic(int source, String cid) {
        this.source = source;
        this.cid = cid;
    }

    @Ignore
    public Comic(int source, String cid, String title, String cover, long download) {
        this(null, source, cid, title, cover == null ? "" : cover, false, false, null,
                null, null, null, download, null, null, null, null,null,null);
    }

    public Comic(Long id, int source, @NonNull String cid, @NonNull String title, @NonNull String cover, boolean highlight,
            boolean local, String update, Boolean finish, Long favorite, Long history, Long download, String last, Integer page,
            String chapter, String url, String intro, String author) {
        this.id = id;
        this.source = source;
        this.cid = cid;
        this.title = title;
        this.cover = cover;
        this.highlight = highlight;
        this.local = local;
        this.update = update;
        this.finish = finish;
        this.favorite = favorite;
        this.history = history;
        this.download = download;
        this.last = last;
        this.page = page;
        this.chapter = chapter;
        this.url = url;
        this.intro = intro;
        this.author = author;
    }

    public Comic() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Comic && ((Comic) o).id.equals(id);
    }

    public void setInfo(String title, String cover, String update, String intro, String author, boolean finish) {
        if (title != null) {
            this.title = title;
        }
        if (cover != null) {
            this.cover = cover;
        }
        if (update != null) {
            this.update = update;
        }
        this.intro = intro;
        if (author != null) {
            this.author = author;
        }
        this.finish = finish;
        this.highlight = false;
    }

    public String getIntro() {
        return this.intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getPage() {
        return this.page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getLast() {
        return this.last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public Long getHistory() {
        return this.history;
    }

    public void setHistory(Long history) {
        this.history = history;
    }

    public Long getFavorite() {
        return this.favorite;
    }

    public void setFavorite(Long favorite) {
        this.favorite = favorite;
    }

    public String getUpdate() {
        return this.update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public String getCover() {
        return this.cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCid() {
        return this.cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public int getSource() {
        return this.source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getHighlight() {
        return this.highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public Long getDownload() {
        return this.download;
    }

    public void setDownload(Long download) {
        this.download = download;
    }

    public Boolean getFinish() {
        return this.finish;
    }

    public void setFinish(Boolean finish) {
        this.finish = finish;
    }

    public boolean getLocal() {
        return this.local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getChapter() {
        return this.chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
