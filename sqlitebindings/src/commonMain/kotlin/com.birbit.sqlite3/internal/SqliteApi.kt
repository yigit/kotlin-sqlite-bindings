package com.birbit.sqlite3.internal

// commonized sqlite APIs to build the rest in common, or most at least
inline class ResultCode(val value: Int) {
    companion object {
        val OK = ResultCode(0)
        val ROW = ResultCode(100)
    }
}

interface ObjRef {
    fun dispose()
    fun isDisposed(): Boolean
}

expect class DbRef : ObjRef {
}

expect class StmtRef : ObjRef {
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
    fun columnLong(stmtRef: StmtRef, index:Int) : Long
    fun reset(stmtRef: StmtRef): ResultCode
    fun close(dbRef: DbRef): ResultCode
    fun finalize(stmtRef: StmtRef): ResultCode
}