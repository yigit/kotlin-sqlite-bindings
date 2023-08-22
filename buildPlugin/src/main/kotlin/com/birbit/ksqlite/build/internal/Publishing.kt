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
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

internal object Publishing {

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
            kotlin.androidTarget {
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
}
