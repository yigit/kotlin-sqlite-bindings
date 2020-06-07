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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID

actual object PlatformTestUtils {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    actual fun getTmpDir(): String = context.filesDir.resolve(
        UUID.randomUUID().toString().substring(0, 6)
    ).let {
        it.mkdirs()
        return it.absolutePath
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun fileSeparator(): Char {
        return File.separatorChar
    }

    actual fun deleteDir(tmpDir: String) {
        File(tmpDir).deleteRecursively()
    }
}
