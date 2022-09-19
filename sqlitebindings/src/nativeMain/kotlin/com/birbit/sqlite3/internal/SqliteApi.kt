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

import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import com.birbit.sqlite3.AuthorizationParams
import com.birbit.sqlite3.Authorizer
import com.birbit.sqlite3.ColumnType
import com.birbit.sqlite3.ResultCode
import com.birbit.sqlite3.SqliteException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.utf8
import kotlinx.cinterop.value
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_TRANSIENT
import sqlite3.sqlite3_bind_blob
import sqlite3.sqlite3_bind_double
import sqlite3.sqlite3_bind_int
import sqlite3.sqlite3_bind_int64
import sqlite3.sqlite3_bind_null
import sqlite3.sqlite3_bind_parameter_count
import sqlite3.sqlite3_bind_parameter_index
import sqlite3.sqlite3_bind_parameter_name
import sqlite3.sqlite3_bind_text
import sqlite3.sqlite3_close_v2
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_count
import sqlite3.sqlite3_column_database_name
import sqlite3.sqlite3_column_decltype
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_name
import sqlite3.sqlite3_column_origin_name
import sqlite3.sqlite3_column_table_name
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_errcode
import sqlite3.sqlite3_errmsg
import sqlite3.sqlite3_errstr
import sqlite3.sqlite3_exec
import sqlite3.sqlite3_expanded_sql
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_free
import sqlite3.sqlite3_normalized_sql
import sqlite3.sqlite3_open
import sqlite3.sqlite3_prepare_v2
import sqlite3.sqlite3_reset
import sqlite3.sqlite3_set_authorizer
import sqlite3.sqlite3_sql
import sqlite3.sqlite3_step
import kotlin.native.concurrent.AtomicReference

internal class NativeRef<T : Any>(target: T) : ObjRef {
    private val _stableRef: AtomicReference<StableRef<T>?> = AtomicReference(StableRef.create(target))
    val stableRef: StableRef<T>
        get() = checkNotNull(_stableRef.value) {
            "tried to access stable ref after it is disposed"
        }

    override fun dispose() {
        _stableRef.value?.dispose()
        _stableRef.value = null
    }

    override fun isDisposed() = _stableRef.value == null
}

actual class StmtRef(@Suppress("unused") actual val dbRef: DbRef, val rawPtr: CPointer<sqlite3_stmt>) : ObjRef {
    internal val nativeRef = NativeRef(this)
    override fun dispose() {
        nativeRef.dispose()
    }

    override fun isDisposed() = nativeRef.isDisposed()
}

// TODO these two classes are almost identical, should probably commanize as more comes
actual class DbRef(val rawPtr: CPointer<sqlite3>) : ObjRef {
    internal val nativeRef = NativeRef(this)
    internal val authorizer = AtomicReference<NativeRef<Authorizer>?>(null)
    override fun dispose() {
        nativeRef.dispose()
    }

    override fun isDisposed() = nativeRef.isDisposed()
}

