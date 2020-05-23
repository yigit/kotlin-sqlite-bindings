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
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.toLong
import kotlinx.cinterop.utf8
import kotlinx.cinterop.value
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.sqlite3_column_int
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_open
import sqlite3.sqlite3_prepare_v2
import sqlite3.sqlite3_step

private inline fun <reified T : Any> jlong.castFromJni(): T {
    val ptr: COpaquePointer = this.toCPointer<CPointed>()!!
    return ptr.asStableRef<T>().get()
}

private inline fun <reified T : Any> StableRef<T>.toJni() = this.asCPointer().toLong()

actual class StmtRef(val rawPtr: CPointer<sqlite3_stmt>) {
    private val stableRef = StableRef.create(this)
    fun toJni() = stableRef.toJni()

    companion object {
        fun fromJni(jlong: jlong): StmtRef = jlong.castFromJni()
    }
}

actual class DbRef(val rawPtr: CPointer<sqlite3>) {
    private val stableRef = StableRef.create(this)
    fun toJni() = stableRef.toJni()

    companion object {
        fun fromJni(jlong: jlong): DbRef = jlong.castFromJni()
    }
}

actual object SqliteApi {
    actual fun openConnection(path: String): DbRef {
        val ptr = nativeHeap.allocPointerTo<sqlite3>()
        val openResult = sqlite3_open(":memory:", ptr.ptr)
        check(openResult == SQLITE_OK) {
            "could not open database $openResult"
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
        return textPtr.reinterpret<ByteVar>().toKStringFromUtf8()
    }

    actual fun columnInt(stmtRef: StmtRef, index: Int): Int {
        return sqlite3_column_int(stmtRef.rawPtr, index)
    }

    actual fun columnIsNull(stmtRef: StmtRef, index: Int): Boolean {
        return sqlite3_column_type(stmtRef.rawPtr, index) == SQLITE_NULL
    }
}