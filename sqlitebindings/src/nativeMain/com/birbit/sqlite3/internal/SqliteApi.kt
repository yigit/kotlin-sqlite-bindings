package com.birbit.sqlite3.internal

import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import com.birbit.jni.jlong
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.toLong
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.utf8
import kotlinx.cinterop.value
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.sqlite3_bind_blob
import sqlite3.sqlite3_bind_null
import sqlite3.sqlite3_close
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_errmsg
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_open
import sqlite3.sqlite3_prepare_v2
import sqlite3.sqlite3_reset
import sqlite3.sqlite3_step

private inline fun <reified T : Any> jlong.castFromJni(): T {
    val ptr: COpaquePointer = this.toCPointer<CPointed>()!!
    return ptr.asStableRef<T>().get()
}

private inline fun <reified T : Any> StableRef<T>.toJni() = this.asCPointer().toLong()

private class NativeRef<T : Any>(target: T) : ObjRef {
    private var _stableRef: StableRef<T>? = StableRef.create(target)
    val stableRef: StableRef<T>
        get() = checkNotNull(_stableRef) {
            "tried to access stable ref after it is disposed"
        }

    override fun dispose() {
        _stableRef?.dispose()
        _stableRef = null
    }

    override fun isDisposed() = _stableRef == null

}

actual class StmtRef(val rawPtr: CPointer<sqlite3_stmt>) : ObjRef {
    private val nativeRef = NativeRef(this)
    fun toJni() = nativeRef.stableRef.toJni()

    companion object {
        fun fromJni(jlong: jlong): StmtRef = jlong.castFromJni()
    }

    override fun dispose() {
        nativeRef.dispose()
    }

    override fun isDisposed() = nativeRef.isDisposed()
}

// TODO these two classes are almost idential, should probably commanize as more comes
actual class DbRef(val rawPtr: CPointer<sqlite3>) : ObjRef {
    private val nativeRef = NativeRef(this)
    fun toJni() = nativeRef.stableRef.toJni()

    companion object {
        fun fromJni(jlong: jlong): DbRef = jlong.castFromJni()
    }

    override fun dispose() {
        nativeRef.dispose()
    }

    override fun isDisposed() = nativeRef.isDisposed()
}

actual object SqliteApi {
    actual fun openConnection(path: String): DbRef {
        val ptr = nativeHeap.allocPointerTo<sqlite3>()
        val openResult = sqlite3_open(path, ptr.ptr)
        check(openResult == SQLITE_OK) {
            "could not open database $openResult $path ${sqlite3_errmsg(ptr.value)?.toKStringFromUtf8()}"
        }
        return DbRef(ptr.value!!)
    }

    actual fun prepareStmt(
        dbRef: DbRef,
        stmt: String
    ): StmtRef {
        val stmtPtr = nativeHeap.allocPointerTo<sqlite3_stmt>()
        val resultCode = sqlite3_prepare_v2(dbRef.rawPtr, stmt.utf8, -1, stmtPtr.ptr, null)
        check(resultCode == SQLITE_OK) {
            "cannot prepare statement $stmt"
        }
        return StmtRef(stmtPtr.value!!)
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
        return ResultCode(sqlite3_close(dbRef.rawPtr))
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

    actual fun bindBlob(stmtRef: StmtRef, index: Int, bytes : ByteArray?) : ResultCode {
        val resultCode = if (bytes == null) {
            sqlite3_bind_null(stmtRef.rawPtr, index)
        } else {
            bytes.usePinned {
                sqlite3_bind_blob(stmtRef.rawPtr, index, it.addressOf(0),bytes.size, null)
            }
        }
        return ResultCode(resultCode)
    }
}