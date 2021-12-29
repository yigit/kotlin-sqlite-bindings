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

import com.birbit.ksqlite.build.internal.Publishing
import com.birbit.ksqlite.build.internal.isBuiltOnThisMachine
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable

data class SoInput(
    val folderName: String,
    val soFile: File
) : Serializable {
    companion object {
        fun folderName(konanTarget: KonanTarget): String {
            // see https://github.com/scijava/native-lib-loader/blob/master/src/main/java/org/scijava/nativelib/NativeLibraryUtil.java
            return when (konanTarget.family) {
                Family.LINUX -> {
                    "linux" + when (konanTarget.architecture) {
                        Architecture.ARM32 -> "_arm"
                        Architecture.ARM64 -> "_arm64"
                        Architecture.X86, Architecture.X64 -> "_${konanTarget.architecture.bitness}"
                        else -> throw GradleException("unexpected architecture ${konanTarget.architecture}")
                    }
                }
                Family.MINGW -> "windows_${konanTarget.architecture.bitness}"
                Family.OSX -> "osx_${konanTarget.architecture.bitness}"
                Family.ANDROID -> when (konanTarget.architecture) {
                    Architecture.X86 -> "x86"
                    Architecture.X64 -> "x86_64"
                    Architecture.ARM32 -> "armeabi-v7a"
                    Architecture.ARM64 -> "arm64-v8a"
                    else -> throw GradleException("add this architecture for android ${konanTarget.architecture}")
                }
                Family.IOS -> "ios" + when (konanTarget.architecture) {
                    Architecture.ARM64 -> "_arm64"
                    Architecture.X64 -> "_x64"
                    else -> throw GradleException("unsupported arch ${konanTarget.architecture} for IOS family")
                }
                else -> throw GradleException("unsupported architecture family ${konanTarget.family}")
            }
        }
    }
}

abstract class CollectNativeLibrariesTask : DefaultTask() {
    @Internal
    lateinit var soInputs: List<SoInput>

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    fun getFolderNames() = soInputs.map { it.folderName }

    @InputFiles
    fun getFilePaths() = soInputs.map { it.soFile }

    @Input
    var forAndroid: Boolean = false

    @TaskAction
    fun doIt() {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val nativeDir = if (forAndroid) {
            outputDir
        } else {
            outputDir.resolve("natives")
        }
        soInputs.forEach {
            nativeDir.resolve(it.folderName).resolve(it.soFile.name).let { outFile ->
                outFile.parentFile.mkdirs()
                outFile.writeBytes(it.soFile.readBytes())
            }
        }
    }

    companion object {

        @Suppress("unused")
        fun create(
            project: Project,
            namePrefix: String,
            outFolder: File,
            forAndroid: Boolean
        ): TaskProvider<CollectNativeLibrariesTask> {
            val suffix = if (forAndroid) {
                "ForAndroid"
            } else {
                "ForJvm"
            }
            return project.tasks.register(
                "collectSharedLibsFor${namePrefix.capitalize()}$suffix",
                CollectNativeLibrariesTask::class.java
            ) {
                configure(it, namePrefix, outFolder, forAndroid)
            }
        }

        fun configure(
            task: CollectNativeLibrariesTask,
            namePrefix: String,
            outFolder: File,
            forAndroid: Boolean
        ) {
            val kotlin = task.project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            checkNotNull(kotlin) {
                "cannot find kotlin extension"
            }
            val soFiles = mutableListOf<SoInput>()
            val distOutputsFolder = Publishing.getDistOutputs()
            var requireAndroidTarget = false
            if (distOutputsFolder == null || forAndroid) {
                // obtain from compilations
                kotlin.targets.withType(KotlinNativeTarget::class.java).filter {
                    requireAndroidTarget = requireAndroidTarget ||
                        it.konanTarget.family == Family.ANDROID
                    it.konanTarget.family != Family.IOS &&
                        it.konanTarget.isBuiltOnThisMachine() &&
                        forAndroid == (it.konanTarget.family == Family.ANDROID)
                }.forEach {
                    val sharedLib = it.binaries.findSharedLib(
                        namePrefix = namePrefix,
                        buildType = NativeBuildType.RELEASE
                    )
                    checkNotNull(sharedLib) {
                        "cannot find shared lib in $it"
                    }
                    soFiles.add(
                        SoInput(
                            folderName = SoInput.folderName(it.konanTarget),
                            soFile = sharedLib.outputFile
                        )
                    )
                    task.dependsOn(sharedLib.linkTask)
                }
            } else {
                requireAndroidTarget = true
                // collect from dist output
                val nativesFolders = distOutputsFolder.walkTopDown().filter {
                    it.isDirectory && it.name == "natives"
                }
                println("native folders: ${nativesFolders.toList()}")
                val foundSoFiles = nativesFolders.flatMap {
                    it.listFiles().asSequence().filter { it.isDirectory }.map { target ->
                        SoInput(
                            folderName = target.name,
                            soFile = target.listFiles().first()
                        )
                    }
                }
                soFiles.addAll(foundSoFiles)
            }
            // soFiles shouldn't be empty unless we are targeting android and have no android
            // targets
            check(soFiles.isNotEmpty() || (forAndroid && !requireAndroidTarget)) {
                "found no SO files for ${task.name}"
            }
            println("found so files:$soFiles, for android $forAndroid")
            task.soInputs = soFiles
            task.outputDir = outFolder
            task.forAndroid = forAndroid
        }
    }
}
