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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Paths

@CacheableTask
abstract class CreateDefFileWithLibraryPathTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var original: File

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var soFilePath: File

    @get:OutputFile
    val target = project.objects.fileProperty()

    // include project path as well so that the abs paths we put into the def file
    // does not make us restore bad cache
    @get:Input
    val projectPath: String
        get() = project.buildDir.canonicalPath

    @TaskAction
    fun doIt() {
        println("will copy from $original to $target")
        val target = target.asFile.get()
        target.parentFile.mkdirs()
        val soLocalPath = Paths.get(soFilePath.parentFile.absolutePath)
        val content = original.readText(Charsets.UTF_8) + System.lineSeparator() + "libraryPaths = \"$soLocalPath\"" +
            System.lineSeparator()
        println("new content: $content")
        target.writeText(content, Charsets.UTF_8)
    }
}
