/*
 * Copyright 2023 Google, LLC.
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
package com.birbit.ksqlite.build.internal.clang

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Platform
import org.jetbrains.kotlin.konan.target.PlatformManager
import javax.inject.Inject

abstract class KonanBuildService @Inject constructor(
    private val execOperations: ExecOperations
) : BuildService<KonanBuildService.Params> {
    private val dist = Distribution(
        konanHome = parameters.konanHome.get().asFile.absolutePath,
        onlyDefaultProfiles = false,
        propertyOverrides = mapOf(
//            "dependenciesUrl" to "file://${parameters.prebuilts.get().asFile}"
        )
    )

    private val platformManager = PlatformManager(
        distribution = dist
    )
    fun compile(clangCompileParameters: ClangCompileParameters) {
        val platform = resolveKonanTarget(clangCompileParameters.konanTarget.get())
        val outputDir = clangCompileParameters.output.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val clang = platform.clang
        execOperations.exec {
            it.executable = "clang"
            it.args(clang.clangArgs.toList())
            it.args(clangCompileParameters.freeArgs.get())
            it.workingDir = clangCompileParameters.output.get().asFile
            it.args(
                "-I${clangCompileParameters.includes.get().asFile.absolutePath}",
                "--compile",
//                "-Wall",
                "-v",
            )
            it.args(clangCompileParameters.sources.files.map { it.absolutePath })
        }
    }

    fun archive(clangArchiveParameters: LlvmArchiveParameters) {
        val platform = resolveKonanTarget(clangArchiveParameters.konanTarget.get())
        clangArchiveParameters.outputFile.get().asFile.also {
            it.delete()
            it.parentFile.mkdirs()
        }
        execOperations.exec {
            it.executable = platform.clang.llvmAr().single()
            it.args("rc")
            it.args(clangArchiveParameters.outputFile.get().asFile.absolutePath)
            it.args(
                clangArchiveParameters.objectFiles.files.flatMap {
                    it.walkTopDown().filter {
                        it.isFile
                    }.map { it.canonicalPath }
                }.distinct()
            )
        }
    }

    private fun resolveKonanTarget(konanTarget: KonanTarget): Platform {
        val wantedTargetName = konanTarget.name
        // for some reason, equals don't work between these, might be classpath issue, hence find it
        // again
        val wantedTarget = platformManager.enabled.find {
            it.name == wantedTargetName
        } ?: error("cannot find enabled target with name $wantedTargetName")
        val platform = platformManager.platform(wantedTarget)
        platform.downloadDependencies()
        return platform
    }

    interface Params : BuildServiceParameters {
        val konanHome: DirectoryProperty
    }

    companion object {
        const val KEY = "konanBuildService"
        fun obtain(
            project: Project
        ): Provider<KonanBuildService> {
            return project.gradle.sharedServices.registerIfAbsent(
                KEY,
                KonanBuildService::class.java
            ) {
                // see:
                // https://github.com/JetBrains/kotlin/blob/d97e8ae456364d90f0ffbd7f20535983d4d62c5d/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/compilerRunner/nativeToolRunners.kt#L33
                // maybe use that instead
                val nativeCompilerDownloader = NativeCompilerDownloader(project)
                nativeCompilerDownloader.downloadIfNeeded()

                it.parameters.konanHome.set(
                    nativeCompilerDownloader.compilerDirectory
                )
            }
        }
    }
}
