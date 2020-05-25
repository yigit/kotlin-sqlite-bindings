package com.birbit.sqlite3.internal

class SqliteException(
    val resultCode: ResultCode,
    val msg : String
) : Throwable(msg) {
    companion object {
        inline fun buildFromConnection(dbRef: DbRef, errorCode: Int?): SqliteException {
            return SqliteException(
                resultCode = errorCode?.let { ResultCode(it) } ?: SqliteApi.errorCode(dbRef),
                msg = SqliteApi.errorMsg(dbRef) ?: errorCode?.let { SqliteApi.errorString(ResultCode(it)) } ?: "unknown error"
            )
        }
    }

    override fun toString(): String {
        return "[ResultCode: $resultCode] $msg"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SqliteException

        if (resultCode != other.resultCode) return false
        if (msg != other.msg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resultCode.hashCode()
        result = 31 * result + msg.hashCode()
        return result
    }
}

// TODO trying to use ResultCode here crashed the compiler, hence using Ints
internal inline fun checkResultCode(
    dbRef: DbRef,
    received : Int,
    expected : Int
) {
    if (received != expected) {
        throw SqliteException.buildFromConnection(dbRef, received)
    }
}