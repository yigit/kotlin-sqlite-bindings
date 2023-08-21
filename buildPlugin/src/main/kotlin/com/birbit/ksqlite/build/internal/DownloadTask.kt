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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.net.URL

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val downloadUrl: Property<String>

    @get:OutputFile
    abstract val downloadTargetFile: RegularFileProperty

    @TaskAction
    fun doIt() {
        val downloadTargetFile = downloadTargetFile.asFile.get()
        downloadTargetFile.delete()
        downloadTargetFile.parentFile.mkdirs()
        URL(downloadUrl.get()).openStream().use { inputStream ->
            FileOutputStream(downloadTargetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        println("downloaded $downloadUrl to $downloadTargetFile")
    }
}
