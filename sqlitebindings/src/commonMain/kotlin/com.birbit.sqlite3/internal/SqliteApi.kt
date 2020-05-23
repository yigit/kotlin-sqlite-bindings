package com.birbit.sqlite3.internal

// commonized sqlite APIs to build the rest in common, or most at least
inline class ResultCode(val value: Int)
expect class DbRef
expect class StmtRef

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
    fun columnIsNull(stmtRef: StmtRef, index: Int) : Boolean
    fun columnText(stmtRef: StmtRef, index: Int): String?
    fun columnInt(stmtRef: StmtRef, index: Int): Int
}