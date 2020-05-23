package com.birbit.sqlite3

expect class DeprecatedSqliteConnection {
    fun version(): String
}

class DeprecatedSqlite3Db(
    private val connectionDeprecated: DeprecatedSqliteConnection
) {

}

expect fun openConnection(path: String): DeprecatedSqliteConnection