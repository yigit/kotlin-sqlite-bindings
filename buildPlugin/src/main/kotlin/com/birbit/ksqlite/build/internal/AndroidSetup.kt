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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

internal object AndroidSetup {
    private const val DOWNLOAD_CMD_LINE_TOOLS_TASK = "downloadAndroidCommandLineTools"
    private const val DOWNLOAD_NDK_TASK = "downloadNdk"
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
        androidLibrary.ndkVersion = "21.3.6528147"
        createInstallNdkTask(project)
    }

    fun createInstallNdkTask(project: Project) {
        // find a reference to android
        val exists = project.rootProject.tasks.findByName(DOWNLOAD_NDK_TASK)
        if (exists != null) {
            return
        }
        val android = project.extensions.findByType(LibraryExtension::class.java)
            ?: return

        val rootProject = project.rootProject
        val buildDir = rootProject.buildDir.resolve("android-cmd-line-tools")
        val toolsZip = buildDir.resolve("tools.zip")
        val downloadTask = project.tasks.register("downloadAndroidCmdLineTools", DownloadTask::class.java) {
            it.downlodUrl = buildCommandLineToolsUrl()
            it.downloadTargetFile = toolsZip
        }
        val cmdLineToolsFolder = buildDir.resolve("tools")
        val unzipCommandLineToolsTask = project.tasks.register("unzipCommandLineTools", Copy::class.java) {
            it.from(project.zipTree(toolsZip))
            it.into(cmdLineToolsFolder)
            it.dependsOn(downloadTask)
        }
        project.rootProject.tasks.register("downloadNdk", Exec::class.java) {
            val os = DefaultNativePlatform.getCurrentOperatingSystem()
            val ext = if (os.isWindows) {
                ".bat"
            } else {
                ""
            }
            if (os.isLinux) {
                it.doFirst {
                    Runtime.getRuntime().exec("sudo chown \$USER:\$USER ${android.sdkDirectory} -R")
                }
            }
            it.executable(cmdLineToolsFolder.resolve("tools/bin/sdkmanager$ext"))
            it.args("--install", "ndk;${android.ndkVersion}", "--verbose")
            it.args("--sdk_root=${android.sdkDirectory.absolutePath}")
            // pass y to accept licenses
            it.setStandardInput("y".byteInputStream(Charsets.UTF_8))
            it.dependsOn(unzipCommandLineToolsTask)
        }
    }

    private fun buildCommandLineToolsUrl(): String {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        val osKey = when {
            os.isWindows -> "win"
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            else -> error("unsupported build OS: ${os.displayName}")
        }
        return "https://dl.google.com/android/repository/commandlinetools-$osKey-6514223_latest.zip"
    }
}
