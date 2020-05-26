package com.birbit.sqlite3

expect object OsSpecificTestUtils {
    // mkdir in posix is different in all OSs so lets use this instead
    internal fun mkdirForTest(path: String)
}
