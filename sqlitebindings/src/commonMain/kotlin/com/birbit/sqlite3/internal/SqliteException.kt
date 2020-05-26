/*
 * Copyright 2020 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.birbit.sqlite3.internal

class SqliteException(
    val resultCode: ResultCode,
    val msg: String
) : Throwable(msg) {
    companion object {
        inline fun buildFromConnection(dbRef: DbRef, errorCode: Int?): SqliteException {
            return SqliteException(
                resultCode = errorCode?.let { ResultCode(it) } ?: SqliteApi.errorCode(dbRef),
                msg = SqliteApi.errorMsg(dbRef) ?: errorCode?.let { SqliteApi.errorString(ResultCode(it)) }
                ?: "unknown error"
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
    received: Int,
    expected: Int
) {
    if (received != expected) {
        throw SqliteException.buildFromConnection(dbRef, received)
    }
}
