package com.birbit.sqlite3

import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteException
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun readErrors() {
        SqliteConnection.openConnection(":memory:").use {
            val result = kotlin.runCatching {
                it.prepareStmt("SELECT * FROM nonExistingTable")
            }

            assertEquals(result.exceptionOrNull(), SqliteException(
                resultCode = ResultCode.ERROR,
                msg = "no such table: nonExistingTable"
            ))
            assertEquals(it.lastErrorCode(), ResultCode.ERROR)
            assertEquals(it.lastErrorMessage(), "no such table: nonExistingTable")
        }
    }
}