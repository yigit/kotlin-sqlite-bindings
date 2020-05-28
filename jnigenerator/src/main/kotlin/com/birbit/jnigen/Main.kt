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
package com.birbit.jnigen

import java.io.File
import java.util.Calendar
import org.jetbrains.kotlin.spec.grammar.tools.parseKotlinCode

/**
 * TODO: should we make this part of the build?
 */
fun main() {
    val srcFile = File("./sqlitebindings/src/jvmMain/kotlin/com/birbit/sqlite3/internal/JvmSqliteApi.kt")
    val targetFile = File("./sqlitebindings/src/nativeMain/com/birbit/sqlite3/internal/GeneratedJni.kt")
    println("hello ${File(".").absolutePath}")
    val tokens = parseKotlinCode(srcFile.readText(Charsets.UTF_8))
    val sqliteApiObject = tokens.objectDeclarations().first {
        it.name == "SqliteApi"
    }

    val methods = sqliteApiObject.functions.groupBy {
        it.external
    }
    val externalMethods = methods[true] ?: error("no external method?")
    val actualMethods = methods[false]?.associateBy {
        "native${it.name.capitalize()}"
    } ?: error("no actual methods?")
    val pairs = externalMethods.map { native ->
        val actualMethod = checkNotNull(actualMethods[native.name]) {
            "cannot find actual method for  $native"
        }
        FunctionPair(
            actualFun = actualMethod,
            nativeFun = native
        )
    }
    println(pairs)
    val copyright = File("./scripts/copyright.txt")
        .readText(Charsets.UTF_8)
        .replace("\$YEAR", Calendar.getInstance().get(Calendar.YEAR).toString())
    JniWriter(copyright, pairs).write(targetFile)
}
