/*
 * Copyright 2021 Google, LLC.
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
package com.birbit.ksqlite.build.internal

import com.google.common.truth.Truth.assertThat
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test

class KonanPropLoaderTest {
    @Test
    fun loadToolchain() {
        val hostManager = HostManager
        assertThat(
            KonanPropLoader.require("llvm.linux_x64.dev")
        ).isEqualTo("llvm-11.1.0-linux-x64-essentials")
        assertThat(KonanPropLoader.llvmHome(KonanTarget.MACOS_X64))
            .isEqualTo("apple-llvm-20200714-macos-x64-essentials")
        assertThat(KonanPropLoader.targetTriple(KonanTarget.LINUX_X64))
            .isEqualTo("x86_64-unknown-linux-gnu")
        assertThat(KonanPropLoader.targetTriple(KonanTarget.ANDROID_ARM32))
            .isEqualTo("arm-unknown-linux-androideabi")
        assertThat(KonanPropLoader.targetTriple(KonanTarget.IOS_X64))
            .isEqualTo("x86_64-apple-ios-simulator")
        assertThat(KonanPropLoader.sysroot(KonanTarget.IOS_X64))
            .isEqualTo("target-sysroot-xcode_12_5-iphonesimulator")
        assertThat(KonanPropLoader.clangFlags(KonanTarget.IOS_X64))
            .isEqualTo("-cc1 -emit-obj -disable-llvm-passes -x ir")
    }
}
