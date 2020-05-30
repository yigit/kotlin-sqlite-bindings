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

import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

object BuildOnServer {
    private lateinit var rootProject: Project
    private lateinit var distDir: File
    val TASK_NAME = "buildOnServer"
    fun init(project: Project) {
        rootProject = project.rootProject
        distDir = rootProject.buildDir.resolve("dist")
        val buildOnServerTask = rootProject.tasks.register(TASK_NAME)
        rootProject.subprojects { subProject ->
            if (subProject.name != "jnigenerator") {
                buildOnServerTask.configure {
                    it.dependsOn(subProject.tasks.named("spotlessCheck"))
                    it.dependsOn(subProject.tasks.named("allTests"))
                    if (subProject.pluginManager.hasPlugin("maven-publish")) {
                        it.dependsOn(subProject.tasks.named("publish"))
                    }
                }
            }
        }
        configureCopyNativeLibraries()
        Publishing.createCombinedRepoTaskIfPossible(rootProject)
    }

    fun getOutRepo(): File {
        return distDir.resolve("repo")
    }

    private fun configureCopyNativeLibraries() {
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
