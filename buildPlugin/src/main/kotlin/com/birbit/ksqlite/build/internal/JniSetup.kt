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
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import java.util.concurrent.Callable

internal object JniSetup {
    fun configure(target: KotlinNativeTarget) {
        // jni already exists on android so we don't need it there
        if (target.konanTarget.family != Family.ANDROID) {
            target.compilations["main"].cinterops.create("jni") {
                // JDK is required here, JRE is not enough
                val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
                var include = File(javaHome, "include")
                if (!include.exists()) {
                    // look upper
                    include = File(javaHome, "../include")
                }
                if (!include.exists()) {
                    throw GradleException("cannot find include")
                }
                val os = DefaultNativePlatform.getCurrentOperatingSystem()
                if (os.isWindows) {
                    it.compilerOpts("-fdeclspec", "-D__int64=long long")
                }
                it.includeDirs(
                    Callable { include },
                    Callable { File(include, "darwin") },
                    Callable { File(include, "linux") },
                    Callable { File(include, "win32") },
                )
            }
        }
    }
}
