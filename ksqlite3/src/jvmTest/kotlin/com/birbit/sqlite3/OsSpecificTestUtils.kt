package com.birbit.sqlite3

import java.io.File

actual object OsSpecificTestUtils {
    internal actual fun mkdirForTest(path: String) {
        File(path).mkdirs()
    }
}
