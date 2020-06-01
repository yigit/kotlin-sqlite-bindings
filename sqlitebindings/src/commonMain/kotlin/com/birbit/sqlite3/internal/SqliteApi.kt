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
package com.birbit.sqlite3.internal

import com.birbit.sqlite3.Authorizer
import com.birbit.sqlite3.ColumnType
import com.birbit.sqlite3.ResultCode
import com.birbit.sqlite3.SqliteException

// TODO trying to use ResultCode here crashed the compiler, hence using Ints
internal inline fun checkResultCode(
    dbRef: DbRef,
    received: Int,
    expected: Int
) {
    if (received != expected) {
        throw SqliteException.buildFromConnection(dbRef, received)
    }
}

internal fun SqliteException.Companion.buildFromConnection(dbRef: DbRef, errorCode: Int?): SqliteException {
    return SqliteException(
        resultCode = errorCode?.let { ResultCode(it) } ?: SqliteApi.errorCode(dbRef),
        msg = SqliteApi.errorMsg(dbRef) ?: errorCode?.let { SqliteApi.errorString(ResultCode(it)) }
        ?: "unknown error"
    )
}
fun ResultCode.errorString() = SqliteApi.errorString(this)

interface ObjRef {
    fun dispose()
    fun isDisposed(): Boolean
}

expect class DbRef : ObjRef

expect class StmtRef : ObjRef {
    val dbRef: DbRef
}

/**
 * Common API for all calls.
 *
 * Native implements this by directly calling SQLite.
 * JVM implements this via JNI which delegates to the native implementation.
 */
expect object SqliteApi {
    fun openConnection(path: String): DbRef
    fun prepareStmt(dbRef: DbRef, stmt: String): StmtRef
    fun step(stmtRef: StmtRef): ResultCode
    fun columnIsNull(stmtRef: StmtRef, index: Int): Boolean
    fun columnText(stmtRef: StmtRef, index: Int): String?
    fun columnInt(stmtRef: StmtRef, index: Int): Int
    fun columnBlob(stmtRef: StmtRef, index: Int): ByteArray?
    fun columnDouble(stmtRef: StmtRef, index: Int): Double
    fun columnLong(stmtRef: StmtRef, index: Int): Long
    fun reset(stmtRef: StmtRef): ResultCode
    fun close(dbRef: DbRef): ResultCode
    fun finalize(stmtRef: StmtRef): ResultCode
    fun bindBlob(stmtRef: StmtRef, index: Int, bytes: ByteArray): ResultCode
    fun bindText(stmtRef: StmtRef, index: Int, value: String): ResultCode
    fun bindInt(stmtRef: StmtRef, index: Int, value: Int): ResultCode
    fun bindLong(stmtRef: StmtRef, index: Int, value: Long): ResultCode
    fun bindDouble(stmtRef: StmtRef, index: Int, value: Double): ResultCode
    fun bindNull(stmtRef: StmtRef, index: Int): ResultCode
    fun errorMsg(dbRef: DbRef): String?
    fun errorCode(dbRef: DbRef): ResultCode
    fun errorString(code: ResultCode): String?
    fun setAuthorizer(dbRef: DbRef, authorizer: Authorizer?): ResultCode
    fun columnType(stmtRef: StmtRef, index: Int): ColumnType
    fun exec(dbRef: DbRef, query: String): ResultCode
    fun columnDeclType(stmtRef: StmtRef, index: Int): String?
    fun columnDatabaseName(stmtRef: StmtRef, index: Int): String?
    fun columnTableName(stmtRef: StmtRef, index: Int): String?
    fun columnOriginName(stmtRef: StmtRef, index: Int): String?
    fun columnCount(stmtRef: StmtRef): Int
    fun columnName(stmtRef: StmtRef, index: Int): String?
    fun expandedSql(stmtRef: StmtRef): String
    fun normalizedSql(stmtRef: StmtRef): String
    fun sql(stmtRef: StmtRef): String
}
