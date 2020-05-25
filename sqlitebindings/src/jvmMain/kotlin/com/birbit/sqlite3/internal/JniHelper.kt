package com.birbit.sqlite3.internal

internal object JniHelper {
    @JvmStatic
    fun createSqliteException(
        resultCode: Int,
        msg : String
    ) : Any = SqliteException(ResultCode(resultCode), msg)
}
