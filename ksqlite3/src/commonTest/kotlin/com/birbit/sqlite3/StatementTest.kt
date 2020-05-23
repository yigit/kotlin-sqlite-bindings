package com.birbit.sqlite3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatementTest {
    @Test
    fun readInt() {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt("SELECT 7")
        stmt.step()
        assertEquals(7, stmt.columnInt(0))
        assertFalse(stmt.columnIsNull(0))
    }

    @Test
    fun readNulls() {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt("SELECT NULL")
        stmt.step()
        assertEquals(0, stmt.columnInt(0))
        assertEquals(null, stmt.columnText(0))
        assertTrue(stmt.columnIsNull(0))
    }

    @Test
    fun readText() {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt("SELECT \"hello\"")
        stmt.step()
        assertEquals("hello", stmt.columnText(0))
        assertFalse(stmt.columnIsNull(0))
    }
}