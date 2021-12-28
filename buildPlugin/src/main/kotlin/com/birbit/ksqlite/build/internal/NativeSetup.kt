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

import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal fun KotlinMultiplatformExtension.setupNative(
    configure: KotlinNativeTarget.() -> Unit
) {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    mingwX64(configure = configure)
    if (os.isWindows) {
        // windows build fails with error: unknown type name '__int64' for JNI in
        // cinteropJniLinuxX64
        return
    }
    linuxX64(configure = configure)
    linuxArm32Hfp(configure = configure)
    macosX64(configure = configure)
    ios(configure = configure)
    androidNativeArm32(configure = configure)
    androidNativeArm64(configure = configure)
    androidNativeX64(configure = configure)
    androidNativeX86(configure = configure)
}

internal fun KotlinMultiplatformExtension.setupCommon(
    configure: KotlinNativeTarget.() -> Unit
) {
    val nativeMain = sourceSets.create("nativeMain") {
        it.dependsOn(sourceSets["commonMain"])
    }
    val nativeTest = sourceSets.create("nativeTest") {
        it.dependsOn(sourceSets["commonTest"])
    }
    setupNative {
        sourceSets[this.targetName + "Main"].dependsOn(nativeMain)
        sourceSets[this.targetName + "Test"].dependsOn(nativeTest)
        this.configure()
    }
}
