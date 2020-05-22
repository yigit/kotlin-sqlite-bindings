package com.birbit.sqlite3

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
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.toLong
import kotlinx.cinterop.utf8
import kotlinx.cinterop.value
import sqlite3.SQLITE_OK
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_libversion
import sqlite3.sqlite3_open
import sqlite3.sqlite3_prepare_v2
import sqlite3.sqlite3_step

inline class StmtPtr(val rawPtr: CPointer<sqlite3_stmt>)
inline class DbPtr(val rawPtr: CPointer<sqlite3>)
actual class SqliteConnection(
    val ptr: DbPtr
) {
    val stableRef : StableRef<SqliteConnection> = StableRef.create(this)
    fun toJni() = stableRef.asCPointer().toLong()


    actual fun version(): String {
        return sqlite3_libversion()!!.toKString()
        // cannot access the one below for some reason, even though compiling w/ fPIC, investigate
//        return sqlite3_version.toKStringFromUtf8()
    }

    fun versionViaQuery() : String? {
        // TODO only for test
        val stmt = prepareStmt("SELECT SQLITE_VERSION() as FOO")
        stmt.step()
        val firstColumn = stmt.readString(0)
        return firstColumn
    }

    fun prepareStmt(stmt: String): SqliteStmt {
        val stmtPtr = nativeHeap.allocPointerTo<sqlite3_stmt>()
        val resultCode = sqlite3_prepare_v2(ptr.rawPtr, stmt.utf8, -1, stmtPtr.ptr, null)
        check(resultCode == SQLITE_OK) {
            "cannot prepare statement $stmt"
        }
        return SqliteStmt(
            dbPtr = ptr,
            stmtPtr = StmtPtr(stmtPtr.value!!)
        )
    }

    companion object {
        fun fromOpaquePointer(ptr : COpaquePointer) : SqliteConnection {
            return ptr.asStableRef<SqliteConnection>().get()
        }
        fun fromJni(jlong: jlong): SqliteConnection {
            return fromOpaquePointer(jlong.toCPointer<CPointed>()!!)
        }
    }
}

class SqliteStmt(
    private val dbPtr: DbPtr,
    val stmtPtr: StmtPtr
) {
    private var finalized = false
    fun step() {
        sqlite3_step(stmtPtr.rawPtr)
    }

    fun readString(columnIndex: Int): String? {
        val textPtr: CPointer<UByteVar>? = sqlite3_column_text(stmtPtr.rawPtr, columnIndex)
        if (textPtr == null) {
            return null
        }
        return textPtr.reinterpret<ByteVar>().toKStringFromUtf8()
    }
}

actual fun openConnection(path: String): SqliteConnection {
    val ptr = nativeHeap.allocPointerTo<sqlite3>()
    val openResult = sqlite3_open(":memory:", ptr.ptr)
    check(openResult == SQLITE_OK) {
        "could not open database $openResult"
    }
    return SqliteConnection(ptr = DbPtr(ptr.value!!))
}