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
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.spec.grammar.tools.parseKotlinCode

/**
 * TODO: should we make this part of the build?
 */
fun main() {
    val srcFile = File("./sqlitebindings/src/commonJvmMain/kotlin/com/birbit/sqlite3/internal/JvmCommonSqliteApi.kt")
    val targetFile = File("./sqlitebindings/src/nativeMain/kotlin/com/birbit/sqlite3/internal/GeneratedJni.kt")
    val jvmMethodNames = findMethodNamesFromClassFile("./sqlitebindings/build")
    val invalidMethods = jvmMethodNames.filter {
        it.suffix.isNotBlank() && it.originalName.startsWith("native")
    }
    if (invalidMethods.isNotEmpty()) {
        val methodNames = invalidMethods.joinToString("\n") {
            it.fullName
        }
        error("""These methods cannot be called from native: $methodNames""")
    }
    println("hello ${File(".").absolutePath}")
    val tokens = parseKotlinCode(srcFile.readText(Charsets.UTF_8))
    val sqliteApiObject = tokens.objectDeclarations().first {
        it.name == "SqliteApi"
    }

    val methods = sqliteApiObject.functions.filterNot {
        false && it.name in listOf("Suppress", "JvmName")
    }.groupBy {
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

fun findMethodNamesFromClassFile(folder: String): List<JvmClassName> {
    val javaHome = System.getenv("JAVA_HOME") ?: error("cannot find java home")
    val javap = File(javaHome, "bin/javap")
    val classFile = File("$folder/classes/kotlin/jvm/main/com/birbit/sqlite3/internal/SqliteApi.class")
    if (!classFile.exists()) {
        throw IllegalStateException("compile the project first, we need to validate mangled names")
    }
    val cmd = javap.absolutePath + " -s $classFile"
    val output = cmd.runCommand(File(".")) ?: "could not run javap"
    return output.lines().mapNotNull {
        it.parseJavapMethodName()
    }
}

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun String.parseJavapMethodName(): JvmClassName? {
    val openParan = indexOf('(')
    if (openParan < 0) return null
    val firstSpace = substring(0, openParan).lastIndexOf(" ")
    val methodName = substring(firstSpace + 1, openParan).let {
        if (it.isBlank()) null
        else it
    } ?: return null
    // native methods gets a `-` in their name, cleanup
    return if (methodName.contains("-")) {
        methodName.split("-").let {
            JvmClassName(it[0], it[1])
        }
    } else {
        JvmClassName(methodName, "")
    }
}

data class JvmClassName(
    val originalName: String,
    val suffix: String
) {
    val fullName: String
        get() = if (suffix.isBlank()) {
            originalName
        } else {
            "$originalName-$suffix"
        }
}
