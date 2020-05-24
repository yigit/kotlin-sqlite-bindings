package com.birbit.sqlite3

import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteApi
import com.birbit.sqlite3.internal.StmtRef

class SqliteStmt(
    private val stmtRef: StmtRef
) {
    internal fun step(): ResultCode = SqliteApi.step(stmtRef)

    // TODO provide an API where we can enforce closing
    fun query(): Sequence<Row> = sequence {
        SqliteApi.reset(stmtRef)
        val row = Row(stmtRef)
        while (SqliteApi.step(stmtRef).value == SqliteResultCodes.SQLITE_ROW) {
            yield(row)
        }
    }
}