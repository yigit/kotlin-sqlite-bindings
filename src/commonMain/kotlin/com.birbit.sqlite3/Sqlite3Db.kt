package com.birbit.sqlite3

expect class SqliteConnection {
    fun version(): String
}

class Sqlite3Db(
    private val connection: SqliteConnection
) {

}

expect fun openConnection(path: String): SqliteConnection