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
pluginManagement {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        google()
    }
    val spotlessVersion: String by settings
    val ktlintVersion: String by settings
    val kotlinVersion: String by settings
    plugins {
        id("com.diffplug.spotless") version spotlessVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
    }
}
includeBuild("buildPlugin")
include("sqlitebindings", "sqlitebindings-api", "jnigenerator", "ksqlite3")
