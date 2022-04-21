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

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("multiplatform") apply false
}

buildscript {
    dependencies {
        // workaround for KMP plugin to find android classes
        classpath("com.android.tools.build:gradle:7.1.3")
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
    if (this.path != ":konan-warmup") {
        apply(plugin = "com.diffplug.spotless")
        this.extensions.getByType(SpotlessExtension::class).apply {
            kotlin {
                target("src/**/*.kt")
                // ktlint fails to parse this
                targetExclude("src/**/JniEnv.kt")
                ktlint().userData(
                    mapOf(
                        "max_line_length" to "120"
                    )
                )
                licenseHeaderFile(project.rootProject.file("scripts/copyright.txt"))
            }
            kotlinGradle {
                ktlint()
            }
        }
    }
}
