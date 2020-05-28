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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

object Publishing {
    fun setupNativePublishing(project: Project) {
        val publishing = project.extensions.findByType<PublishingExtension>()
            ?: error("cannot find publishing extension")
        publishing.repositories {
            it.maven("file://${BuildOnServer.getOutRepo().absolutePath}")
        }
        val buildId = System.getenv("GITHUB_RUN_ID")?.padStart(6, '0')
        publishing.publications {
            it.all {
                if (it is DefaultMavenPublication) {
                    it.groupId = "com.birbit.ksqlite3"
                    if (buildId != null) {
                        it.version = "0.1.0.${buildId}"
                    } else {
                        it.version = "0.1.0-SNAPSHOT"
                    }
                } else {
                    error("unexpected publication $it")
                }
            }
        }

        val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            ?: error("cannot find KMP extension")
        kotlinExtension.targets.all { target ->
            target.mavenPublication(Action<MavenPublication> {targetPublication ->
                project.tasks.withType<AbstractPublishToMaven>()
                    .matching {
                        it.publication == targetPublication
                    }.all {
                        it.onlyIf {
                            target.platformType != KotlinPlatformType.jvm
                        }
                    }
            })
        }

    }
}