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

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @Input
    lateinit var downlodUrl: String

    @OutputFile
    lateinit var downloadTargetFile: File

    @TaskAction
    fun doIt() {
        downloadTargetFile.delete()
        downloadTargetFile.parentFile.mkdirs()
        URL(downlodUrl).openStream().use { inputStream ->
            FileOutputStream(downloadTargetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        println("downloaded $downlodUrl to $downloadTargetFile")
    }
}
