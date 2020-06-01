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

class SqliteStmt(
    val connection: SqliteConnection,
    private val stmtRef: StmtRef
) {
    fun close() {
        check(SqliteApi.finalize(stmtRef) == ResultCode.OK) {
            "failed to close result code"
        }
        stmtRef.dispose()
    }

    fun obtainResultMetadata(): ResultMetadata {
        val columnCount = SqliteApi.columnCount(stmtRef)
        val columns = (0 until columnCount).map { columnIndex ->
            ResultMetadata.ColumnInfo(
                databaseName = SqliteApi.columnDatabaseName(stmtRef, columnIndex),
                tableName = SqliteApi.columnTableName(stmtRef, columnIndex),
                originName = SqliteApi.columnOriginName(stmtRef, columnIndex),
                declaredType = SqliteApi.columnDeclType(stmtRef, columnIndex),
                name = SqliteApi.columnName(stmtRef, columnIndex)
            )
        }
        return ResultMetadata(columns)
    }

    fun obtainBindMetadata(): BindParameterMetadata {
        val bindParamCount = SqliteApi.bindParameterCount(stmtRef)
        return BindParameterMetadata(
            params = (1..bindParamCount).map {
                BindParameterMetadata.BindParameter(
                    index = it,
                    name = SqliteApi.bindParameterName(stmtRef, index = it)
                )
            }
        )
    }

    fun <T> use(block: (SqliteStmt) -> T): T {
        return try {
            block(this)
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

    fun bind(index: Int, value: Double) {
        val resultCode = SqliteApi.bindDouble(stmtRef, index, value)
        check(ResultCode.OK == resultCode) {
            "unable to bind value $value to index $index"
        }
    }

    // TODO provide an API where we can enforce closing
    //  maybe sth like `use` which will give APIs like query during the time `use` is called.
    //  might be better to call it `acquire` or `obtain` if we won't close afterwards though.
    fun query(): Sequence<Row> = sequence {
        SqliteApi.reset(stmtRef)
        val row = Row(stmtRef)
        val stepResultCode: ResultCode = ResultCode.OK
        while (SqliteApi.step(stmtRef).also { stepResultCode == it } == ResultCode.ROW) {
            yield(row)
        }
        check(stepResultCode == ResultCode.OK || stepResultCode == ResultCode.DONE) {
            "querying rows ended prematurely $stepResultCode"
        }
    }

    fun bindValue(index: Int, value: Any?) {
        // should we delgate to sqlite? might be tricky w/ all type casting
        when (value) {
            null -> bindNull(index)
            is Int -> bind(index, value)
            is Long -> bind(index, value)
            is Float -> bind(index, value.toDouble())
            is Double -> bind(index, value)
            is String -> bind(index, value)
            is ByteArray -> bind(index, value)
            is Number -> bind(index, value.toDouble())
            else -> throw SqliteException(ResultCode.FORMAT, "cannot bind $value")
        }
    }

    fun bindValues(args: List<Any?>) {
        args.forEachIndexed { index, value ->
            bindValue(index + 1, value)
        }
    }

    fun columnType(index: Int) = SqliteApi.columnType(stmtRef, index)
    fun normalizedQuery() = SqliteApi.normalizedSql(stmtRef)
    fun expandedQuery() = SqliteApi.expandedSql(stmtRef)
    fun sql() = SqliteApi.sql(stmtRef)

    data class ResultMetadata(
        val columns: List<ColumnInfo>
    ) {
        data class ColumnInfo(
            val name: String?,
            val databaseName: String?,
            val tableName: String?,
            val originName: String?,
            val declaredType: String?
        )
    }

    data class BindParameterMetadata(
        val params: List<BindParameter>
    ) {
        data class BindParameter(
            val index: Int,
            val name: String?
        )
    }
}
