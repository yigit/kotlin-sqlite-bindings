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

import com.birbit.ksqlite.build.internal.BuildOnServer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.gradle.api.specs.AndSpec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

class KSqliteBuildPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        BuildOnServer.initIfNecessary(target)
        target.extensions.create<KSqliteBuildExtension>(
            "ksqliteBuild"
        )
        target.disableCinteropUpToDateChecks()
        target.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            it.kotlinOptions.jvmTarget = "1.8"
        }
        target.tasks.withType<KotlinNativeCompile>().configureEach {
            it.compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    private fun Project.disableCinteropUpToDateChecks() {
        tasks.withType<CInteropProcess>().configureEach {
            // workaround for https://youtrack.jetbrains.com/issue/KT-52243/
            it.clearUpToDateChecks()
        }
    }

    /**
     * some KMP tasks have unreasonable up to date checks that completely blocks the caching.
     */
    private fun CInteropProcess.clearUpToDateChecks() {
        val outputClass = outputs::class.java
        outputClass.getDeclaredField("upToDateSpec").let { field ->
            check(field.trySetAccessible())
            field.set(outputs, AndSpec.empty<TaskInternal>())
        }
    }
}
