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
        maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")

        mavenCentral()

        maven("https://plugins.gradle.org/m2/")
    }
}
include("sqlitebindings", "jnigenerator", "ksqlite3")

