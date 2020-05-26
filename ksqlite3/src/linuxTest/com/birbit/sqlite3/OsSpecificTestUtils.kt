package com.birbit.sqlite3

import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.S_IXUSR
import platform.posix.mkdir

actual object OsSpecificTestUtils{
    internal actual fun mkdirForTest(path: String) {
        mkdir(path, S_IRUSR.or(S_IWUSR).or(S_IXUSR).toUInt())
    }
}