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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
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
                else -> throw GradleException("unsupported architecture family ${konanTarget.family}")
            }
        }
    }
}

// TODO
//  public output of this as a github action output and then have another job that'll merge them into final jar build
abstract class CollectNativeLibrariesTask : DefaultTask() {
    lateinit var soInputs: List<SoInput>

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    fun getFolderNames() = soInputs.map { it.folderName }

    @InputFiles
    fun getFilePaths() = soInputs.map { it.soFile }

    @TaskAction
    fun doIt() {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val nativeDir = outputDir.resolve("natives")
        soInputs.forEach {
            nativeDir.resolve(it.folderName).resolve(it.soFile.name).let { outFile ->
                outFile.parentFile.mkdirs()
                outFile.writeBytes(it.soFile.readBytes())
            }
        }
    }

    companion object {

        fun create(project: Project, namePrefix: String, outFolder: File): TaskProvider<CollectNativeLibrariesTask> {
            return project.tasks.register(
                "collectSharedLibsFor${namePrefix.capitalize()}",
                CollectNativeLibrariesTask::class.java
            ) {
                configure(it, namePrefix, outFolder)
            }
        }

        fun configure(task: CollectNativeLibrariesTask, namePrefix: String, outFolder: File) {
            val kotlin = task.project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            checkNotNull(kotlin) {
                "cannot find kotlin extension"
            }
            val soFiles = mutableListOf<SoInput>()
            val distOutputsFolder = Publishing.getDistOutputs()
            if (distOutputsFolder == null) {
                // obtain from compilations
                kotlin.targets.withType(KotlinNativeTarget::class.java).filter {
                    it.konanTarget.isBuiltOnThisMachine()
                }.forEach {
                    val sharedLib = it.binaries.findSharedLib(
                        namePrefix = namePrefix,
                        buildType = NativeBuildType.DEBUG // TODO
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
            check(soFiles.isNotEmpty()) {
                println("sth is wrong, there should be some so files")
            }
            println("found so files:$soFiles")
            task.soInputs = soFiles
            task.outputDir = outFolder
        }
    }
}