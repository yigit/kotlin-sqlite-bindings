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

import com.birbit.ksqlite.build.CollectNativeLibrariesTask
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider

object BuildOnServer {
    private lateinit var distDir: File
    val TASK_NAME = "buildOnServer"
    private fun buildOnServerTask(project: Project): TaskProvider<Task> {
        val rootProject = project.rootProject
        if (rootProject.tasks.findByPath(TASK_NAME) == null) {
            distDir = rootProject.buildDir.resolve("dist")
            rootProject.tasks.register(TASK_NAME)
            configureCopyNativeLibraries(rootProject)
            Publishing.createCombinedRepoTaskIfPossible(rootProject)
        }
        return rootProject.tasks.named(TASK_NAME)
    }

    fun initIfNecessary(project: Project) {
        buildOnServerTask(project)
    }

    fun register(project: Project) {
        val rootTask = buildOnServerTask(project)
        rootTask.configure {
            it.dependsOn(project.tasks.named("spotlessCheck"))
            it.dependsOn(project.tasks.named("allTests"))
            if (project.pluginManager.hasPlugin("maven-publish")) {
                it.dependsOn(project.tasks.named("publish"))
            }
        }
    }

    fun getOutRepo(): File {
        return distDir.resolve("repo")
    }

    private fun configureCopyNativeLibraries(rootProject: Project) {
        val copyNativeLibsTask = rootProject.tasks.register("copyNativeLibs", Copy::class.java) { copyTask ->
            rootProject.childProjects["sqlitebindings"]!!.tasks.withType(CollectNativeLibrariesTask::class.java)
                .all {
                    copyTask.from(it.outputDir)
                    copyTask.dependsOn(it)
                }
            copyTask.destinationDir = distDir.resolve("nativeLibs")
        }
        rootProject.tasks.named(TASK_NAME).configure {
            it.dependsOn(copyNativeLibsTask)
        }
    }
}
