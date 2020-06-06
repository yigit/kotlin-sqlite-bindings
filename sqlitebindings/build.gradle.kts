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

import com.birbit.ksqlite.build.Dependencies
import com.birbit.ksqlite.build.Publishing
import com.birbit.ksqlite.build.SqliteCompilation
import com.birbit.ksqlite.build.SqliteCompilationConfig
import com.birbit.ksqlite.build.setupCommon

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
}
android {
    defaultConfig {
        compileSdkVersion = "android-29"
    }
}
kotlin {
    setupCommon(gradle) {
        binaries {
            sharedLib(namePrefix = "sqlite3jni")
        }
        compilations["main"].defaultSourceSet {
            dependencies {
                project(":sqlitebindings-api")
            }
        }
        compilations["main"].cinterops.create("jni") {
            // JDK is required here, JRE is not enough
            val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
            println("java home:$javaHome")
            var include = File(javaHome, "include")
            if (!include.exists()) {
                // look upper
                include = File(javaHome, "../include")
            }
            if (!include.exists()) {
                throw GradleException("cannot find include")
            }
            packageName = "com.birbit.jni"
            includeDirs(
                Callable { include },
                Callable { File(include, "darwin") },
                Callable { File(include, "linux") },
                Callable { File(include, "win32") }
            )
        }
    }

    val combinedSharedLibsFolder = project.buildDir.resolve("combinedSharedLibs")
    val combineSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask
            .create(project, "sqlite3jni", combinedSharedLibsFolder)
    jvm().compilations["main"].compileKotlinTask.dependsOn(combineSharedLibsTask)
    android {
        publishAllLibraryVariants()
        compilations.all {
            println("ANDROID $this")
            compileKotlinTask.dependsOn(combineSharedLibsTask)
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":sqlitebindings-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val commonJvmMain = create("commonJvmMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(Dependencies.NATIVE_LIB_LOADER)
            }
            resources.srcDir(combinedSharedLibsFolder)
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
SqliteCompilation.setup(
    project,
    SqliteCompilationConfig(
        version = "3.31.1"
    )
)
Publishing.setup(project)
