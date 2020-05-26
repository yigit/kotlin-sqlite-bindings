/*
 * Copyright 2020 Google, Inc.
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Paths

abstract class CreateDefFileWithLibraryPathTask : DefaultTask() {
    @InputFile
    lateinit var original: File

    @InputFile
    lateinit var soFilePath: File

    @OutputFile
    lateinit var target: File

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