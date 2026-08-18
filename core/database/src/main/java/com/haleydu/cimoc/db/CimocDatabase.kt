package com.haleydu.cimoc.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.model.Task

@Database(
    entities = [Comic::class, Chapter::class, Tag::class, TagRef::class, Source::class, Task::class, ImageUrl::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(ImageUrl.StringConverter::class)
abstract class CimocDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun tagDao(): TagDao
    abstract fun tagRefDao(): TagRefDao
    abstract fun sourceDao(): SourceDao
    abstract fun taskDao(): TaskDao
    abstract fun imageUrlDao(): ImageUrlDao

    companion object {
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM SOURCE WHERE _id NOT IN " +
                        "(SELECT min_id FROM (SELECT MIN(_id) AS min_id FROM SOURCE GROUP BY TYPE))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_SOURCE_TYPE` ON `SOURCE` (`TYPE`)")
            }
        }

        @Volatile
        private var instance: CimocDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): CimocDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, CimocDatabase::class.java, "cimoc.db")
                    .addMigrations(MIGRATION_12_13)
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
