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
package com.birbit.ksqlite3

import java.io.File
import java.nio.file.Paths
import java.util.UUID

actual object PlatformTestUtils {
    actual fun getTmpDir(): String {
        val tmpDirPath = System.getProperty("java.io.tmpdir") ?: error("cannot find java tmp dir")
        val fullPath = Paths.get(tmpDirPath, "ksqlite", UUID.randomUUID().toString().substring(0, 20))
        val file = fullPath.toFile()
        file.mkdirs()
        return file.absolutePath
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun fileSeparator(): Char {
        return File.separatorChar
    }

    actual fun deleteDir(tmpDir: String) {
        val file = File(tmpDir)
        if (!file.exists()) return
        file.deleteRecursively()
    }
}
