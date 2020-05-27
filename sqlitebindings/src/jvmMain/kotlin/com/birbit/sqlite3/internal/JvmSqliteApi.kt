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

import org.scijava.nativelib.NativeLoader

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

actual object SqliteApi {
    init {
        NativeLoader.loadLibrary("sqlite3jni")
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

    external fun nativeReset(stmtPtr: Long): ResultCode
    actual fun close(dbRef: DbRef): ResultCode {
        return nativeClose(dbRef.ptr)
    }

    external fun nativeClose(ptr: Long): ResultCode

    actual fun finalize(stmtRef: StmtRef): ResultCode {
        return nativeFinalize(stmtRef.ptr)
    }

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

    external fun nativeBindBlob(stmtPtr: Long, index: Int, bytes: ByteArray): ResultCode
    actual fun bindText(stmtRef: StmtRef, index: Int, value: String): ResultCode {
        return nativeBindText(stmtRef.ptr, index, value)
    }

    external fun nativeBindText(stmtPtr: Long, index: Int, value: String): ResultCode
    actual fun bindInt(stmtRef: StmtRef, index: Int, value: Int): ResultCode {
        return nativeBindInt(stmtRef.ptr, index, value)
    }

    external fun nativeBindInt(stmtPtr: Long, index: Int, value: Int): ResultCode

    actual fun bindLong(stmtRef: StmtRef, index: Int, value: Long): ResultCode {
        return nativeBindLong(stmtRef.ptr, index, value)
    }

    external fun nativeBindLong(stmtPtr: Long, index: Int, value: Long): ResultCode

    actual fun bindNull(stmtRef: StmtRef, index: Int): ResultCode {
        return nativeBindNull(stmtRef.ptr, index)
    }

    external fun nativeBindNull(stmtPtr: Long, index: Int): ResultCode
    actual fun errorMsg(dbRef: DbRef): String? {
        return nativeErrorMsg(dbRef.ptr)
    }

    external fun nativeErrorMsg(dbPtr: Long): String?

    actual fun errorCode(dbRef: DbRef): ResultCode {
        return nativeErrorCode(dbRef.ptr)
    }

    external fun nativeErrorCode(dbPtr: Long): ResultCode

    actual fun errorString(code: ResultCode): String? {
        return nativeErrorString(code)
    }

    external fun nativeErrorString(code: ResultCode): String?
    actual fun bindDouble(stmtRef: StmtRef, index: Int, value: Double): ResultCode {
        return nativeBindDouble(stmtRef.ptr, index, value)
    }

    external fun nativeBindDouble(stmtPtr: Long, index: Int, value: Double): ResultCode
}
