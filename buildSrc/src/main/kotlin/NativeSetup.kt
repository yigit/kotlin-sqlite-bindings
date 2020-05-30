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
package com.birbit.ksqlite.build

import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun KotlinMultiplatformExtension.setupNative(
    gradle: Gradle,
    configure: KotlinNativeTarget.() -> Unit
) {
    val runningInIdea = gradle.startParameter.systemPropertiesArgs.containsKey("idea.active")
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    if (runningInIdea || os.isWindows) {
        when {
            os.isLinux -> {
                linuxX64(configure = configure)
            }
            os.isWindows -> {
                mingwX64(configure = configure)
            }
            os.isMacOsX -> {
                macosX64(configure = configure)
            }
            else -> error("OS $os is not supported")
        }
    } else {
        linuxX64(configure = configure)
        linuxArm32Hfp(configure = configure)
        mingwX64(configure = configure)
        macosX64(configure = configure)
    }
}

fun KotlinMultiplatformExtension.setupCommon(
    gradle: Gradle,
    configure: KotlinNativeTarget.() -> Unit
) {
    setupNative(gradle) {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        val osSpecificFolderPrefix = when {
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            os.isWindows -> "windows"
            else -> null
        }
        // TODO we should nest these folders 1 more to be consistent w/ common
        osSpecificFolderPrefix?.let {
            compilations["main"].defaultSourceSet {
                kotlin.srcDir("src/${it}Main")
            }
            compilations["test"].defaultSourceSet {
                kotlin.srcDir("src/${it}Test")
            }
        }
        compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain")
        }
        compilations["test"].defaultSourceSet {
            kotlin.srcDir("src/nativeTest")
        }
        this.configure()
    }
}
