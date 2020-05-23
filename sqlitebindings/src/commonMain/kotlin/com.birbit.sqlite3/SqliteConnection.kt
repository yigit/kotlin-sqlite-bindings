package com.birbit.sqlite3

import com.birbit.sqlite3.internal.DbRef
import com.birbit.sqlite3.internal.SqliteApi

class SqliteConnection private constructor(
    val dbRef: DbRef
) {
    fun prepareStmt(stmt:String) : SqliteStmt {
        return SqliteStmt(SqliteApi.prepareStmt(dbRef, stmt))
    }
    companion object {
        fun openConnection(path : String) = SqliteConnection(
            SqliteApi.openConnection(path)
        )
    }
}