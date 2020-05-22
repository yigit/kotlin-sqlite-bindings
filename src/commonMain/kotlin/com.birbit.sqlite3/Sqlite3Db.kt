package com.birbit.sqlite3

expect class SqliteDb {
    fun version(): String
}

expect fun openDb(path: String): SqliteDb