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
}