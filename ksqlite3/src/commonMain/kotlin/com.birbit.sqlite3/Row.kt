package com.birbit.sqlite3

import com.birbit.sqlite3.internal.SqliteApi
import com.birbit.sqlite3.internal.StmtRef

class Row internal constructor(
    private val stmt: StmtRef
){
    fun isNull(index : Int) = SqliteApi.columnIsNull(stmt, index)
    fun readString(index: Int) = SqliteApi.columnText(stmt, index)
    fun readInt(index: Int) = SqliteApi.columnInt(stmt, index)
}