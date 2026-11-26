package com.haleydu.cimoc.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceHealth
import com.haleydu.cimoc.model.SourceRule
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.model.Task

@Database(
    entities = [Comic::class, Chapter::class, Tag::class, TagRef::class, Source::class, Task::class, ImageUrl::class, SourceRule::class, SourceHealth::class],
    version = 17,
    exportSchema = true
)
@TypeConverters(ImageUrl.StringConverter::class)
abstract class CimocDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun tagDao(): TagDao
    abstract fun tagRefDao(): TagRefDao
    abstract fun sourceDao(): SourceDao
    abstract fun sourceRuleDao(): SourceRuleDao
    abstract fun sourceHealthDao(): SourceHealthDao
    abstract fun taskDao(): TaskDao
    abstract fun imageUrlDao(): ImageUrlDao
}
