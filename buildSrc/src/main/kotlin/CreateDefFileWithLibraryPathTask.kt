package com.birbit.ksqlite.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Paths

abstract class CreateDefFileWithLibraryPathTask : DefaultTask() {
    @InputFile
    lateinit var original : File
    @InputFile
    lateinit var soFilePath : File
    @OutputFile
    lateinit var target : File

    @TaskAction
    fun doIt() {
        println("will copy from ${original} to ${target}")
        target.parentFile.mkdirs()
        val soLocalPath = Paths.get(soFilePath.parentFile.absolutePath)
        val content = original.readText(Charsets.UTF_8) + System.lineSeparator() + "libraryPaths=\"${soLocalPath}\""
        println("new content: $content")
        target.writeText(content, Charsets.UTF_8)
    }
}