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
import com.android.build.api.extension.LibraryAndroidComponentsExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

internal object KonanUtil {
    // taken from https://github.com/Dominaezzz/kotlin-sqlite
    private val konanUserDir = File(
        System.getenv("KONAN_DATA_DIR")
            ?: "${System.getProperty("user.home")}/.konan"
    )
    private val konanDeps = konanUserDir.resolve("dependencies")
    private val toolChainFolderName = when {
        HostManager.hostIsLinux -> "clang-llvm-8.0.0-linux-x86-64"
        HostManager.hostIsMac -> "clang-llvm-apple-8.0.0-darwin-macos"
        HostManager.hostIsMingw -> "msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1"
        else -> error("Unknown host OS")
    }
    private val llvmBinFolder = konanDeps.resolve("$toolChainFolderName/bin")

    internal class TargetInfo(
        val targetName: String,
        val sysRoot: (Project) -> File,
        val clangArgs: List<String> = emptyList()
    )

    fun registerArchiveTask(
        project: Project,
        prefix: String,
        konanTarget: KonanTarget,
        input: File,
        output: File,
        configure: (Exec) -> Unit
    ): TaskProvider<Exec> {
        return project.tasks.register("$prefix${konanTarget.presetName.capitalize()}", Exec::class.java) {
            it.onlyIf { HostManager().isEnabled(konanTarget) }

            it.inputs.file(input)
            it.outputs.file(output)

            it.executable(llvmBinFolder.resolve("llvm-ar").absolutePath)
            it.args(
                "rc", output.absolutePath,
                input.absolutePath
            )
            it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
            configure(it)
        }
    }

    fun registerCompilationTask(
        project: Project,
        prefix: String,
        konanTarget: KonanTarget,
        configure: (Exec) -> Unit
    ): TaskProvider<Exec> {
        return project.tasks.register("$prefix${konanTarget.presetName.capitalize()}", Exec::class.java) {
            it.onlyIf { HostManager().isEnabled(konanTarget) }
            // we need konan executables downloaded and this is a nice hacky way to get them :)
            // TODO figure out how to get these download dependencies properly
            it.dependsOn(project.rootProject.findProject(":konan-warmup")!!.tasks.named("assemble"))

            it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
            if (HostManager.hostIsMac && konanTarget == KonanTarget.MACOS_X64) {
                it.environment(
                    "CPATH",
                    "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/" +
                        "SDKs/MacOSX.sdk/usr/include"
                )
            }
            it.executable(llvmBinFolder.resolve("clang").absolutePath)
            it.args("--compile", "-Wall")
            if (konanTarget.family == Family.ANDROID) {
                it.args("-Oz") // optimize for size
            } else {
                it.args("-O3")
            }

            if (konanTarget.family != Family.MINGW) {
                it.args("-fPIC")
            }
            val targetInfo = targetInfoMap[konanTarget] ?: error("missing target info $konanTarget")
            it.args("--target=${targetInfo.targetName}")
            it.args("--sysroot=${targetInfo.sysRoot(project).absolutePath}")
            it.args(targetInfo.clangArgs)
            configure(it)
        }
    }

    private val targetInfoMap = mapOf(
        KonanTarget.LINUX_X64 to TargetInfo(
            "x86_64-unknown-linux-gnu",
            { konanDeps.resolve("target-gcc-toolchain-3-linux-x86-64/x86_64-unknown-linux-gnu/sysroot") }
        ),
        KonanTarget.MACOS_X64 to TargetInfo(
            "x86_64-apple-darwin10", // Not sure about this but it doesn't matter yet.
            { konanDeps.resolve("target-sysroot-10-macos_x64") }
        ),
        KonanTarget.IOS_ARM64 to TargetInfo(
            "arm64-apple-darwin10", // Not sure about this but it doesn't matter yet.
            { File("/Applications/Xcode.app/Contents/Developer/Platforms/" +
                    "iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk/") },
            listOf("-framework", "Foundation")
        ),
        KonanTarget.IOS_X64 to TargetInfo(
            "x86_64-apple-ios10.3-simulator",
            { File("/Applications/Xcode.app/Contents/Developer/Platforms/" +
                    "iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk/") },
            listOf("-framework", "Foundation")
        ),
        KonanTarget.MINGW_X64 to TargetInfo(
            "x86_64-w64-mingw32",
            { konanDeps.resolve("msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1") }
        ),
        KonanTarget.MINGW_X86 to TargetInfo(
            "i686-w64-mingw32",
            { konanDeps.resolve("msys2-mingw-w64-i686-clang-llvm-lld-compiler_rt-8.0.1") }
        ),
        KonanTarget.LINUX_ARM32_HFP to TargetInfo(
            "armv6-unknown-linux-gnueabihf",
            // YOLO
            { konanDeps.resolve("arm-unknown-linux-gnueabihf-gcc-8.3.0-glibc-2.19-kernel-4.9-2/" +
                    "arm-unknown-linux-gnueabihf/sysroot/") },
            listOf("-mfpu=vfp", "-mfloat-abi=hard")
        ),
        KonanTarget.ANDROID_ARM32 to TargetInfo(
            "arm-linux-androideabi",
            { it.ndkSysrootDir() }
        ),
        KonanTarget.ANDROID_ARM64 to TargetInfo(
            "aarch64-linux-android",
            { it.ndkSysrootDir() }
        ),
        KonanTarget.ANDROID_X86 to TargetInfo(
            "i686-linux-android",
            { it.ndkSysrootDir() }
        ),
        KonanTarget.ANDROID_X64 to TargetInfo(
            "x86_64-linux-android",
            { it.ndkSysrootDir() }
        )
    )

    private fun Project.ndkSysrootDir(): File {
        val libraryComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
        val androidLibrary = project.extensions.findByType(LibraryExtension::class.java)
                ?: error("cannot find library extension on $project")
        // hack, for some reason, sdkComponents.ndkDirectory is NOT set so we default to sdk/ndk
        val ndkVersion = androidLibrary.ndkVersion
        val ndkDir = libraryComponents.sdkComponents.sdkDirectory.get().asFile.resolve("ndk/$ndkVersion/sysroot")
        check(ndkDir.exists()) {
            println("NDK directory is missing")
        }
        return ndkDir
    }
}
