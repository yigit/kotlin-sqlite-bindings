package com.birbit.sqlite3.internal

import org.scijava.nativelib.NativeLoader

actual class DbRef(
    val dbPtr: Long
)

actual class StmtRef(
    val dbPtr: Long
)

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
        return StmtRef(nativePrepareStmt(dbRef.dbPtr, stmt))
    }

    external fun nativePrepareStmt(dbPtr: Long, stmt: String): Long

    actual fun step(stmtRef: StmtRef): ResultCode {
        return nativeStep(stmtRef.dbPtr)
    }

    external fun nativeStep(stmtPtr: Long): ResultCode

    actual fun columnText(stmtRef: StmtRef, index: Int): String? {
        return nativeColumnText(stmtRef.dbPtr, index)
    }

    external fun nativeColumnText(stmtPtr: Long, index: Int): String?
    actual fun columnInt(stmtRef: StmtRef, index: Int): Int {
        return nativeColumnInt(stmtRef.dbPtr, index)
    }

    external fun nativeColumnInt(stmtPtr: Long, index: Int): Int

    actual fun columnIsNull(stmtRef: StmtRef, index: Int): Boolean {
        return nativeColumnIsNull(stmtRef.dbPtr, index)
    }

    external fun nativeColumnIsNull(stmtPtr: Long, index: Int): Boolean
    actual fun reset(stmtRef: StmtRef): ResultCode {
        return nativeReset(stmtRef.dbPtr)
    }

    external fun nativeReset(stmtPtr: Long): ResultCode
}