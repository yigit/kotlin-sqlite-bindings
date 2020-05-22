package com.birbit.ksqlite.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
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
        fun folderName(konanTarget: KonanTarget) : String {
            // see https://github.com/scijava/native-lib-loader/blob/master/src/main/java/org/scijava/nativelib/NativeLibraryUtil.java
            return when(konanTarget.family) {
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
    @Input
    lateinit var soInputs: List<SoInput>

    @OutputDirectory
    lateinit var outputDir: File

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
            kotlin.targets.withType(KotlinNativeTarget::class.java) {
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
            task.soInputs = soFiles
            task.outputDir = outFolder
        }
    }
}