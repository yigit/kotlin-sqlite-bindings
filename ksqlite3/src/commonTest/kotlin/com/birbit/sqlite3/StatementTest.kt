/*
 * Copyright 2020 Google, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.birbit.sqlite3

import com.birbit.sqlite3.SqliteStmt.Metadata
import com.birbit.sqlite3.SqliteStmt.Metadata.ColumnInfo
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
        oneRowQuery("SELECT 2.1234") { row ->
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

    @Test
    fun bindInt() {
        query("SELECT ?") { stmt ->
            stmt.bind(1, Int.MIN_VALUE)
            val read = stmt.query().first().readInt(0)
            assertEquals(Int.MIN_VALUE, read)
        }
    }

    @Test
    fun bindLong() {
        query("SELECT ?") { stmt ->
            stmt.bind(1, Long.MIN_VALUE)
            val read = stmt.query().first().readLong(0)
            assertEquals(Long.MIN_VALUE, read)
        }
    }

    @Test
    fun bindNull() {
        query("SELECT ?") { stmt ->
            stmt.bind(1, 3)
            stmt.bindNull(1)
            val read = stmt.query().first().readString(0)
            assertEquals(null, read)
        }
    }

    @Test
    fun bindDouble() {
        query("SELECT ?") { stmt ->
            stmt.bind(1, Double.MAX_VALUE)
            val read = stmt.query().first().readDouble(0)
            assertEquals(Double.MAX_VALUE, read)
        }
    }

    @Test
    fun bindValues() {
        query("VALUES(?, ?, ?, ?, ?, ?, ?, ?)") { stmt ->
            val byteArray = byteArrayOf(1, 2, 3)
            stmt.bindValues(
                listOf(
                    1,
                    Double.MAX_VALUE,
                    Long.MIN_VALUE,
                    null,
                    "a",
                    byteArray,
                    3.14f,
                    0b011
                )
            )
            val row = stmt.query().first()
            assertEquals(1, row.readInt(0))
            assertEquals(Double.MAX_VALUE, row.readDouble(1))
            assertEquals(Long.MIN_VALUE, row.readLong(2))
            assertEquals(null, row.readString(3))
            assertEquals("a", row.readString(4))
            assertEquals(byteArray.joinToString(), row.readByteArray(5)?.joinToString())
            assertEquals(3.14f, row.readDouble(6).toFloat())
            assertEquals(0b011, row.readInt(7))
        }
    }

    @Test
    fun bindValuesInvalid() {
        query("SELECT ?") {
            val result = kotlin.runCatching {
                it.bindValue(0, listOf("a", "b"))
            }
            assertEquals(
                SqliteException(ResultCode.FORMAT, "cannot bind ${listOf("a", "b")}"),
                result.exceptionOrNull()
            )
        }
    }

    @Test
    fun columnTypes() {
        query("VALUES(?, ?, ?, ?, ?)") {
            it.bindNull(1)
            it.bind(2, 1)
            it.bind(3, 3.14)
            it.bind(4, byteArrayOf(0b0, 0b1))
            it.bind(5, "foo")
            it.query().first()
            assertEquals(ColumnType.NULL, it.columnType(0))
            assertEquals(ColumnType.INTEGER, it.columnType(1))
            assertEquals(ColumnType.FLOAT, it.columnType(2))
            assertEquals(ColumnType.BLOB, it.columnType(3))
            assertEquals(ColumnType.STRING, it.columnType(4))
        }
    }

    @Test
    fun metadata() {
        val columns = listOf(
            "colInt" to "Integer",
            "colString" to "String",
            "colText" to "Text",
            "colNumber" to "NUMBER",
            "colBlob" to "BLOB",
            "colDouble" to "Double"
        )
        val metadata = SqliteConnection.openConnection(":memory:").use {
            it.exec(
                """
                CREATE TABLE Test(
                    ${columns.joinToString(",") { (name, type) -> "$name $type" }}
                )
            """.trimIndent()
            )
            it.prepareStmt("SELECT * FROM Test").use {
                it.obtainMetadata()
            }
        }
        assertEquals(Metadata(
            columns = columns.map { (name, type) ->
                ColumnInfo(
                    databaseName = "main",
                    tableName = "Test",
                    originName = name,
                    declaredType = type,
                    name = name
                )
            }
        ), metadata)
    }

    @Test
    fun normalized() {
        query("SELECT * FROM sqlite_master WHERE name = ?") { stmt ->
            val normalized = stmt.normalizedQuery()
            assertEquals("SELECT*FROM sqlite_master WHERE name=?;", normalized)
        }
    }

    @Test
    fun sql() {
        query("SELECT * FROM sqlite_master WHERE name = ?") { stmt ->
            val query = stmt.sql()
            assertEquals("SELECT * FROM sqlite_master WHERE name = ?", query)
        }
    }

    @Test
    fun expanded() {
        query("SELECT * FROM sqlite_master WHERE name = ?") { stmt ->
            stmt.bind(1, "FOO")
            val normalized = stmt.expandedQuery()

            assertEquals("SELECT * FROM sqlite_master WHERE name = 'FOO'", normalized)
        }
    }

    @Test
    fun metadata_noTable() {
        val metadata = SqliteConnection.openConnection(":memory:").use {
            it.prepareStmt("VALUES(1, 3.4)").use {
                it.obtainMetadata()
            }
        }
        assertEquals(
            Metadata(
                columns = listOf(
                    ColumnInfo(
                        name = "column1",
                        databaseName = null,
                        tableName = null,
                        declaredType = null,
                        originName = null
                    ),
                    ColumnInfo(
                        name = "column2",
                        databaseName = null,
                        tableName = null,
                        declaredType = null,
                        originName = null
                    )
                )
            ), metadata
        )
    }

    private fun oneRowQuery(query: String, block: (Row) -> Unit) {
        return query(query) {
            block(it.query().first())
        }
    }

    private fun query(query: String, block: (SqliteStmt) -> Unit) {
        val conn = SqliteConnection.openConnection(":memory:")
        conn.use {
            val stmt = conn.prepareStmt(query)
            stmt.use {
                block(stmt)
            }
        }
    }
}
