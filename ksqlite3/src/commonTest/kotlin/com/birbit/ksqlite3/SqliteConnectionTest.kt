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
package com.birbit.ksqlite3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqliteConnectionTest {
    @Test
    fun openInMemory() {
        val conn = SqliteConnection.openConnection(":memory:")
        conn.close()
    }

    @Test
    fun openInvalidPath() {
        val result = kotlin.runCatching {
            SqliteConnection.openConnection("/::")
        }
        assertEquals(
            result.exceptionOrNull(), SqliteException(
                ResultCode.CANTOPEN,
                "could not open database at path  /::"
            )
        )
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

            assertEquals(
                result.exceptionOrNull(), SqliteException(
                    resultCode = ResultCode.ERROR,
                    msg = "no such table: nonExistingTable"
                )
            )
            assertEquals(it.lastErrorCode(), ResultCode.ERROR)
            assertEquals(it.lastErrorMessage(), "no such table: nonExistingTable")
        }
    }

    @Test
    fun oneTimeQuery() {
        val res = SqliteConnection.openConnection(":memory:").use {
            it.query("SELECT ?, ?", listOf(3, "a")) {
                it.first().let {
                    it.readInt(0) to it.readString(1)
                }
            }
        }
        assertEquals(res, 3 to "a")
    }

    @Test
    fun authCallback() {
        val auth1 = LoggingAuthCallback()
        val auth2 = LoggingAuthCallback()
        SqliteConnection.openConnection(":memory:").use { conn ->
            conn.setAuthCallback(auth1::invoke)
            conn.prepareStmt("SELECT * from sqlite_master").use { }
            auth1.assertParam("sqlite_master")
            auth1.clear()
            conn.setAuthCallback(auth2::invoke)
            conn.prepareStmt("SELECT * from sqlite_master").use { }
            auth2.assertParam("sqlite_master")
            auth1.assertEmpty()
            conn.clearAuthCallback()
            auth2.clear()
            conn.prepareStmt("SELECT * from sqlite_master").use { }
            auth1.assertEmpty()
            auth2.assertEmpty()
        }
    }

    @Test
    fun readEmptyTable() {
        SqliteConnection.openConnection(":memory:").use {
            it.exec("CREATE TABLE Test(id Int)")
            val result = it.prepareStmt("SELECT * FROM Test").use {
                it.query().toList()
            }
            assertEquals(result, emptyList<Row>())
        }
    }

    private class LoggingAuthCallback {
        val allParams = mutableSetOf<String>()
        fun invoke(params: AuthorizationParams): AuthResult {
            allParams.addAll(listOfNotNull(params.param1, params.param2, params.param3, params.param4))
            return AuthResult.OK
        }

        fun assertParam(param: String) {
            assertTrue(allParams.contains(param), "missing $param in $allParams")
        }

        fun assertEmpty() {
            assertEquals(allParams, emptySet<String>(), "this should be empty but have $allParams")
        }

        fun clear() {
            allParams.clear()
        }
    }
}
