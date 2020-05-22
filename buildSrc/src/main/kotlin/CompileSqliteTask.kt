package com.birbit.ksqlite.build

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

abstract class CompileSqliteTask : Exec() {
    @InputDirectory
    lateinit var srcDir: File

    @OutputFile
    lateinit var outputDir: File

    @TaskAction
    fun doIt() {
        println("runing compile task from $srcDir into $outputDir")
    }

    companion object {
        fun configure(task: CompileSqliteTask) {

        }
    }
}