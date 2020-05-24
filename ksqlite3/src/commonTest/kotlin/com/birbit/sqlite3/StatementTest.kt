package com.birbit.sqlite3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatementTest {
    @Test
    fun readInt() {
        oneRowQuery("SELECT 7") { row ->
            assertEquals(7, row.readInt(0))
            assertFalse(row.isNull(0))
        }
    }

    @Test
    fun readNulls() {
        oneRowQuery("SELECT NULL") { row ->
            assertEquals(0, row.readInt(0))
            assertEquals(0.0, row.readDouble(0))
            assertEquals(null, row.readString(0))
            assertEquals(null, row.readByteArray(0))
            assertTrue(row.isNull(0))
        }
    }

    @Test
    fun readText() {
        oneRowQuery("SELECT \"hello\"") { row ->
            assertEquals("hello", row.readString(0))
            assertFalse(row.isNull(0))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readBlob() {
        oneRowQuery("SELECT \"Foo bar\"") { row ->
            val bytes = row.readByteArray(0)
            assertEquals("Foo bar", bytes?.decodeToString(), "invalid blob returned")
        }
    }

    @Test
    fun readDouble() {
        oneRowQuery("SELECT 2.1234") {row ->
            assertEquals(2.1234, row.readDouble(0))
        }
    }

    private fun oneRowQuery(query:String, block : (Row) -> Unit) {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt(query)
        stmt.use {
            block(stmt.query().first())
        }
    }
}