/*
 * Copyright 2020 Google, Inc.
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

import com.birbit.sqlite3.internal.DbRef
import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteApi

class SqliteConnection private constructor(
    private val dbRef: DbRef
) {
    fun prepareStmt(stmt:String) : SqliteStmt {
        return SqliteStmt(this, SqliteApi.prepareStmt(dbRef, stmt))
    }

    fun lastErrorMessage() = SqliteApi.errorMsg(dbRef)

    fun lastErrorCode() = SqliteApi.errorCode(dbRef)

    fun <T> use(block : (SqliteConnection) -> T) : T {
        return try {
            block(this)
        } finally {
            close()
        }
    }

    fun close() {
        check(SqliteApi.close(dbRef) == ResultCode.OK) {
            "failed to close database"
        }
        dbRef.dispose()
    }

    companion object {
        fun openConnection(path : String) = SqliteConnection(
            SqliteApi.openConnection(path)
        )
    }
}