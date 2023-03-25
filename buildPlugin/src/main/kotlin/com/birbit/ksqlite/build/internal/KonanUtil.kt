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

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object KonanUtil {
    private val konanDeps = DependencyDirectories.defaultDependenciesRoot
    private val konanProps = KonanPropLoader
    private val toolChainFolderName = konanProps.llvmHome(HostManager.host)
    private val llvmBinFolder = konanDeps.resolve("$toolChainFolderName/bin")

    internal class TargetInfo(
        val targetName: String,
        val sysRoot: (Project) -> File,
        val clangArgs: List<String> = emptyList()
    ) {
        val cacheKey: String by lazy {
            (clangArgs + targetName).joinToString("-")
        }
    }

    fun obtainWrapper(
        project: Project,
        execOperations: ExecOperations,
        konanTarget: KonanTarget
    ) = KonanCompilerWrapper(
        project = project,
        execOperations = execOperations,
        konanTarget = konanTarget
    )

    private fun targetInfoFromProps(target: KonanTarget): TargetInfo {
        return TargetInfo(
            targetName = konanProps.targetTriple(target),
            sysRoot = { project ->
                val appleSdkRoot = getAppleSdkRoot(target)
                if (appleSdkRoot != null) {
                    File(appleSdkRoot)
                } else {
                    konanDeps.resolve(
                        konanProps.sysroot(target)
                    )
                }
            },
            clangArgs = emptyList()
        )
    }

    private fun Project.ndkSysrootDir(): File {
        val libraryComponents =
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
        val androidLibrary = project.extensions.findByType(LibraryExtension::class.java)
            ?: error("cannot find library extension on $project")
        // hack, for some reason, sdkComponents.ndkDirectory is NOT set so we default to sdk/ndk
        val ndkVersion = androidLibrary.ndkVersion
        val ndkDir =
            libraryComponents.sdkComponents.sdkDirectory.get().asFile.resolve("ndk/$ndkVersion/sysroot")
        check(ndkDir.exists()) {
            "NDK directory is missing: ${ndkDir.absolutePath}"
        }
        return ndkDir
    }

    private fun getAppleSdkRoot(target: KonanTarget): String? {
        // https://github.com/JetBrains/kotlin-native/blob/bb568a8a7e529a9eae473432a71c13ca105ba5ee/shared/src/main/kotlin/org/jetbrains/kotlin/konan/target/Xcode.kt
        val sdkName = when (target.family) {
            Family.IOS -> {
                when (target.architecture) {
                    Architecture.ARM64, Architecture.ARM32 -> "iphoneos"
                    Architecture.X86, Architecture.X64 -> "iphonesimulator"
                    else -> "unexpected sdk param: $target"
                }
            }
            Family.OSX -> "macosx"
            Family.WATCHOS -> when (target.architecture) {
                Architecture.ARM64, Architecture.ARM32 -> "watchos"
                Architecture.X86, Architecture.X64 -> "watchsimulator"
                else -> "unexpected sdk param: $target"
            }
            Family.TVOS -> when (target.architecture) {
                Architecture.ARM64, Architecture.ARM32 -> "appletvos"
                Architecture.X86, Architecture.X64 -> "appletvsimulator"
                else -> "unexpected sdk param: $target"
            }
            else -> null
        }
        return sdkName?.let {
            xcrun("--sdk", it, "--show-sdk-path")
        }
    }

    private fun xcrun(vararg args: String): String = try {
        Runtime.getRuntime().exec(arrayOf("/usr/bin/xcrun") + args).inputStream.reader(
            Charsets.UTF_8
        ).readText().trim()
    } catch (e: Throwable) {
        throw RuntimeException("cannot call xcrun $args", e)
    }

    class KonanCompilerWrapper(
        val project: Project,
        val execOperations: ExecOperations,
        val konanTarget: KonanTarget
    ) {
        private val konanTargetInfo by lazy {
            targetInfoFromProps(konanTarget)
        }
        fun canCompile() = konanTarget.isBuiltOnThisMachine()

        private fun cacheKeyInputs() = listOf(
            konanTarget.name, project.path, konanTargetInfo.cacheKey
        )
        val cacheKey by lazy {
            cacheKeyInputs().joinToString(", ")
        }

        fun compile(
            args: List<String>
        ) {
            obtainNativeCompiler(project, execOperations, konanTarget)
            val konanTargetInfo = targetInfoFromProps(konanTarget)
            execOperations.exec {
                it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
                it.executable(llvmBinFolder.resolve("clang").absolutePath)
                val targetInfo = konanTargetInfo
                it.args("--compile", "-Wall")
                if (konanTarget.family == Family.ANDROID) {
                    it.args("-Oz") // optimize for size
                } else {
                    it.args("-O3")
                }

                if (konanTarget.family != Family.MINGW) {
                    it.args("-fPIC")
                }
                it.args("--target=${targetInfo.targetName}")
                val sysRoot = targetInfo.sysRoot(project)
                it.args("--sysroot=${sysRoot.absolutePath}")
                it.args(targetInfo.clangArgs)
                it.args(args)
            }
        }
        fun archiveNativeBinary(
            input: File,
            output: File,
        ) {
            obtainNativeCompiler(project, execOperations, konanTarget)
            execOperations.exec {
                it.executable(llvmBinFolder.resolve("llvm-ar").absolutePath)
                it.args(
                    "rc", output.absolutePath,
                    input.absolutePath
                )
                it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
            }
        }
        companion object {
            private val downloadNativeCompilerLock = ReentrantLock()
            private fun obtainNativeCompiler(
                project: Project,
                execOperations: ExecOperations,
                konanTarget: KonanTarget
            ) {
                downloadNativeCompilerLock.withLock {
                    val nativeCompilerDownloader = NativeCompilerDownloader(
                        project = project
                    )
                    nativeCompilerDownloader.downloadIfNeeded()
                    val result = execOperations.exec {
                        val konancName = if (HostManager.hostIsMingw) {
                            "konanc.bat"
                        } else {
                            "konanc"
                        }
                        val konanc = nativeCompilerDownloader.compilerDirectory
                            .resolve("bin/$konancName")
                        check(konanc.exists()) {
                            "Cannot find konan compiler at $konanc"
                        }
                        it.executable = konanc.absolutePath
                        it.args("-Xcheck-dependencies", "-target", konanTarget.visibleName)
                    }
                    result.assertNormalExitValue()
                }
            }
        }
    }
}
