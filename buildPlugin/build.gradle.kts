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
    alias(libs.plugins.ktlint)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinReflect)
    implementation(libs.kotlinNativeUtils)
    implementation(libs.kotlinGradlePluginApi)
    implementation(libs.kotlinStdlibJdk)
    // workaround for KMP plugin to find android classes
    implementation(libs.agpApi)
    testImplementation(libs.truth)
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
    kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
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
