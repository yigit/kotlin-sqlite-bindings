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

actual class StmtRef(ptr: Long) : JvmObjRef(ptr), ObjRef

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
        return StmtRef(nativePrepareStmt(dbRef.ptr, stmt))
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
    external fun nativeColumnLong(stmtPtr: Long, index: Int) : Long
    actual fun bindBlob(stmtRef: StmtRef, index: Int, bytes: ByteArray?): ResultCode {
        return nativeBindBlob(stmtRef.ptr, index, bytes)
    }
    external fun nativeBindBlob(stmtPtr: Long, index: Int, bytes: ByteArray?) : ResultCode
    actual fun bindText(stmtRef: StmtRef, index: Int, value: String): ResultCode {
        return nativeBindText(stmtRef.ptr, index, value)
    }
    external fun nativeBindText(stmtPtr: Long, index: Int, value: String) : ResultCode

}