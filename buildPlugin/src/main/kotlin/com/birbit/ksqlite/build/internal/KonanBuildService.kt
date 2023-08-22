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
package com.birbit.ksqlite.build.internal

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

internal abstract class KonanBuildService
@Inject constructor(
    private val execOperations: ExecOperations
) :
    BuildService<KonanBuildService.Params> {
    interface Params : BuildServiceParameters {
        val compilerPath: Property<File>
    }

    fun obtainWrapper(
        konanTarget: KonanTarget
    ) = KonanUtil.KonanCompilerWrapper(
        execOperations = execOperations,
        konanTarget = konanTarget
    ).also {
        KonanUtil.KonanCompilerWrapper.ensureSupportForTarget(
            parameters.compilerPath.get(),
            execOperations,
            konanTarget
        )
    }
    companion object {
        const val KEY = "konanBuildService"
        fun register(
            project: Project
        ): Provider<KonanBuildService> {
            val compilerPath = KonanUtil.KonanCompilerWrapper.obtainNativeCompiler(
                project
            )
            return project.gradle.sharedServices.registerIfAbsent(
                KEY,
                KonanBuildService::class.java
            ) {
                it.parameters.compilerPath.set(compilerPath)
            }
        }
    }
}
