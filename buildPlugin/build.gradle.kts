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
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.gradle.spotless")
    `java-gradle-plugin`
}
// has to be separate while using M2
apply(plugin = "kotlin-platform-jvm")
buildscript {
    val properties = java.util.Properties()
    rootDir.resolve("../gradle.properties").inputStream().use {
        properties.load(it)
    }
    properties.forEach {
        rootProject.extra.set(it.key as String, it.value)
    }
    val kotlinVersion: String by rootProject
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

val kotlinVersion: String by rootProject
val agpVersion: String by rootProject

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    implementation(kotlin("stdlib-jdk8"))
    // workaround for KMP plugin to find android classes
    implementation("com.android.tools.build:gradle-api:$agpVersion")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("ksqliteBuild") {
            id = "ksqlite-build"
            implementationClass = "com.birbit.ksqlite.build.KSqliteBuildPlugin"
        }
        create("ksqliteDependencies") {
            id = "ksqlite-dependencies"
            implementationClass = "com.birbit.ksqlite.build.Dependencies"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
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
