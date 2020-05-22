package com.birbit.ksqlite.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.presetName
import java.io.File
import java.io.Serializable

data class SoInput(
    val presetName:String,
    val soFile:File
) : Serializable {
    fun folderName() = when(presetName) {
        "macosX64" -> "osx_64"
        "linuxX64" -> "linux_64"
        else -> throw GradleException("don't know how to rename $presetName. see: https://github.com/scijava/native-lib-loader")
    }
}

abstract class CollectNativeLibrariesTask : DefaultTask() {
    @Input
    lateinit var soInputs : List<SoInput>
    @OutputDirectory
    lateinit var outputDir : File

    @TaskAction
    fun doIt() {
        outputDir.delete()
        outputDir.mkdirs()
        val nativeDir = outputDir.resolve("natives")
        soInputs.forEach {
            nativeDir.resolve(it.folderName()).resolve(it.soFile.name).let {outFile ->
                outFile.parentFile.mkdirs()
                outFile.writeBytes(it.soFile.readBytes())
            }
        }
    }

    companion object {

        fun create(project:Project, namePrefix: String, outFolder:File): TaskProvider<CollectNativeLibrariesTask> {
            return project.tasks.register("collectSharedLibsFor${namePrefix.capitalize()}", CollectNativeLibrariesTask::class.java) {
                configure(it, namePrefix, outFolder)
            }
        }

        fun configure(task : CollectNativeLibrariesTask, namePrefix:String, outFolder:File) {
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
                soFiles.add(SoInput(
                    presetName = it.konanTarget.presetName,
                    soFile = sharedLib.outputFile
                ))
                task.dependsOn(sharedLib.linkTask)
            }
            task.soInputs = soFiles
            task.outputDir = outFolder
        }
    }
}