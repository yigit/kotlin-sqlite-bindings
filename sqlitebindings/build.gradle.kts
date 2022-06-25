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

import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.birbit.ksqlite.build.SqliteCompilationConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    id(libs.plugins.agpLibrary.get().pluginId)
    alias(libs.plugins.kotlinMp)
    id("maven-publish")
    id("ksqlite-build")
}
ksqliteBuild {
    native(
        includeAndroidNative = true,
        includeJni = true
    ) {
        binaries {
            sharedLib(namePrefix = "sqlite3jni")
        }
        compilations["main"].defaultSourceSet {
            dependencies {
                project(":sqlitebindings-api")
                implementation(libs.kotlinStdlibCommon)
            }
        }
    }
    android()
    includeSqlite(
        SqliteCompilationConfig(
            version = "3.38.5"
        )
    )
    publish()
    buildOnServer()
}

kotlin {

    val combineSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask
            .create(
                project = project,
                namePrefix = "sqlite3jni",
                outFolder = project.layout.buildDirectory.dir("combinedSharedLibs"),
                forAndroid = false
            )

    val combineAndroidSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask
            .create(
                project = project,
                namePrefix = "sqlite3jni",
                outFolder = project.layout.buildDirectory.dir("combinedAndroidSharedLibs"),
                forAndroid = true
            )
    project.android.sourceSets {
        this["main"].jniLibs {
            srcDir(
                combineAndroidSharedLibsTask.map {
                    it.outputDir
                }
            )
        }
    }
    // TODO we shouldn't need this but srcDir thing above doesn't seem to work
    val androidExt = project.extensions.findByType(com.android.build.gradle.LibraryExtension::class)
    androidExt!!.libraryVariants.all {
        this.javaCompileProvider.dependsOn(combineAndroidSharedLibsTask)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinStdlibCommon)
                api(project(":sqlitebindings-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTestCommon)
                implementation(libs.kotlinTestAnnotationsCommon)
            }
        }
        val commonJvmMain = create("commonJvmMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinStdlibJdk)
            }
        }
        val nativeMain by getting
        // commonization of jni does not work across jvm-android anymore, hence we duplicate
        // the code for them. Using symlinks is not possible due to ide not liking it + windows
        // issues
        val jniWrapperComonMain by creating {
            dependsOn(commonMain)
            dependsOn(nativeMain)
        }
        val androidJniWrapperMain by creating {
            dependsOn(jniWrapperComonMain)
        }
        val jvmJniWrapperMain by creating {
            dependsOn(jniWrapperComonMain)
        }
        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.kotlinStdlibJdk)
            }
        }
        val androidTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.kotlinTestJunit)
                implementation(libs.bundles.androidTest)
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.nativeLibLoader)
            }
            resources.srcDir(
                combineSharedLibsTask.map {
                    it.outputDir
                }
            )
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(libs.kotlinTestJunit)
            }
        }
        targets.forEach { target ->
            if (target.platformType == KotlinPlatformType.native) {
                val family = (target as KotlinNativeTarget).konanTarget.family
                when (family) {
                    Family.ANDROID -> target.compilations["main"].defaultSourceSet {
                        dependsOn(androidJniWrapperMain)
                    }
                    Family.OSX, Family.MINGW, Family.LINUX -> target.compilations["main"].defaultSourceSet {
                        dependsOn(jvmJniWrapperMain)
                    }
                    else -> {
                        // skip, doesn't need JNI
                    }
                }
            }
        }
    }
}
