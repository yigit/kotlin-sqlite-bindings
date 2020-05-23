package com.birbit.sqlite3.internal

// commonized sqlite APIs to build the rest in common, or most at least
inline class ResultCode(val value: Int)
expect class DbRef
expect class StmtRef

expect object SqliteApi {
    fun openConnection(path: String): DbRef
    fun prepareStmt(dbRef: DbRef, stmt: String): StmtRef
    fun step(stmtRef: StmtRef): ResultCode
    fun columnText(stmtRef: StmtRef, index: Int): String?
}