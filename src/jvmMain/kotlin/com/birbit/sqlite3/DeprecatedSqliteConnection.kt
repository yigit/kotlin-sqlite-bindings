package com.birbit.sqlite3

import com.birbit.jni.NativeHost

actual class DeprecatedSqliteConnection(
    private val ptr: Long
) {
    actual fun version(): String {
        return NativeHost.getSqliteVersion(ptr) ?: "null version"
    }
}

actual fun openConnection(path: String): DeprecatedSqliteConnection {
    val openDb = NativeHost.openDb(path)
    return DeprecatedSqliteConnection(openDb)
}