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
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.diffplug.gradle.spotless") version "4.0.1"
}

// has to be separate while using M2
apply(plugin = "kotlin-platform-jvm")
buildscript {
    val kotlinVersion = "1.3.72"
    project.extensions.extraProperties["KOTLIN_VERSION"] = kotlinVersion
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
val kotlinVersion = project.extensions.extraProperties["KOTLIN_VERSION"]
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://dl.bintray.com/kotlin/kotlin-eap") {
        metadataSources {
            this.artifact()
            gradleMetadata()
            this.mavenPom()
        }
    }
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    implementation(kotlin("stdlib-jdk8"))
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
