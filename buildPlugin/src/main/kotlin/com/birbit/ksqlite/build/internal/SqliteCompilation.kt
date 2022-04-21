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

import com.birbit.ksqlite.build.CreateDefFileWithLibraryPathTask
import com.birbit.ksqlite.build.SqliteCompilationConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.presetName
import java.io.File
import java.util.Locale

@OptIn(kotlin.ExperimentalStdlibApi::class)
internal object SqliteCompilation {
    fun setup(project: Project, config: SqliteCompilationConfig) {
        val buildFolder = project.buildDir.resolve("sqlite-compilation")
        val generatedDefFileFolder = project.buildDir.resolve("sqlite-def-files")
        // TODO convert these to gradle properties
        val downloadFile = buildFolder.resolve("download/amalgamation.zip")
        val srcDir = buildFolder.resolve("src")
        val downloadTask = project.tasks.register("downloadSqlite", DownloadTask::class.java) {
            it.downloadUrl = computeDownloadUrl(config.version)
            it.downloadTargetFile = downloadFile
        }

        val unzipTask = project.tasks.register("unzipSqlite", Copy::class.java) {
            it.from(project.zipTree(downloadFile))
            it.into(srcDir)
            it.eachFile {
                // get rid of the amalgamation folder in output dir
                it.path = it.path.replaceFirst("sqlite-amalgamation-[\\d]+/".toRegex(), "")
            }
            it.dependsOn(downloadTask)
        }

        val kotlinExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val compiledOutputDir = buildFolder.resolve("output")
        val compileTasks = mutableListOf<TaskProvider<out Task>>()
        val soFiles = mutableListOf<File>()
        kotlinExt.targets.withType(KotlinNativeTarget::class.java).filter { nativeTarget ->
            nativeTarget.konanTarget.isBuiltOnThisMachine()
        }.forEach {
            val konanTarget = it.konanTarget
            val targetDir = compiledOutputDir.resolve(konanTarget.presetName)

            val sourceFile = srcDir.resolve("sqlite3.c")
            val objFile = targetDir.resolve("sqlite3.o")
            val staticLibFile = targetDir.resolve("libsqlite3.a")
            val compileSQLite = KonanUtil.registerCompilationTask(
                project = project,
                prefix = "compileSQLite",
                konanTarget = konanTarget
            ) {
                it.dependsOn(unzipTask)
                it.inputs.file(sourceFile)
                it.outputs.file(objFile)
                it.args(
                    "-DSQLITE_ENABLE_COLUMN_METADATA=1",
                    "-DSQLITE_ENABLE_NORMALIZE=1",
                    // "-DSQLITE_ENABLE_EXPLAIN_COMMENTS=1",
                    // "-DSQLITE_ENABLE_DBSTAT_VTAB=1",
                    "-DSQLITE_ENABLE_LOAD_EXTENSION=1",
                    // "-DSQLITE_HAVE_ISNAN=1",
                    "-DHAVE_USLEEP=1",
                    // "-DSQLITE_CORE=1",
                    "-DSQLITE_ENABLE_FTS3=1",
                    "-DSQLITE_ENABLE_FTS3_PARENTHESIS=1",
                    "-DSQLITE_ENABLE_FTS4=1",
                    "-DSQLITE_ENABLE_FTS5=1",
                    "-DSQLITE_ENABLE_JSON1=1",
                    "-DSQLITE_ENABLE_RTREE=1",
                    "-DSQLITE_ENABLE_STAT4=1",
                    "-DSQLITE_THREADSAFE=1",
                    "-DSQLITE_DEFAULT_MEMSTATUS=0",
                    "-DSQLITE_OMIT_PROGRESS_CALLBACK=0",
                    "-DSQLITE_ENABLE_RBU=1"
                )
                // TODO these absolute paths in args will break caching in all likelihood
                it.argsWithoutCacheImpact(
                    "-I${srcDir.absolutePath}",
                    "-o", objFile.absolutePath,
                    sourceFile.absolutePath
                )
            }
            val archiveSQLite = KonanUtil.registerArchiveTask(
                project = project,
                konanTarget = konanTarget,
                prefix = "archiveSQLite",
                input = objFile,
                output = staticLibFile
            ) {
                it.dependsOn(compileSQLite)
            }
            compileTasks.add(archiveSQLite)
            soFiles.add(staticLibFile)
            it.compilations["main"].cinterops.create("sqlite") {
                // JDK is required here, JRE is not enough
                it.packageName = "sqlite3"
                val cInteropTask = project.tasks[it.interopProcessingTaskName]
                cInteropTask.dependsOn(unzipTask)
                cInteropTask.dependsOn(archiveSQLite)
                it.includeDirs(srcDir)
                val original = it.defFile
                val newDefFile = generatedDefFileFolder.resolve("${konanTarget.presetName}/sqlite-generated.def")
                val createDefFileTask = project.tasks.register(
                    "createDefFileForSqlite${konanTarget.presetName.capitalize(Locale.US)}",
                    CreateDefFileWithLibraryPathTask::class.java
                ) { task ->
                    task.dependsOn(archiveSQLite)
                    task.original = original
                    task.target = newDefFile
                    task.soFilePath = staticLibFile
                }
                // create def file w/ library paths. couldn't figure out how else to add it :/ :)
                it.defFile = newDefFile
                cInteropTask.dependsOn(createDefFileTask)
            }
        }
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

    private const val BASE_URL = "https://www.sqlite.org/2020/sqlite-amalgamation-"
}
