package com.birbit.sqlite3

import com.birbit.sqlite3.internal.ResultCode
import com.birbit.sqlite3.internal.SqliteApi
import com.birbit.sqlite3.internal.StmtRef

class SqliteStmt(
    private val stmtRef: StmtRef
) {
    fun close() {
        check(SqliteApi.finalize(stmtRef) == ResultCode.OK) {
            "failed to close result code"
        }
        stmtRef.dispose()
    }

    fun <T> use(block : () -> T) : T {
        return try {
            block()
        } finally {
            close()
        }
    }

    // TODO provide an API where we can enforce closing
    //  maybe sth like `use` which will give APIs like query during the time `use` is called.
    //  might be better to call it `acquire` or `obtain` if we won't close afterwards though.
    fun query(): Sequence<Row> = sequence {
        SqliteApi.reset(stmtRef)
        val row = Row(stmtRef)
        while (SqliteApi.step(stmtRef) == ResultCode.ROW) {
            yield(row)
        }
    }
}