package com.birbit.sqlite3

import com.birbit.jni.NativeHost

actual class SqliteConnection(
    private val ptr: Long
) {
    actual fun version(): String {
        return NativeHost.getSqliteVersion(ptr) ?: "null version"
    }
}

actual fun openConnection(path: String): SqliteConnection {
    val openDb = NativeHost.openDb(path)
    return SqliteConnection(openDb)
}