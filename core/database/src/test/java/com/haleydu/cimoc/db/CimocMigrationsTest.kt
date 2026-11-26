package com.haleydu.cimoc.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class CimocMigrationsTest {
    @Test
    fun migratesVersion12Through17() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE SOURCE (_id INTEGER PRIMARY KEY, TITLE TEXT, TYPE INTEGER NOT NULL, ENABLE INTEGER NOT NULL)"
                )
                statement.execute(
                    "CREATE TABLE COMIC (_id INTEGER PRIMARY KEY, SOURCE INTEGER NOT NULL, CID TEXT NOT NULL)"
                )
                statement.execute(
                    "CREATE TABLE CHAPTER (" +
                        "_id INTEGER PRIMARY KEY, SOURCE_COMIC INTEGER NOT NULL, TITLE TEXT, PATH TEXT, " +
                        "COMPLETE INTEGER NOT NULL)"
                )
                statement.execute("INSERT INTO SOURCE (_id, TITLE, TYPE, ENABLE) VALUES (1, 'A', 7, 1)")
                statement.execute("INSERT INTO SOURCE (_id, TITLE, TYPE, ENABLE) VALUES (2, 'A duplicate', 7, 0)")
                statement.execute("INSERT INTO SOURCE (_id, TITLE, TYPE, ENABLE) VALUES (3, 'B', 8, 1)")
                statement.execute("INSERT INTO COMIC (_id, SOURCE, CID) VALUES (1, 7, 'comic')")
                statement.execute("INSERT INTO CHAPTER (_id, SOURCE_COMIC, TITLE, PATH, COMPLETE) VALUES (1, 10, 'old', 'ch-1', 0)")
                statement.execute("INSERT INTO CHAPTER (_id, SOURCE_COMIC, TITLE, PATH, COMPLETE) VALUES (2, 10, 'done', 'ch-1', 1)")
                statement.execute("INSERT INTO CHAPTER (_id, SOURCE_COMIC, TITLE, PATH, COMPLETE) VALUES (3, 10, 'blank-old', '', 0)")
                statement.execute("INSERT INTO CHAPTER (_id, SOURCE_COMIC, TITLE, PATH, COMPLETE) VALUES (4, 10, 'blank-done', '', 1)")
                statement.execute("INSERT INTO CHAPTER (_id, SOURCE_COMIC, TITLE, PATH, COMPLETE) VALUES (5, 11, 'only', 'ch-2', 0)")
            }
            val exec: (String) -> Unit = { sql -> db.exec(sql) }
            CimocMigrations.migrate12to13(exec)
            CimocMigrations.migrate13to14(exec)
            CimocMigrations.migrate14to15(exec)
            CimocMigrations.migrate15to16(exec)
            CimocMigrations.migrate16to17(exec)

            assertEquals(listOf(1L, 3L), db.queryLongs("SELECT _id FROM SOURCE ORDER BY _id"))
            assertEquals(listOf(2L, 4L, 5L), db.queryLongs("SELECT _id FROM CHAPTER ORDER BY _id"))
            assertEquals("done", db.queryString("SELECT TITLE FROM CHAPTER WHERE _id = 2"))
            assertTrue(db.hasIndex("index_SOURCE_TYPE"))
            assertTrue(db.hasIndex("index_SOURCE_RULE_TYPE"))
            assertTrue(db.hasIndex("index_COMIC_SOURCE_CID"))
            assertTrue(db.hasIndex("index_CHAPTER_SOURCE_COMIC"))
            assertTrue(db.hasIndex("index_CHAPTER_SOURCE_COMIC_PATH"))
            assertTrue(db.hasTable("SOURCE_RULE"))
            assertTrue(db.hasTable("SOURCE_HEALTH"))
        }
    }

    private fun Connection.exec(sql: String) {
        createStatement().use { it.execute(sql) }
    }

    private fun Connection.queryLongs(sql: String): List<Long> {
        createStatement().use { statement ->
            statement.executeQuery(sql).use { cursor ->
                val values = ArrayList<Long>()
                while (cursor.next()) values.add(cursor.getLong(1))
                return values
            }
        }
    }

    private fun Connection.queryString(sql: String): String {
        createStatement().use { statement ->
            statement.executeQuery(sql).use { cursor ->
                cursor.next()
                return cursor.getString(1)
            }
        }
    }

    private fun Connection.hasIndex(name: String): Boolean {
        prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?").use { statement ->
            statement.setString(1, name)
            statement.executeQuery().use { cursor ->
                return cursor.next()
            }
        }
    }

    private fun Connection.hasTable(name: String): Boolean {
        prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use { statement ->
            statement.setString(1, name)
            statement.executeQuery().use { cursor ->
                return cursor.next()
            }
        }
    }
}
