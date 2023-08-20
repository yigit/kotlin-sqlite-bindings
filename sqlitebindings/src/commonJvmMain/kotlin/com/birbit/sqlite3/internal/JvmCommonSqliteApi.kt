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

open class JvmObjRef(
    ptr: Long
) : ObjRef {
    var ptr: Long = ptr
        private set

    override fun dispose() {
        ptr = 0
    }

    override fun isDisposed() = ptr == 0L
}

actual class DbRef(ptr: Long) : JvmObjRef(ptr), ObjRef

actual class StmtRef(actual val dbRef: DbRef, ptr: Long) : JvmObjRef(ptr), ObjRef

@Suppress("INAPPLICABLE_JVM_NAME")
actual object SqliteApi {
    // for all native jvm name methods, see: https://youtrack.jetbrains.com/issue/KT-28135
    init {
        loadNativeLibrary()
    }

    actual fun openConnection(path: String): DbRef {
        return DbRef(nativeOpenConnection(path))
    }

    external fun nativeOpenConnection(path: String): Long

    actual fun prepareStmt(
        dbRef: DbRef,
        stmt: String
    ): StmtRef {
        return StmtRef(dbRef, nativePrepareStmt(dbRef.ptr, stmt))
    }

    external fun nativePrepareStmt(ptr: Long, stmt: String): Long

    actual fun step(stmtRef: StmtRef): ResultCode {
        return nativeStep(stmtRef.ptr)
    }

    @JvmName("nativeStep")
    external fun nativeStep(stmtPtr: Long): ResultCode

    actual fun columnText(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnText(stmtRef.ptr, index)
    }

    external fun nativeColumnText(stmtPtr: Long, index: Int): String?
    actual fun columnInt(stmtRef: StmtRef, index: Int): Int {
        return nativeColumnInt(stmtRef.ptr, index)
    }

    external fun nativeColumnInt(stmtPtr: Long, index: Int): Int

    actual fun columnIsNull(stmtRef: StmtRef, index: Int): Boolean {
        return nativeColumnIsNull(stmtRef.ptr, index)
    }

    external fun nativeColumnIsNull(stmtPtr: Long, index: Int): Boolean
    actual fun reset(stmtRef: StmtRef): ResultCode {
        return nativeReset(stmtRef.ptr)
    }

    @JvmName("nativeReset")
    external fun nativeReset(stmtPtr: Long): ResultCode
    actual fun close(dbRef: DbRef): ResultCode {
        return nativeClose(dbRef.ptr)
    }

    @JvmName("nativeClose")
    external fun nativeClose(ptr: Long): ResultCode

    actual fun finalize(stmtRef: StmtRef): ResultCode {
        return nativeFinalize(stmtRef.ptr)
    }

    @JvmName("nativeFinalize")
    external fun nativeFinalize(stmtPtr: Long): ResultCode

    actual fun columnBlob(stmtRef: StmtRef, index: Int): ByteArray? {
        return nativeColumnBlob(stmtRef.ptr, index)
    }

    external fun nativeColumnBlob(stmtPtr: Long, index: Int): ByteArray?

    actual fun columnDouble(stmtRef: StmtRef, index: Int): Double {
        return nativeColumnDouble(stmtRef.ptr, index)
    }

    external fun nativeColumnDouble(stmtPtr: Long, index: Int): Double
    actual fun columnLong(stmtRef: StmtRef, index: Int): Long {
        return nativeColumnLong(stmtRef.ptr, index)
    }

    external fun nativeColumnLong(stmtPtr: Long, index: Int): Long
    actual fun bindBlob(stmtRef: StmtRef, index: Int, bytes: ByteArray): ResultCode {
        return nativeBindBlob(stmtRef.ptr, index, bytes)
    }

    @JvmName("nativeBindBlob")
    external fun nativeBindBlob(stmtPtr: Long, index: Int, bytes: ByteArray): ResultCode

    actual fun bindText(stmtRef: StmtRef, index: Int, value: String): ResultCode {
        return nativeBindText(stmtRef.ptr, index, value)
    }

    @JvmName("nativeBindText")
    external fun nativeBindText(stmtPtr: Long, index: Int, value: String): ResultCode
    actual fun bindInt(stmtRef: StmtRef, index: Int, value: Int): ResultCode {
        return nativeBindInt(stmtRef.ptr, index, value)
    }

    @JvmName("nativeBindInt")
    external fun nativeBindInt(stmtPtr: Long, index: Int, value: Int): ResultCode

    actual fun bindLong(stmtRef: StmtRef, index: Int, value: Long): ResultCode {
        return nativeBindLong(stmtRef.ptr, index, value)
    }

    @JvmName("nativeBindLong")
    external fun nativeBindLong(stmtPtr: Long, index: Int, value: Long): ResultCode

    actual fun bindNull(stmtRef: StmtRef, index: Int): ResultCode {
        return nativeBindNull(stmtRef.ptr, index)
    }

    @JvmName("nativeBindNull")
    external fun nativeBindNull(stmtPtr: Long, index: Int): ResultCode

    actual fun errorMsg(dbRef: DbRef): String? {
        return nativeErrorMsg(dbRef.ptr)
    }

    external fun nativeErrorMsg(dbPtr: Long): String?

    actual fun errorCode(dbRef: DbRef): ResultCode {
        return nativeErrorCode(dbRef.ptr)
    }

    @JvmName("nativeErrorCode")
    external fun nativeErrorCode(dbPtr: Long): ResultCode

    actual fun errorString(code: ResultCode): String? {
        return nativeErrorString(code)
    }

    @JvmName("nativeErrorString")
    external fun nativeErrorString(code: ResultCode): String?

    actual fun bindDouble(stmtRef: StmtRef, index: Int, value: Double): ResultCode {
        return nativeBindDouble(stmtRef.ptr, index, value)
    }

    @JvmName("nativeBindDouble")
    external fun nativeBindDouble(stmtPtr: Long, index: Int, value: Double): ResultCode
    actual fun setAuthorizer(
        dbRef: DbRef,
        authorizer: Authorizer?
    ): ResultCode {
        return nativeSetAuthorizer(dbRef.ptr, authorizer)
    }

    @JvmName("nativeSetAuthorizer")
    external fun nativeSetAuthorizer(dbPtr: Long, authorizer: Authorizer?): ResultCode
    actual fun columnType(
        stmtRef: StmtRef,
        index: Int
    ): ColumnType {
        return nativeColumnType(stmtRef.ptr, index)
    }

    @JvmName("nativeColumnType")
    external fun nativeColumnType(stmtPtr: Long, index: Int): ColumnType
    actual fun exec(
        dbRef: DbRef,
        query: String
    ): ResultCode {
        return nativeExec(dbRef.ptr, query)
    }

    @JvmName("nativeExec")
    external fun nativeExec(dbPtr: Long, query: String): ResultCode
    actual fun columnDeclType(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnDeclType(stmtRef.ptr, index)
    }

    external fun nativeColumnDeclType(stmtPtr: Long, index: Int): String?

    actual fun columnDatabaseName(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnDatabaseName(stmtRef.ptr, index)
    }

    external fun nativeColumnDatabaseName(stmtPtr: Long, index: Int): String?

    actual fun columnTableName(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnTableName(stmtRef.ptr, index)
    }

    external fun nativeColumnTableName(stmtPtr: Long, index: Int): String?

    actual fun columnOriginName(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnOriginName(stmtRef.ptr, index)
    }

    external fun nativeColumnOriginName(stmtPtr: Long, index: Int): String?
    actual fun columnCount(stmtRef: StmtRef): Int {
        return nativeColumnCount(stmtRef.ptr)
    }

    external fun nativeColumnCount(ptr: Long): Int

    actual fun columnName(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnName(stmtRef.ptr, index)
    }

    external fun nativeColumnName(stmtPtr: Long, index: Int): String?
    actual fun expandedSql(stmtRef: StmtRef): String {
        return nativeExpandedSql(stmtRef.ptr)
    }

    external fun nativeExpandedSql(stmtPtr: Long): String

    actual fun normalizedSql(stmtRef: StmtRef): String {
        return nativeNormalizedSql(stmtRef.ptr)
    }

    external fun nativeNormalizedSql(ptr: Long): String
    actual fun sql(stmtRef: StmtRef): String {
        return nativeSql(stmtRef.ptr)
    }

    external fun nativeSql(stmtPtr: Long): String
    actual fun bindParameterCount(stmtRef: StmtRef): Int {
        return nativeBindParameterCount(stmtRef.ptr)
    }

    external fun nativeBindParameterCount(stmtPtr: Long): Int

    actual fun bindParameterName(stmtRef: StmtRef, index: Int): String? {
        return nativeBindParameterName(stmtRef.ptr, index)
    }

    external fun nativeBindParameterName(stmtPtr: Long, index: Int): String?

    actual fun bindParameterIndex(stmtRef: StmtRef, name: String): Int {
        return nativeBindParameterIndex(stmtRef.ptr, name)
    }

    external fun nativeBindParameterIndex(stmtPtr: Long, name: String): Int
}
