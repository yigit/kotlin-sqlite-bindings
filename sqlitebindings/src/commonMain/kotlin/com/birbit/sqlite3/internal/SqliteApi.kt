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
package com.birbit.sqlite3.internal

// commonized sqlite APIs to build the rest in common, or most at least
inline class ResultCode(val value: Int) {
    fun errorString() = SqliteApi.errorString(this)

    companion object {
        val OK = ResultCode(0) /* Successful result */
        val ERROR = ResultCode(1) /* Generic error */
        val INTERNAL = ResultCode(2) /* Internal logic error in SQLite */
        val PERM = ResultCode(3) /* Access permission denied */
        val ABORT = ResultCode(4) /* Callback routine requested an abort */
        val BUSY = ResultCode(5) /* The database file is locked */
        val LOCKED = ResultCode(6) /* A table in the database is locked */
        val NOMEM = ResultCode(7) /* A malloc() failed */
        val READONLY = ResultCode(8) /* Attempt to write a readonly database */
        val INTERRUPT = ResultCode(9) /* Operation terminated by sqlite3_interrupt()*/
        val IOERR = ResultCode(10) /* Some kind of disk I/O error occurred */
        val CORRUPT = ResultCode(11) /* The database disk image is malformed */
        val NOTFOUND = ResultCode(12) /* Unknown opcode in sqlite3_file_control() */
        val FULL = ResultCode(13) /* Insertion failed because database is full */
        val CANTOPEN = ResultCode(14) /* Unable to open the database file */
        val PROTOCOL = ResultCode(15) /* Database lock protocol error */
        val EMPTY = ResultCode(16) /* Internal use only */
        val SCHEMA = ResultCode(17) /* The database schema changed */
        val TOOBIG = ResultCode(18) /* String or BLOB exceeds size limit */
        val CONSTRAINT = ResultCode(19) /* Abort due to constraint violation */
        val MISMATCH = ResultCode(20) /* Data type mismatch */
        val MISUSE = ResultCode(21) /* Library used incorrectly */
        val NOLFS = ResultCode(22) /* Uses OS features not supported on host */
        val AUTH = ResultCode(23) /* Authorization denied */
        val FORMAT = ResultCode(24) /* Not used */
        val RANGE = ResultCode(25) /* 2nd parameter to sqlite3_bind out of range */
        val NOTADB = ResultCode(26) /* File opened that is not a database file */
        val NOTICE = ResultCode(27) /* Notifications from sqlite3_log() */
        val WARNING = ResultCode(28) /* Warnings from sqlite3_log() */
        val ROW = ResultCode(100) /* sqlite3_step() has another row ready */
        val DONE = ResultCode(101) /* sqlite3_step() has finished executing */
    }
}

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
    fun bindNull(stmtRef: StmtRef, index: Int): ResultCode
    fun errorMsg(dbRef: DbRef): String?
    fun errorCode(dbRef: DbRef): ResultCode
    fun errorString(code: ResultCode): String?
}
