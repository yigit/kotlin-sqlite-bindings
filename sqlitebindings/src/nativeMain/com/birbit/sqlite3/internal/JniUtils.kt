/*
 * Copyright 2020 Google, Inc.
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

@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.JNI_ABORT
import com.birbit.jni.jboolean
import com.birbit.jni.jbyteArray
import com.birbit.jni.jclass
import com.birbit.jni.jint
import com.birbit.jni.jmethodID
import com.birbit.jni.jobject
import com.birbit.jni.jstring
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.getBytes
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readValues
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.usePinned

internal fun initPlatform() {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false
}

typealias SqliteExceptionConstructor = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jint, jstring?) -> jobject>

internal object JvmReferences {
    fun throwJvmSqliteException(
        env: CPointer<JNIEnvVar>,
        sqliteException: SqliteException
    ) {
        val nativeInterface = env.nativeInterface()
        val exception = memScoped {
            val exceptionClass = nativeInterface.FindClass!!(
                env, "com/birbit/sqlite3/internal/SqliteException".cstr.ptr)
                ?: error("cannot find SqliteException class from native")
            val constructor = nativeInterface.GetMethodID!!(
                env, exceptionClass, "<init>".cstr.ptr, "(ILjava/lang/String;)V".cstr.ptr
            ) ?: error("cannot find build exception method")
            nativeInterface.NewObject!!.reinterpret<SqliteExceptionConstructor>().invoke(
                env,
                exceptionClass,
                constructor,
                sqliteException.resultCode.value,
                sqliteException.msg.toJString(env)
            )
        }
        nativeInterface.Throw!!(env, exception)
    }
}

internal inline fun CPointer<JNIEnvVar>.nativeInterface() = checkNotNull(this.pointed.pointed)

internal inline fun jstring?.toKString(env: CPointer<JNIEnvVar>): String? {
    val chars = env.nativeInterface().GetStringUTFChars!!(env, this, null)
    try {
        return chars?.toKStringFromUtf8()
    } finally {
        env.nativeInterface().ReleaseStringUTFChars!!(env, this, chars)
    }
}

internal inline fun String?.toJString(env: CPointer<JNIEnvVar>): jstring? = this?.let {
    memScoped {
        env.nativeInterface().NewStringUTF!!.invoke(env, this@toJString.cstr.ptr)!!
    }
}

internal val JFALSE = 0.toUByte()
internal val JTRUE = 1.toUByte()

internal inline fun Boolean.toJBoolean(): jboolean = if (this) JTRUE else JFALSE

internal inline fun jboolean.toKBoolean(): Boolean = this != JFALSE

internal inline fun ByteArray?.toJByteArray(env: CPointer<JNIEnvVar>) : jbyteArray? = memScoped {
    // TODO there is a double copy here from both sqlite to knative and then knative to java, we should probably
    //  avoid it in the future
    if (this@toJByteArray == null) {
        return@memScoped null
    }
    val nativeInterface = env.nativeInterface()
    val newByteArray = nativeInterface.NewByteArray!!(env, this@toJByteArray.size)
    checkNotNull(newByteArray) {
        "jvm didn't provide valid byte array"
    }
    val self = this@toJByteArray
    self.usePinned {
        nativeInterface.SetByteArrayRegion!!(env, newByteArray, 0, this@toJByteArray.size, it.addressOf(0))
    }
    newByteArray
}

internal inline fun jbyteArray?.toKByteArray(env: CPointer<JNIEnvVar>) : ByteArray? {
    if (this == null) return null
    val bytes = env.nativeInterface().GetByteArrayElements!!(env, this, null)
    checkNotNull(bytes) {
        "unable to get bytes from JNI"
    }
    return try {
        // TODO probably needs to be optimized
        val length = env.nativeInterface().GetArrayLength!!(env, this)
        bytes.pointed.readValues(length).getBytes()
    } finally {
        env.nativeInterface().ReleaseByteArrayElements!!(env, this, bytes, JNI_ABORT)
    }
}

// TODO we should wrap more exceptions
internal inline fun <reified T> runWithJniExceptionConversion(
    env:CPointer<JNIEnvVar>,
    dummy : T,
    block: () -> T
) : T {
    return try {
        block()
    } catch (sqliteException : SqliteException) {
        JvmReferences.throwJvmSqliteException(env, sqliteException)
        dummy
    }
}