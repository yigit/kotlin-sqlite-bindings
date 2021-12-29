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

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

internal object Publishing {
    fun createCombinedRepoTaskIfPossible(
        rootProject: Project
    ) {
        val distOutputsFolder = getDistOutputs() ?: return
        val repoFolders = distOutputsFolder.walkTopDown().filter {
            it.name == "repo" && it.isDirectory
        }
        rootProject.tasks.register(BUILD_COMBINED_REPO_TASK_NAME, Copy::class.java) { copyTask ->
            repoFolders.forEach {
                copyTask.from(it)
            }
            copyTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            copyTask.from(BuildOnServer.getOutRepo())
            copyTask.destinationDir = rootProject.buildDir.resolve("dist/combinedRepo")
            rootProject.subprojects { subProject ->
                if (subProject.pluginManager.hasPlugin("maven-publish")) {
                    copyTask.dependsOn(subProject.tasks.named("publish"))
                }
            }
        }
    }

    fun setup(project: Project) {
        val publishing = project.extensions.findByType<PublishingExtension>()
            ?: error("cannot find publishing extension")
        publishing.repositories {
            it.maven("file://${BuildOnServer.getOutRepo().absolutePath}")
        }
        val buildId = System.getenv("GITHUB_RUN_ID")?.padStart(10, '0')
        project.group = "com.birbit.ksqlite3"
        project.version = if (buildId != null) {
            "0.1.0.$buildId"
        } else {
            "0.1.0-SNAPSHOT"
        }
        // if it is android, enable publishing for debug & release
        val android = project.extensions.findByType<LibraryExtension>()
        if (android != null) {
            val kotlin = project.extensions.findByType<KotlinMultiplatformExtension>()
                ?: error("must apply KMP extension")
            kotlin.android {
                publishLibraryVariants("debug", "release")
            }
        }
    }

    /**
     * outputs of other builds in CI
     */
    fun getDistOutputs(): File? {
        return System.getenv(DIST_OUTPUTS_ENV_VAR)?.let {
            File(it).let {
                check(it.exists()) {
                    "invalid $DIST_OUTPUTS_ENV_VAR param"
                }
                it.normalize()
            }
        }
    }

    private const val DIST_OUTPUTS_ENV_VAR = "DIST_OUTPUTS"
    private const val BUILD_COMBINED_REPO_TASK_NAME = "createCombinedRepo"
}
