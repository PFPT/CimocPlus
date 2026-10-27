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
import com.haleydu.cimoc.model.SourceHealth
import com.haleydu.cimoc.model.SourceRule
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.model.Task

@Database(
    entities = [Comic::class, Chapter::class, Tag::class, TagRef::class, Source::class, Task::class, ImageUrl::class, SourceRule::class, SourceHealth::class],
    version = 17,
    exportSchema = false
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

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SOURCE_RULE` (" +
                        "`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`TYPE` INTEGER NOT NULL, " +
                        "`SCRIPT` TEXT NOT NULL, " +
                        "`VERSION` TEXT NOT NULL, " +
                        "`REMOTE_URL` TEXT, " +
                        "`UPDATED_AT` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_SOURCE_RULE_TYPE` ON `SOURCE_RULE` (`TYPE`)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SOURCE_HEALTH` (" +
                        "`TYPE` INTEGER NOT NULL, " +
                        "`FAIL_COUNT` INTEGER NOT NULL, " +
                        "`LATENCY_MS` INTEGER NOT NULL, " +
                        "`SUCCESS_AT` INTEGER NOT NULL, " +
                        "`FAILURE_AT` INTEGER NOT NULL, " +
                        "`ERROR_KIND` TEXT, " +
                        "PRIMARY KEY(`TYPE`))"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_COMIC_SOURCE_CID` ON `COMIC` (`SOURCE`, `CID`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CHAPTER_SOURCE_COMIC` ON `CHAPTER` (`SOURCE_COMIC`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM CHAPTER WHERE PATH IS NOT NULL AND PATH != '' AND _id NOT IN (" +
                        "SELECT keep_id FROM (" +
                        "SELECT COALESCE(MIN(CASE WHEN COMPLETE != 0 THEN _id END), MIN(_id)) AS keep_id " +
                        "FROM CHAPTER WHERE PATH IS NOT NULL AND PATH != '' " +
                        "GROUP BY SOURCE_COMIC, PATH))"
                )
                db.execSQL(
                    "DELETE FROM CHAPTER WHERE PATH = '' AND _id NOT IN (" +
                        "SELECT keep_id FROM (" +
                        "SELECT COALESCE(MIN(CASE WHEN COMPLETE != 0 THEN _id END), MIN(_id)) AS keep_id " +
                        "FROM CHAPTER WHERE PATH = '' GROUP BY SOURCE_COMIC))"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_CHAPTER_SOURCE_COMIC_PATH` " +
                        "ON `CHAPTER` (`SOURCE_COMIC`, `PATH`)"
                )
            }
        }

        @Volatile
        private var instance: CimocDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): CimocDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, CimocDatabase::class.java, "cimoc.db")
                    .addMigrations(
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17
                    )
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
