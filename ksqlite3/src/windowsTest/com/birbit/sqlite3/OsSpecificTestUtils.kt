package com.birbit.sqlite3

actual object OsSpecificTestUtils {
    internal actual fun mkdirForTest(path: String) {
        platform.posix.mkdir(path)
    }
}
