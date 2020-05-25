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
    fun readLong() {
        val value = Long.MAX_VALUE
        oneRowQuery("SELECT $value") { row ->
            assertEquals(value, row.readLong(0))
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

    @Test
    fun bindBlob() {
        query("SELECT ?") { stmt ->
            val byteArray = byteArrayOf(0b0, 0b1, 0b0, 0b1)
            stmt.bind(1, byteArray)
            val read = stmt.query().first().readByteArray(0)
            assertEquals(byteArray.joinToString(), read!!.joinToString())
        }
    }

    @Test
    fun bindString() {
        query("SELECT ?") { stmt ->
            stmt.bind(1, "Foo baz")
            val read = stmt.query().first().readString(0)
            assertEquals("Foo baz", read)
        }
    }

    private fun oneRowQuery(query:String, block : (Row) -> Unit) {
        return query(query) {
            block(it.query().first())
        }
    }

    private fun query(query:String, block : (SqliteStmt) -> Unit) {
        val conn = SqliteConnection.openConnection(":memory:")
        val stmt = conn.prepareStmt(query)
        stmt.use {
            block(stmt)
        }
    }
}