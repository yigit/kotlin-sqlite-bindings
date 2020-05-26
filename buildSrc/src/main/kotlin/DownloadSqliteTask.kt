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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.net.URL

abstract class DownloadSqliteTask : DefaultTask() {
    @Input
    lateinit var version: String

    @OutputFile
    lateinit var downloadTargetFile: File

    @TaskAction
    fun doIt() {
        downloadTargetFile.delete()
        downloadTargetFile.parentFile.mkdirs()
        val downlodUrl = computeDownloadUrl(version)
        URL(downlodUrl).openStream().use { inputStream ->
            FileOutputStream(downloadTargetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        println("downloaded file to $downloadTargetFile")
    }

    private fun computeDownloadUrl(version: String): String {
        // see https://www.sqlite.org/download.html
        // The version is encoded so that filenames sort in order of increasing version number
        // when viewed using "ls".
        // For version 3.X.Y the filename encoding is 3XXYY00.
        // For branch version 3.X.Y.Z, the encoding is 3XXYYZZ.
        val sections = version.split('.')
        check(sections.size >= 3) { // TODO add support for branch versions
            "invalid sqlite version $version"
        }
        val major = sections[0].toInt()
        val minor = sections[1].toInt()
        val patch = sections[2].toInt()
        val branch = if (sections.size >= 4) sections[3].toInt() else 0
        val fileName = String.format("%d%02d%02d%02d.zip", major, minor, patch, branch)
        println("filename: $fileName")
        return "$BASE_URL$fileName"
    }

    companion object {
        private const val BASE_URL = "https://www.sqlite.org/2020/sqlite-amalgamation-"
    }
}