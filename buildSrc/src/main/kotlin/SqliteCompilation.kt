package com.birbit.ksqlite.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.Callable

data class SqliteCompilationConfig(
    val version: String
)

data class SqliteCompilationOutputs(
    val srcDir: File,
    val compileTasks : List<TaskProvider<out Task>>,
    val soFiles: List<File>
)

// taken from https://github.com/Dominaezzz/kotlin-sqlite
val konanUserDir = File(System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan")
val konanDeps = konanUserDir.resolve("dependencies")
val toolChainFolderName = when {
    HostManager.hostIsLinux -> "clang-llvm-8.0.0-linux-x86-64"
    HostManager.hostIsMac -> "clang-llvm-apple-8.0.0-darwin-macos"
    HostManager.hostIsMingw -> "msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1"
    else -> error("Unknown host OS")
}
val llvmBinFolder = konanDeps.resolve("${toolChainFolderName}/bin")

val androidSysRootParent = konanDeps.resolve("target-sysroot-1-android_ndk").resolve("android-21")

data class TargetInfo(val targetName: String, val sysRoot: File, val clangArgs: List<String> = emptyList())

val targetInfoMap = mapOf(
    KonanTarget.LINUX_X64 to TargetInfo(
        "x86_64-unknown-linux-gnu",
        konanDeps.resolve("target-gcc-toolchain-3-linux-x86-64/x86_64-unknown-linux-gnu/sysroot")
    ),
    KonanTarget.MACOS_X64 to TargetInfo(
        "x86_64-apple-darwin10", // Not sure about this but it doesn't matter yet.
        konanDeps.resolve("target-sysroot-10-macos_x64")
    ),
    KonanTarget.MINGW_X64 to TargetInfo(
        "x86_64-w64-mingw32",
        konanDeps.resolve("msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1")
    ),
    KonanTarget.MINGW_X86 to TargetInfo(
        "i686-w64-mingw32",
        konanDeps.resolve("msys2-mingw-w64-i686-clang-llvm-lld-compiler_rt-8.0.1")
    ),
    KonanTarget.LINUX_ARM32_HFP to TargetInfo(
        "armv6-unknown-linux-gnueabihf",
        konanDeps.resolve("target-sysroot-2-raspberrypi"),
        listOf("-mfpu=vfp", "-mfloat-abi=hard")
    ),
    KonanTarget.ANDROID_ARM32 to TargetInfo(
        "arm-linux-androideabi",
        androidSysRootParent.resolve("arch-arm")
    ),
    KonanTarget.ANDROID_ARM64 to TargetInfo(
        "aarch64-linux-android",
        androidSysRootParent.resolve("arch-arm64")
    ),
    KonanTarget.ANDROID_X86 to TargetInfo(
        "i686-linux-android",
        androidSysRootParent.resolve("arch-x86")
    ),
    KonanTarget.ANDROID_X64 to TargetInfo(
        "x86_64-linux-android",
        androidSysRootParent.resolve("arch-x64")
    )
)


// TODO: could be a plugin instead?
object SqliteCompilation {
    fun setup(project: Project, config: SqliteCompilationConfig) {
        val buildFolder = project.buildDir.resolve("sqlite-compilation")
        val generatedDefFileFolder = project.buildDir.resolve("sqlite-def-files")
        // TODO convert these to gradle properties
        val downloadFile = buildFolder.resolve("download/amalgamation.zip")
        val srcDir = buildFolder.resolve("src")
        val downloadTask = project.tasks.register("downloadSqlite", DownloadSqliteTask::class.java) {
            it.version = config.version
            it.downloadTargetFile = downloadFile
        }

        val unzipTask = project.tasks.register("unzipSqlite", Copy::class.java) {
            it.from(project.zipTree(downloadFile))
            it.into(srcDir)
            it.eachFile {
                // get rid of the amalgamation folder in output dir
                it.path = it.path.replaceFirst("sqlite-amalgamation-[\\d]+/".toRegex(), "")
            }
            it.dependsOn(downloadTask)
        }

        val kotlinExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val compiledOutputDir = buildFolder.resolve("output")
        val compileTasks = mutableListOf<TaskProvider<out Task>>()
        val soFiles = mutableListOf<File>()
        kotlinExt.targets.withType(KotlinNativeTarget::class.java) {
            val konanTarget = it.konanTarget
            val targetDir = compiledOutputDir.resolve(konanTarget.presetName)

            val sourceFile = srcDir.resolve("sqlite3.c")
            val objFile = targetDir.resolve("sqlite3.o")
            val staticLibFile = targetDir.resolve("libsqlite3.a")

            val compileSQLite =
                project.tasks.register("compileSQLite${konanTarget.presetName.capitalize()}", Exec::class.java) {
                    it.onlyIf { HostManager().isEnabled(konanTarget) }

                    it.dependsOn(unzipTask)
                    it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
                    if (HostManager.hostIsMac && konanTarget == KonanTarget.MACOS_X64) {
                        it.environment(
                            "CPATH",
                            "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include"
                        )
                    }

                    it.inputs.file(sourceFile)
                    it.outputs.file(objFile)

                    it.executable(llvmBinFolder.resolve("clang").absolutePath)
                    it.args("-c", "-Wall", "-fPIC")

                    val targetInfo = targetInfoMap.getValue(konanTarget)

                    it.args("--target=${targetInfo.targetName}", "--sysroot=${targetInfo.sysRoot}")
                    it.args(targetInfo.clangArgs)
                    it.args(
                        "-DSQLITE_ENABLE_FTS3",
                        "-DSQLITE_ENABLE_FTS5",
                        "-DSQLITE_ENABLE_RTREE",
                        "-DSQLITE_ENABLE_DBSTAT_VTAB",
                        "-DSQLITE_ENABLE_JSON1",
                        "-DSQLITE_ENABLE_RBU"
                    )
                    it.args(
                        "-I${srcDir.absolutePath}",
                        "-o", objFile.absolutePath,
                        sourceFile.absolutePath
                    )
                }
            val archiveSQLite =
                project.tasks.register("archiveSQLite${konanTarget.presetName.capitalize()}", Exec::class.java) {
                    it.onlyIf { HostManager().isEnabled(konanTarget) }
                    it.dependsOn(compileSQLite)

                    it.inputs.file(objFile)
                    it.outputs.file(staticLibFile)

                    it.executable(llvmBinFolder.resolve("llvm-ar").absolutePath)
                    it.args(
                        "rc", staticLibFile.absolutePath,
                        objFile.absolutePath
                    )
                    it.environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
                }
            compileTasks.add(archiveSQLite)
            soFiles.add(staticLibFile)
            it.compilations["main"].cinterops.create("sqlite") {
                // JDK is required here, JRE is not enough
                it.packageName = "sqlite3"
                val cInteropTask = project.tasks[it.interopProcessingTaskName]
                cInteropTask.dependsOn(unzipTask)
                cInteropTask.dependsOn(archiveSQLite)
                it.includeDirs(
                    Callable { srcDir }
                )
                val original = it.defFile
                val newDefFile = generatedDefFileFolder.resolve("${konanTarget.presetName}/sqlite-generated.def")
                val createDefFileTask = project.tasks.register("createDefFileForSqlite${konanTarget.presetName.capitalize()}", CreateDefFileWithLibraryPathTask::class.java) {task ->
                    task.original = original
                    task.target = newDefFile
                    task.soFilePath =  staticLibFile
                }
                // create def file w/ library paths. couldn't figure out how else to add it :/ :)
                it.defFile = newDefFile
                cInteropTask.dependsOn(createDefFileTask)            }
        }
    }
}