package com.birbit.sqlite3

import com.birbit.sqlite3.internal.DbRef
import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteApi

class SqliteConnection private constructor(
    private val dbRef: DbRef
) {
    fun prepareStmt(stmt:String) : SqliteStmt {
        return SqliteStmt(this, SqliteApi.prepareStmt(dbRef, stmt))
    }

    fun lastErrorMessage() = SqliteApi.errorMsg(dbRef)

    fun lastErrorCode() = SqliteApi.errorCode(dbRef)

    fun <T> use(block : (SqliteConnection) -> T) : T {
        return try {
            block(this)
        } finally {
            close()
        }
    }

    fun close() {
        check(SqliteApi.close(dbRef) == ResultCode.OK) {
            "failed to close database"
        }
        dbRef.dispose()
    }

    companion object {
        fun openConnection(path : String) = SqliteConnection(
            SqliteApi.openConnection(path)
        )
    }
}