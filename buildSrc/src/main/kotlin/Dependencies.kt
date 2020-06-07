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
package com.birbit.ksqlite.build

object Dependencies {
    val NATIVE_LIB_LOADER = "org.scijava:native-lib-loader:2.3.4"
    val KOTLIN_POET = "com.squareup:kotlinpoet:1.5.0"
    val ANDROID_TEST = listOf(
        "androidx.test.ext:junit:1.1.1",
        "androidx.test:runner:1.2.0")
}
