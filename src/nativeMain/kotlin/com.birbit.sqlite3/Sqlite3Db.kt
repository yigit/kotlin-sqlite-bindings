package com.birbit.sqlite3

import kotlinx.cinterop.*
import sqlite3.*
actual class SqliteDb(
    val ptr : CPointer<sqlite3>
) {
    actual fun version(): String {
        return sqlite3_version.toKStringFromUtf8()
    }
}

actual fun openDb(path: String): SqliteDb {
    val ptr = nativeHeap.allocPointerTo<sqlite3>()
    val openResult = sqlite3_open(":memory:", ptr.ptr)
    check(openResult == SQLITE_OK) {
        "could not open database $openResult"
    }
    return SqliteDb(ptr = ptr.value!!)
}