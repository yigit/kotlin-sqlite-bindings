/*
 * Copyright 2023 Google, LLC.
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
package com.birbit.ksqlite.build.internal.clang

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class ClangCompileParameters {
    @get:Input
    abstract val konanTarget: Property<KonanTarget>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: DirectoryProperty

    @get:Input
    abstract val freeArgs: ListProperty<String>
}

abstract class CompileWorker : WorkAction<CompileWorker.Params> {
    interface Params : WorkParameters {
        val clangParameters: Property<ClangCompileParameters>
        val buildService: Property<KonanBuildService>
    }

    override fun execute() {
        val buildService = parameters.buildService.get()
        buildService.compile(
            parameters.clangParameters.get()
        )
    }
}

@CacheableTask
abstract class ClangCompileTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {
    @Suppress("UnstableApiUsage")
    @get:ServiceReference(KonanBuildService.KEY)
    abstract val konanBuildService: Property<KonanBuildService>

    @get:Nested
    abstract val clangParameters: ClangCompileParameters

    @TaskAction
    fun compile() {
        workerExecutor.noIsolation().submit(
            CompileWorker::class.java
        ) {
            it.buildService.set(konanBuildService)
            it.clangParameters.set(clangParameters)
        }
    }
}
