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

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UIntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.runBlocking
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.S_IXUSR
import platform.posix._opaque_pthread_t
import platform.posix.mkdir
import platform.posix.pthread_getname_np
import platform.posix.pthread_getugid_np
import platform.posix.pthread_self
import platform.posix.uid_t
import platform.posix.uid_tVar

actual object OsSpecificTestUtils {
    internal actual fun mkdirForTest(path: String) {
        mkdir(path, S_IRUSR.or(S_IWUSR).or(S_IXUSR).convert())
    }

    internal actual fun <T> myRunBlocking(block: suspend () -> T): T {
        return runBlocking {
            memScoped {

            }
            block()
        }
    }

    internal actual fun threadId(): String = "${pthread_self()}"
//        val self : CPointer<_opaque_pthread_t> = pthread_self()!!
//        val id :UInt = self.reinterpret<UIntVar>()[0]
//        return "$id"
//    }
}
