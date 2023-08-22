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
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("androidx.build.gradle.gcpbuildcache") version("1.0.0-beta01")
    id("com.gradle.enterprise") version("3.10")
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        capture {
            isTaskInputFiles = true
        }
    }
}

val gcpKey = providers.environmentVariable("GRADLE_CACHE_KEY").orNull
    ?: providers.environmentVariable("GRADLE_CACHE_FILE").orNull?.let {
        File(it).readText()
    }
val cacheIsPush = providers.environmentVariable("GRADLE_CACHE_PUSH").orNull?.toBoolean() ?: false
if (gcpKey != null) {
    println("setting up remote build cache with push: $cacheIsPush")
    buildCache {
        remote(androidx.build.gradle.gcpbuildcache.GcpBuildCache::class) {
            projectId = "kotlin-sqlite-bindings"
            bucketName = "kotlin-sqlite-bindings-cache"
            credentials = androidx.build.gradle.gcpbuildcache.ExportedKeyGcpCredentials {
                gcpKey
            }
            isPush = cacheIsPush
        }
    }
} else {
    println("not using remote build cache")
}
includeBuild("buildPlugin")
include("sqlitebindings", "sqlitebindings-api", "jnigenerator", "ksqlite3")
