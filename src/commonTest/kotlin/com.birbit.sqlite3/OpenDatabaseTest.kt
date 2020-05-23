package com.birbit.sqlite3

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenDatabaseTest {
    @Test
    fun openDb() {
        SqliteConnection.openConnection(":memory:")
        // cool
    }

    @Test
    fun prepareStmt() {
        val connection = SqliteConnection.openConnection(":memory:")
        val stmt = connection.prepareStmt("SELECT \"abc\"")
        stmt.step()
        assertEquals("abc", stmt.columnText(0))
    }
}