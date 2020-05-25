package com.birbit.sqlite3

import kotlin.test.Test
import kotlin.test.assertTrue

class OpenDatabaseTest {
    @Test
    fun openInMemory() {
        val conn = SqliteConnection.openConnection(":memory:")
        conn.close()
    }

    @Test
    fun openOnDisk() {
        withTmpFolder {
            val dbPath = getFilePath("testdb.db")
            val connection = SqliteConnection.openConnection(dbPath)
            connection.close()
            assertTrue(PlatformTestUtils.fileExists(dbPath), "no file in $dbPath")
        }
    }
}