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

    fun bind(index: Int, value: ByteArray) {
        val resultCode = SqliteApi.bindBlob(stmtRef, index, value)
        check(ResultCode.OK == resultCode) {
            "unable to bind bytes: $resultCode"
        }
    }

    fun bind(index: Int, value: String) {
        val resultCode = SqliteApi.bindText(stmtRef, index, value)
        check(ResultCode.OK == resultCode) {
            "unable to bind value $value to index $index"
        }
    }


    fun bind(index: Int, value: Int) {
        val resultCode = SqliteApi.bindInt(stmtRef, index, value)
        check(ResultCode.OK == resultCode) {
            "unable to bind value $value to index $index"
        }
    }

    fun bind(index: Int, value: Long) {
        val resultCode = SqliteApi.bindLong(stmtRef, index, value)
        check(ResultCode.OK == resultCode) {
            "unable to bind value $value to index $index"
        }
    }

    fun bindNull(index: Int) {
        val resultCode = SqliteApi.bindNull(stmtRef, index)
        check(ResultCode.OK == resultCode) {
            "unable to bind null to index $index"
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