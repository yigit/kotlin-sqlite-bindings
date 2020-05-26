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
package com.birbit.sqlite3

import kotlinx.cinterop.memScoped
import platform.posix.F_OK
import platform.posix.access
import platform.posix.system
import kotlin.random.Random

actual object PlatformTestUtils {
    private fun randomFolderName(): String {
        return (0..20).map {
            'a' + Random.nextInt(0, 26)
        }.joinToString("")
    }

    actual fun getTmpDir(): String {
        val tmpDir = memScoped {
            val tmpName = "ksqlite_tmp${randomFolderName()}"
            // for some reason, mkdtemp does not work on command line tests :/
            // second param to mkdir is UShort on mac and UInt on linux :/
            OsSpecificTestUtils.mkdirForTest(tmpName)
            tmpName
        }
        return checkNotNull(tmpDir) {
            "mkdtemp failed environment variable"
        }
    }

    actual fun fileExists(path: String): Boolean {
        return access(path, F_OK) != -1
    }

    actual fun fileSeparator(): Char {
        return when (Platform.osFamily) {
            OsFamily.WINDOWS -> '\\'
            else -> '/'
        }
    }

    actual fun deleteDir(tmpDir: String) {
        when (Platform.osFamily) {
            OsFamily.WINDOWS -> {
                system("rmdir $tmpDir /s /q")
            }
            else -> system("rm -rf $tmpDir")
        }
    }
}
