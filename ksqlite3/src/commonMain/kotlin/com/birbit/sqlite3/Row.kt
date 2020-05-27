/*
 * Copyright 2020 Google, LLC.
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
package com.birbit.sqlite3

import com.birbit.sqlite3.internal.SqliteApi
import com.birbit.sqlite3.internal.StmtRef

class Row internal constructor(
    private val stmt: StmtRef
) {
    fun isNull(index: Int) = SqliteApi.columnIsNull(stmt, index)
    fun readString(index: Int) = SqliteApi.columnText(stmt, index)
    fun readInt(index: Int) = SqliteApi.columnInt(stmt, index)
    fun readByteArray(index: Int): ByteArray? = SqliteApi.columnBlob(stmt, index)
    fun readDouble(index: Int): Double = SqliteApi.columnDouble(stmt, index)
    fun readLong(index: Int): Long = SqliteApi.columnLong(stmt, index)
}
