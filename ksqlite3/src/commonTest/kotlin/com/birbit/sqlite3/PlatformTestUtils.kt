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

expect object PlatformTestUtils {
    fun getTmpDir(): String
    fun fileExists(path: String): Boolean
    fun fileSeparator(): Char
    fun deleteDir(tmpDir: String)
}

fun <T> withTmpFolder(block: TmpFolderScope.() -> T) {
    val tmpDir = PlatformTestUtils.getTmpDir()
    val scope = object : TmpFolderScope {
        override fun getFilePath(name: String) = tmpDir + PlatformTestUtils.fileSeparator() + name
    }
    try {
        scope.block()
    } finally {
        PlatformTestUtils.deleteDir(tmpDir)
    }
}

interface TmpFolderScope {
    fun getFilePath(name: String): String
}
