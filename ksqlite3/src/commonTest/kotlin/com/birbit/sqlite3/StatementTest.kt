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
        val row = stmt.query().first()
        assertEquals(7, row.readInt(0))
        assertFalse(row.isNull(0))
    }

    @Test
    fun readNulls() {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt("SELECT NULL")
        val row = stmt.query().first()
        assertEquals(0, row.readInt(0))
        assertEquals(null, row.readString(0))
        assertTrue(row.isNull(0))
    }

    @Test
    fun readText() {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt("SELECT \"hello\"")
        val row = stmt.query().first()
        assertEquals("hello", row.readString(0))
        assertFalse(row.isNull(0))
    }
}