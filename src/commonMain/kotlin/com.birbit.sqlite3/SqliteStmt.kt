package com.birbit.sqlite3

import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteApi
import com.birbit.sqlite3.internal.StmtRef

class SqliteStmt(
    val stmtRef: StmtRef
) {
    fun step(): ResultCode = SqliteApi.step(stmtRef)
    fun columnText(index: Int) = SqliteApi.columnText(stmtRef, index)
}