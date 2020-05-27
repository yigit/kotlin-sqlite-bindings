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
@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.JNINativeInterface_
import com.birbit.jni.JNI_ABORT
import com.birbit.jni.JNI_OK
import com.birbit.jni.JNI_VERSION_1_2
import com.birbit.jni.JavaVMVar
import com.birbit.jni.jboolean
import com.birbit.jni.jbyteArray
import com.birbit.jni.jclass
import com.birbit.jni.jint
import com.birbit.jni.jmethodID
import com.birbit.jni.jobject
import com.birbit.jni.jstring
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.getBytes
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValues
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value

// @Suppress("FunctionName")
// @CName("JNI_OnLoad")
// fun JNI_OnLoad(vm: CPointer<JavaVMVar>, reserved: kotlinx.cinterop.CValuesRef<*>?): jint {
//     println("setting global VM")
//     globalVM.value = vm
//     return JNI_VERSION_1_2
// }

internal fun initPlatform() {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false
}

//
class JvmAuthCallbackWrapper(
    private val globalRef: jobject,
    private val invokeMethodID: jmethodID
) : Authorizer {
    companion object {
        fun createFromInstance(env: CPointer<JNIEnvVar>, jobject: jobject): JvmAuthCallbackWrapper {
            val globalRef = JvmReferences.getGlobalRef(env, jobject)
            return memScoped {
                val authorizerClass = findClass(env, "com/birbit/sqlite3/internal/Authorizer")
                val invokeMethod =
                    getMethodId(env, authorizerClass, "invoke", "(Lcom/birbit/sqlite3/internal/AuthorizationParams;)I")

                JvmAuthCallbackWrapper(
                    globalRef = globalRef,
                    invokeMethodID = invokeMethod
                )
            }
        }
    }

    override fun invoke(params: AuthorizationParams): AuthResult {
        val jvmVar: CPointer<JavaVMVar> = globalVM.value ?: error("cannot access global VM")
        val authCode = memScoped {
            val outEnv = alloc<COpaquePointerVar>()
            val res = jvmVar.pointed.pointed!!.GetEnv!!(
                jvmVar,
                outEnv.ptr,
                JNI_VERSION_1_2
            )
            check(res == JNI_OK) {
                "couldn't get env, probably need to attach"
            }
            // TODO handle not attached thread etc
            val env: CPointer<JNIEnvVar> = outEnv.reinterpret<CPointerVar<JNIEnvVar>>().value?.reinterpret()
                ?: error("cannot get env")

            val nativeInterface: JNINativeInterface_ = env.nativeInterface()

            val callIntMethod = nativeInterface.CallIntMethod!!.reinterpret<AuthCallbackMethod>()
                ?: error("cannot get call int method")
            // TODO cache these
            val paramsJObject = JvmAuthorizerParams.instance.createJvmInstance(env, params)
            // TODO make sure we are not leaking the new params
            callIntMethod.invoke(env, globalRef, invokeMethodID, paramsJObject)
        }
        return AuthResult(authCode)
    }
}

typealias SqliteExceptionConstructor = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jint, jstring?) -> jobject>
typealias NewGlobalRef = CFunction<(CPointer<JNIEnvVar>, jobject) -> jobject>
typealias AuthCallbackMethod = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jobject) -> jint>

internal object JvmReferences {
    fun throwJvmSqliteException(
        env: CPointer<JNIEnvVar>,
        sqliteException: SqliteException
    ) {
        val nativeInterface = env.nativeInterface()
        val exception = memScoped {
            val exceptionClass = findClass(
                env, "com/birbit/sqlite3/internal/SqliteException"
            )

            val constructor = getMethodId(env, exceptionClass, "<init>", "(ILjava/lang/String;)V")
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

    fun getGlobalRef(env: CPointer<JNIEnvVar>, obj: jobject): jobject {
        return env.nativeInterface().NewGlobalRef!!.reinterpret<NewGlobalRef>().invoke(
            env,
            obj
        )
    }
}

internal inline fun CPointer<JNIEnvVar>.nativeInterface() = checkNotNull(this.pointed.pointed) {
    "there is no JNINativeInterface_ in $this environment"
}

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

internal inline fun ByteArray?.toJByteArray(env: CPointer<JNIEnvVar>): jbyteArray? = memScoped {
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

internal inline fun jbyteArray?.toKByteArray(env: CPointer<JNIEnvVar>): ByteArray? {
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
    env: CPointer<JNIEnvVar>,
    dummy: T,
    block: () -> T
): T {
    return try {
        block()
    } catch (sqliteException: SqliteException) {
        JvmReferences.throwJvmSqliteException(env, sqliteException)
        dummy
    }
}
