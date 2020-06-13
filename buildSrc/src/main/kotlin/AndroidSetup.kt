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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project

internal object AndroidSetup {
    fun configure(project: Project) {
        val androidLibrary = project.extensions.findByType(LibraryExtension::class.java)
            ?: error("cannot find library extension on $project")
        androidLibrary.compileSdkVersion = "android-29"
        androidLibrary.defaultConfig {
            it.minSdkVersion(21)
            it.targetSdkVersion(29)
            it.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            it.ndk {
                it.abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
        androidLibrary.sourceSets {
            it.getByName("androidTest").java
                .srcDir(project.file("src/androidTest/kotlin"))
        }
        androidLibrary.ndkVersion = "21.2.6472646"
    }
}
