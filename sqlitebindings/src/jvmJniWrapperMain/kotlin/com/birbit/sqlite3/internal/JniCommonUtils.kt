/*
 * Copyright 2022 Google, LLC.
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
@file:OptIn(ExperimentalForeignApi::class)

package com.birbit.sqlite3.internal

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import platform.android.jlong

internal fun StmtRef.toJni() = nativeRef.stableRef.toJni()
internal fun DbRef.toJni() = nativeRef.stableRef.toJni()

internal fun dbRefFromJni(jlong: jlong): DbRef = jlong.castFromJni()
internal fun stmtRefFromJni(jlong: jlong): StmtRef = jlong.castFromJni()
internal inline fun <reified T : Any> jlong.castFromJni(): T {
    val ptr: COpaquePointer = this.toCPointer()!!
    return ptr.asStableRef<T>().get()
}

internal inline fun <reified T : Any> StableRef<T>.toJni() = this.asCPointer().toLong()
