package com.haleydu.cimoc.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object CimocMigrations {
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate12to13(db::execSQL)
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate13to14(db::execSQL)
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate14to15(db::execSQL)
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate15to16(db::execSQL)
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate16to17(db::execSQL)
    }

    val ALL = arrayOf(
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17
    )

    fun migrate12to13(exec: (String) -> Unit) {
        exec(
            "DELETE FROM SOURCE WHERE _id NOT IN " +
                "(SELECT min_id FROM (SELECT MIN(_id) AS min_id FROM SOURCE GROUP BY TYPE))"
        )
        exec("CREATE UNIQUE INDEX IF NOT EXISTS `index_SOURCE_TYPE` ON `SOURCE` (`TYPE`)")
    }

    fun migrate13to14(exec: (String) -> Unit) {
        exec(
            "CREATE TABLE IF NOT EXISTS `SOURCE_RULE` (" +
                "`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`TYPE` INTEGER NOT NULL, " +
                "`SCRIPT` TEXT NOT NULL, " +
                "`VERSION` TEXT NOT NULL, " +
                "`REMOTE_URL` TEXT, " +
                "`UPDATED_AT` INTEGER NOT NULL)"
        )
        exec("CREATE UNIQUE INDEX IF NOT EXISTS `index_SOURCE_RULE_TYPE` ON `SOURCE_RULE` (`TYPE`)")
    }

    fun migrate14to15(exec: (String) -> Unit) {
        exec(
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

    fun migrate15to16(exec: (String) -> Unit) {
        exec("CREATE INDEX IF NOT EXISTS `index_COMIC_SOURCE_CID` ON `COMIC` (`SOURCE`, `CID`)")
        exec("CREATE INDEX IF NOT EXISTS `index_CHAPTER_SOURCE_COMIC` ON `CHAPTER` (`SOURCE_COMIC`)")
    }

    fun migrate16to17(exec: (String) -> Unit) {
        exec(
            "DELETE FROM CHAPTER WHERE PATH IS NOT NULL AND PATH != '' AND _id NOT IN (" +
                "SELECT keep_id FROM (" +
                "SELECT COALESCE(MIN(CASE WHEN COMPLETE != 0 THEN _id END), MIN(_id)) AS keep_id " +
                "FROM CHAPTER WHERE PATH IS NOT NULL AND PATH != '' " +
                "GROUP BY SOURCE_COMIC, PATH))"
        )
        exec(
            "DELETE FROM CHAPTER WHERE PATH = '' AND _id NOT IN (" +
                "SELECT keep_id FROM (" +
                "SELECT COALESCE(MIN(CASE WHEN COMPLETE != 0 THEN _id END), MIN(_id)) AS keep_id " +
                "FROM CHAPTER WHERE PATH = '' GROUP BY SOURCE_COMIC))"
        )
        exec(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_CHAPTER_SOURCE_COMIC_PATH` " +
                "ON `CHAPTER` (`SOURCE_COMIC`, `PATH`)"
        )
    }
}
