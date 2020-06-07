import com.diffplug.gradle.spotless.SpotlessExtension

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

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1" apply false
    id("com.diffplug.gradle.spotless") version "4.0.1"
}

data class BuildVersions(
    private val data: Map<String, String>
) {
    val kotlin = data["kotlin"] ?: error("cannot find kotlin version")
    val agp = data["agp"] ?: error("cannot find agp version")
}

fun buildDepVersions(): BuildVersions {
    val data = extensions.extraProperties["BUILD_DEP_VERSIONS"] as? Map<String, String>
        ?: error("build versions are not defined")
    return BuildVersions(data)
}

// has to be separate while using M2
apply(plugin = "kotlin-platform-jvm")
buildscript {
    val kotlinVersion = "1.3.72"
    val agpVersion = "3.6.3"
    project.extensions.extraProperties["BUILD_DEP_VERSIONS"] = mapOf(
        "kotlin" to kotlinVersion,
        "agp" to agpVersion
    )

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlinx")
    google()
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${buildDepVersions().kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-native-utils:${buildDepVersions().kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${buildDepVersions().kotlin}")
    implementation(kotlin("stdlib-jdk8"))
    // workaround for KMP plugin to find android classes
    implementation("com.android.tools.build:gradle:${buildDepVersions().agp}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

extensions.getByType(SpotlessExtension::class).apply {
    kotlin {
        target("**/*.kt")
        ktlint().userData(
            mapOf(
                "max_line_length" to "120"
            )
        )
        licenseHeaderFile(project.rootProject.file("../scripts/copyright.txt"))
    }
    kotlinGradle {
        ktlint()
    }
}
