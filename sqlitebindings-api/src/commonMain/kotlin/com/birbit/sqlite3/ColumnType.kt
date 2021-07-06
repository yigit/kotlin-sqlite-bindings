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

import kotlin.jvm.JvmInline

// see https://www.sqlite.org/c3ref/c_blob.html
@JvmInline
value class ColumnType(val value: Int) {
    companion object {
        val INTEGER = ColumnType(1)
        val FLOAT = ColumnType(2)
        val STRING = ColumnType(3)
        val BLOB = ColumnType(4)
        val NULL = ColumnType(5)
    }
}
