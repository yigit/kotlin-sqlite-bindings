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
package com.birbit.ksqlite.build.internal

import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal fun shouldBuildAndroidNative(gradle: Gradle): Boolean {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    return !gradle.runningInIdea() && when {
        os.isWindows -> !runningInCI()
        else -> true
    }
}

internal fun KotlinMultiplatformExtension.setupNative(
    gradle: Gradle,
    includeAndroidNative: Boolean,
    configure: KotlinNativeTarget.() -> Unit
) {
    // TODO change this to build only on one target in CI
    val runningInIdea = gradle.runningInIdea()
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
        ios(configure = configure)
        if (shouldBuildAndroidNative(gradle) && includeAndroidNative) {
            androidNativeArm32(configure = configure)
            androidNativeArm64(configure = configure)
            androidNativeX64(configure = configure)
            androidNativeX86(configure = configure)
        }
    }
}

internal fun KotlinMultiplatformExtension.setupCommon(
    gradle: Gradle,
    includeAndroidNative: Boolean,
    configure: KotlinNativeTarget.() -> Unit
) {
    setupNative(gradle, includeAndroidNative) {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        val osSpecificFolderPrefix = when {
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            os.isWindows -> "windows"
            else -> null
        }
        //  TODO move them to shared folder once we move to 1.4-M3
        osSpecificFolderPrefix?.let {
            compilations["main"].defaultSourceSet {
                kotlin.srcDir("src/${it}Main/kotlin")
            }
            compilations["test"].defaultSourceSet {
                kotlin.srcDir("src/${it}Test/kotlin")
            }
        }

        compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
        }
        compilations["test"].defaultSourceSet {
            kotlin.srcDir("src/nativeTest/kotlin")
        }

        this.configure()
    }
}
