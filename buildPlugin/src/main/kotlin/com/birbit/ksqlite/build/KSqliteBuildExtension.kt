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

import com.birbit.ksqlite.build.internal.AndroidSetup
import com.birbit.ksqlite.build.internal.BuildOnServer
import com.birbit.ksqlite.build.internal.JniSetup
import com.birbit.ksqlite.build.internal.Publishing
import com.birbit.ksqlite.build.internal.SqliteCompilation
import com.birbit.ksqlite.build.internal.setupCommon
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

open class KSqliteBuildExtension
@Inject constructor(
    private val project: Project,
    private val execOperations: ExecOperations
) {
    fun android() {
        AndroidSetup.configure(project)
    }

    fun publish() {
        Publishing.setup(project)
    }

    fun native(
        includeAndroidNative: Boolean = false,
        includeJni: Boolean = false,
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val kmpExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            ?: error("Must apply KMP plugin")
        kmpExt.setupCommon(
            includeAndroidNative = includeAndroidNative,
            configure = {
                if (includeJni) {
                    JniSetup.configure(this)
                }
                this.configure()
            }
        )
    }

    fun includeSqlite(config: SqliteCompilationConfig) {
        SqliteCompilation.setup(project, config)
    }

    fun buildOnServer() {
        BuildOnServer.register(project)
    }
}