actual object SqliteApi {
    actual fun openConnection(path: String): DbRef {
        val ptr = nativeHeap.allocPointerTo<sqlite3>()
        val openResult = sqlite3_open(path, ptr.ptr)
        if (openResult != SQLITE_OK) {
            try {
                throw SqliteException(
                    ResultCode(openResult),
                    "could not open database at path  $path"
                )
            } finally {
                ptr.value?.let {
                    close(DbRef(it))
                }
            }
        }
        return DbRef(ptr.value!!)
    }

    actual fun prepareStmt(
        dbRef: DbRef,
        stmt: String
    ): StmtRef {
        val stmtPtr = nativeHeap.allocPointerTo<sqlite3_stmt>()
        val resultCode = sqlite3_prepare_v2(dbRef.rawPtr, stmt.utf8, -1, stmtPtr.ptr, null)
        checkResultCode(dbRef, resultCode, SQLITE_OK)
        return StmtRef(dbRef, stmtPtr.value!!)
    }

    actual fun step(stmtRef: StmtRef): ResultCode {
        return ResultCode(sqlite3_step(stmtRef.rawPtr))
    }

    actual fun columnText(stmtRef: StmtRef, index: Int): String? {
        val textPtr: CPointer<UByteVar> = sqlite3_column_text(stmtRef.rawPtr, index) ?: return null
        // TODO free C data
        return textPtr.reinterpret<ByteVar>().toKStringFromUtf8()
    }

    actual fun columnInt(stmtRef: StmtRef, index: Int): Int {
        return sqlite3_column_int(stmtRef.rawPtr, index)
    }

    actual fun columnIsNull(stmtRef: StmtRef, index: Int): Boolean {
        return sqlite3_column_type(stmtRef.rawPtr, index) == SQLITE_NULL
    }

    actual fun reset(stmtRef: StmtRef): ResultCode {
        return ResultCode(sqlite3_reset(stmtRef.rawPtr))
    }

    actual fun close(dbRef: DbRef): ResultCode {
        return ResultCode(sqlite3_close_v2(dbRef.rawPtr))
    }

    actual fun finalize(stmtRef: StmtRef): ResultCode {
        return ResultCode(sqlite3_finalize(stmtRef.rawPtr))
    }

    actual fun columnBlob(stmtRef: StmtRef, index: Int): ByteArray? {
        val size = sqlite3_column_bytes(stmtRef.rawPtr, index)
        if (size == 0) {
            return null
        }
        val blob = sqlite3_column_blob(stmtRef.rawPtr, index)
        checkNotNull(blob) {
            "columnBlob for $index is null"
        }
        // TODO do we need to free this blob, figure out
        return blob.readBytes(size)
    }

    actual fun columnDouble(stmtRef: StmtRef, index: Int): Double {
        return sqlite3_column_double(stmtRef.rawPtr, index)
    }

    actual fun columnLong(stmtRef: StmtRef, index: Int): Long {
        return sqlite3_column_int64(stmtRef.rawPtr, index)
    }

    actual fun bindBlob(stmtRef: StmtRef, index: Int, bytes: ByteArray): ResultCode {
        val resultCode = bytes.usePinned {
            sqlite3_bind_blob(stmtRef.rawPtr, index, it.addressOf(0), bytes.size, SQLITE_TRANSIENT)
        }
        return ResultCode(resultCode)
    }

    actual fun bindText(stmtRef: StmtRef, index: Int, value: String): ResultCode {
        val resultCode = sqlite3_bind_text(stmtRef.rawPtr, index, value, -1, SQLITE_TRANSIENT)
        return ResultCode(resultCode)
    }

    actual fun bindInt(stmtRef: StmtRef, index: Int, value: Int): ResultCode {
        val resultCode = sqlite3_bind_int(stmtRef.rawPtr, index, value)
        return ResultCode(resultCode)
    }

    actual fun bindLong(stmtRef: StmtRef, index: Int, value: Long): ResultCode {
        val resultCode = sqlite3_bind_int64(stmtRef.rawPtr, index, value)
        return ResultCode(resultCode)
    }

    actual fun bindNull(stmtRef: StmtRef, index: Int): ResultCode {
        val resultCode = sqlite3_bind_null(stmtRef.rawPtr, index)
        return ResultCode(resultCode)
    }

    actual fun bindDouble(stmtRef: StmtRef, index: Int, value: Double): ResultCode {
        val resultCode = sqlite3_bind_double(stmtRef.rawPtr, index, value)
        return ResultCode(resultCode)
    }

    actual fun errorMsg(dbRef: DbRef): String? {
        return sqlite3_errmsg(dbRef.rawPtr)?.toKStringFromUtf8()
    }

    actual fun errorCode(dbRef: DbRef): ResultCode {
        return ResultCode(sqlite3_errcode(dbRef.rawPtr))
    }

    actual fun errorString(code: ResultCode): String? {
        return sqlite3_errstr(code.value)?.toKStringFromUtf8()
    }

    actual fun setAuthorizer(
        dbRef: DbRef,
        authorizer: Authorizer?
    ): ResultCode {
        val (authRef, resultCode) = if (authorizer != null) {
            val authRef = NativeRef(authorizer)
            val resultCode = sqlite3_set_authorizer(
                dbRef.rawPtr,
                staticCFunction(::callAuthCallback),
                authRef.stableRef.asCPointer()
            )
            authRef to resultCode
        } else {
            null to sqlite3_set_authorizer(dbRef.rawPtr, null, null)
        }
        checkResultCode(dbRef, resultCode, SQLITE_OK)
        // dispose previous one if exists
        val previous = dbRef.authorizer.value
        previous?.let {
            it.stableRef.get().dispose()
            it.dispose()
        }
        dbRef.authorizer.value = authRef
        return ResultCode(SQLITE_OK)
    }

    actual fun columnType(
        stmtRef: StmtRef,
        index: Int
    ): ColumnType {
        val result = sqlite3_column_type(stmtRef.rawPtr, index)
        return ColumnType(result)
    }

    actual fun exec(
        dbRef: DbRef,
        query: String
    ): ResultCode {
        val resultCode = sqlite3_exec(dbRef.rawPtr, query, null, null, null)
        checkResultCode(dbRef, resultCode, SQLITE_OK)
        return ResultCode(resultCode)
    }

    actual fun columnDeclType(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_column_decltype(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun columnDatabaseName(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_column_database_name(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun columnTableName(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_column_table_name(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun columnOriginName(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_column_origin_name(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun columnCount(stmtRef: StmtRef): Int {
        return sqlite3_column_count(stmtRef.rawPtr)
    }

    actual fun columnName(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_column_name(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun expandedSql(stmtRef: StmtRef): String {
        return checkNotNull(
            sqlite3_expanded_sql(stmtRef.rawPtr)?.let {
                val kstring = it.toKStringFromUtf8()
                sqlite3_free(it)
                kstring
            }
        )
    }

    actual fun normalizedSql(stmtRef: StmtRef): String {
        return checkNotNull(
            sqlite3_normalized_sql(stmtRef.rawPtr)?.toKStringFromUtf8()
        )
    }

    actual fun sql(stmtRef: StmtRef): String {
        return checkNotNull(
            sqlite3_sql(stmtRef.rawPtr)?.toKStringFromUtf8()
        )
    }

    actual fun bindParameterCount(stmtRef: StmtRef): Int {
        return sqlite3_bind_parameter_count(stmtRef.rawPtr)
    }

    actual fun bindParameterName(stmtRef: StmtRef, index: Int): String? {
        return sqlite3_bind_parameter_name(stmtRef.rawPtr, index)?.toKStringFromUtf8()
    }

    actual fun bindParameterIndex(stmtRef: StmtRef, name: String): Int {
        return sqlite3_bind_parameter_index(stmtRef.rawPtr, name)
    }
}

fun callAuthCallback(
    authorizer: COpaquePointer?,
    actionCode: Int,
    param1: CPointer<ByteVar>?,
    param2: CPointer<ByteVar>?,
    param3: CPointer<ByteVar>?,
    param4: CPointer<ByteVar>?
): Int {
    StableRef
    val auth = authorizer!!.asStableRef<Authorizer>().get()
    return auth(
        AuthorizationParams(
            actionCode = actionCode,
            param1 = param1?.toKStringFromUtf8(),
            param2 = param2?.toKStringFromUtf8(),
            param3 = param3?.toKStringFromUtf8(),
            param4 = param4?.toKStringFromUtf8()
        )
    ).value
}

internal actual fun loadNativeLibrary() {
    // no-op
}
