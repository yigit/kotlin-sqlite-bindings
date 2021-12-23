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
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

//internal fun shouldBuildAndroidNative(gradle: Gradle): Boolean {
//    val os = DefaultNativePlatform.getCurrentOperatingSystem()
//    return !gradle.runningInIdea() && when {
//        os.isWindows -> !runningInCI()
//        else -> true
//    }
//}

internal fun KotlinMultiplatformExtension.setupNative(
    gradle: Gradle,
    includeAndroidNative: Boolean,
    configure: KotlinNativeTarget.() -> Unit
) {
    linuxX64(configure = configure)
    linuxArm32Hfp(configure = configure)
    mingwX64(configure = configure)
    macosX64(configure = configure)
    ios(configure = configure)
    androidNativeArm32(configure = configure)
    androidNativeArm64(configure = configure)
    androidNativeX64(configure = configure)
    androidNativeX86(configure = configure)
}

internal fun KotlinMultiplatformExtension.setupCommon(
    gradle: Gradle,
    includeAndroidNative: Boolean,
    configure: KotlinNativeTarget.() -> Unit
) {
    val nativeMain = sourceSets.create("nativeMain") {
        it.dependsOn(sourceSets["commonMain"])
    }
    val nativeTest = sourceSets.create("nativeTest") {
        it.dependsOn(sourceSets["commonTest"])
    }
    setupNative(gradle, includeAndroidNative) {
        // TODO move them to shared folder once we move to 1.4-M3
        //  unfortunately it doesn't work because IDE cannot detect
        //  the cinterop outputs
        //  see: https://youtrack.jetbrains.com/issue/KT-36086
//        compilations["main"].defaultSourceSet {
//            kotlin.srcDir("src/nativeMain/kotlin")
//        }
//        compilations["test"].defaultSourceSet {
//            kotlin.srcDir("src/nativeTest/kotlin")
//        }
        sourceSets[this.targetName + "Main"].dependsOn(nativeMain)
        sourceSets[this.targetName + "Test"].dependsOn(nativeTest)
        this.configure()
    }
}
