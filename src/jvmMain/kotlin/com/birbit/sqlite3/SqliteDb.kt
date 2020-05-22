package com.birbit.sqlite3

import com.birbit.jni.NativeHost

actual class SqliteDb(
    private val ptr: ULong
) {
    actual fun version(): String {
        TODO("Not yet implemented")
    }
}

actual fun openDb(path: String): SqliteDb {
    NativeHost.openDb(path)
}