package com.haleydu.cimoc.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Hiroshi on 2016/8/20.
 */
@Entity(tableName = "IMAGE_URL")
public class ImageUrl {

    public static final int STATE_NULL = 0;
    public static final int STATE_PAGE_1 = 1;
    public static final int STATE_PAGE_2 = 2;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @NonNull
    @ColumnInfo(name = "COMIC_CHAPTER")
    private Long comicChapter;
    @ColumnInfo(name = "NUM")
    private int num;
    @TypeConverters(StringConverter.class)
    @ColumnInfo(name = "URLS")
    private String[] urls;
    @ColumnInfo(name = "CHAPTER")
    private String chapter;
    @ColumnInfo(name = "STATE")
    private int state;
    @ColumnInfo(name = "HEIGHT")
    private int height;
    @ColumnInfo(name = "WIDTH")
    private int width;
    @ColumnInfo(name = "LAZY")
    private boolean lazy;
    @ColumnInfo(name = "LOADING")
    private boolean loading;
    @ColumnInfo(name = "SUCCESS")
    private boolean success;
    @ColumnInfo(name = "DOWNLOAD")
    private boolean download;
    @Ignore
    private boolean preview;

    @Ignore
    public ImageUrl(Long id, Long comicChapter, int num, String[] urls, String chapter, int state, boolean lazy) {
        this(id, comicChapter, num, urls, chapter, state, 0, 0, lazy,
                false, false,false);
    }

    @Ignore
    public ImageUrl(Long id,Long comicChapter,int num, String url, boolean lazy) {
       this(id, comicChapter, num, new String[]{url}, null, STATE_NULL,
               0, 0, lazy, false, false, false);
    }

    @Ignore
    public ImageUrl(Long id, @NonNull Long comicChapter, int num, String[] urls,
            String chapter, int state, int height, int width, boolean lazy, boolean loading,
            boolean success, boolean download) {
        this.id = id;
        this.comicChapter = comicChapter;
        this.num = num;
        this.urls = urls;
        this.chapter = chapter;
        this.state = state;
        this.height = height;
        this.width = width;
        this.lazy = lazy;
        this.loading = loading;
        this.success = success;
        this.download = download;
    }

    public ImageUrl() {
    }

    public Long getId() {
        return id;
    }

    public int getNum() {
        return num;
    }

    public String[] getUrls() {
        return urls;
    }

    public String getUrl() {
        if (urls == null || urls.length == 0) {
            return "";
        }
        return urls[0];
    }

    public void setUrl(String url) {
        this.urls = new String[]{url};
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public long getSize() {
        return height * width;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    @Ignore
    public boolean isPreview() {
        return preview;
    }

    @Ignore
    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ImageUrl && ((ImageUrl) o).id == id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComicChapter() {
        return this.comicChapter;
    }

    public void setComicChapter(Long comicChapter) {
        this.comicChapter = comicChapter;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    @Ignore
    public boolean getLazy() {
        return this.lazy;
    }

    @Ignore
    public boolean getLoading() {
        return this.loading;
    }

    @Ignore
    public boolean getSuccess() {
        return this.success;
    }

    @Ignore
    public boolean getDownload() {
        return this.download;
    }


    public static class StringConverter {
        private static final String SPLIT = "##Cimoc##";

        @TypeConverter
        public String[] convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            } else {
                return databaseValue.split(SPLIT);
            }
        }

        @TypeConverter
        public String convertToDatabaseValue(String[] entityProperty) {
            if (entityProperty == null) {
                return null;
            } else {
                StringBuilder sb = new StringBuilder();
                for (String str : entityProperty) {
                    sb.append(str).append(SPLIT);
                }
                return sb.toString();
            }

        }

    }
}
