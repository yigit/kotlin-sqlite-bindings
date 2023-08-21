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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class CreateDefFileWithLibraryPathTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val original: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val soFile: RegularFileProperty

    @get:OutputFile
    abstract val target: RegularFileProperty

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun doIt() {
        println("will copy from $original to $target")
        val target = target.asFile.get()
        target.parentFile.mkdirs()
        // use relative path to the owning project so it can be cached.
        val soLocalPath = soFile.asFile.get().parentFile.relativeTo(projectDir.get().asFile)
        val content = original.asFile.get().readText(Charsets.UTF_8) + System.lineSeparator() + "libraryPaths = \"$soLocalPath\"" +
            System.lineSeparator()
        println("new content: $content")
        target.writeText(content, Charsets.UTF_8)
    }
}
