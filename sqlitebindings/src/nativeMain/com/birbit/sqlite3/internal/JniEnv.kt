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

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.JNI_OK
import com.birbit.jni.JNI_VERSION_1_2
import com.birbit.jni.JavaVMVar
import com.birbit.jni.jclass
import com.birbit.jni.jint
import com.birbit.jni.jmethodID
import com.birbit.jni.jobject
import com.birbit.jni.jstring
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

typealias AuthParamsConstructor = CFunction<(
    CPointer<JNIEnvVar>,
    jclass,
    jmethodID,
    jint,
    jstring?,
    jstring?,
    jstring?,
    jstring?
) -> jobject>

val globalVM = AtomicReference<CPointer<JavaVMVar>?>(null)

private fun CPointer<JavaVMVar>.getEnv(): CPointer<JNIEnvVar> {
    return memScoped {
        val outEnv = alloc<COpaquePointerVar>()
        val res = this@getEnv.pointed.pointed?.GetEnv?.invoke(
            this@getEnv,
            outEnv.ptr,
            JNI_VERSION_1_2
        )
        // TODO handle attach
        check(res == JNI_OK) {
            "couldn't get env, probably need to attach"
        }
        val env: CPointer<JNIEnvVar> = outEnv.reinterpret<CPointerVar<JNIEnvVar>>().value?.reinterpret()
            ?: error("cannot get env")
        env
    }
}

private fun obtainEnv(): CPointer<JNIEnvVar> {
    val jvmVar: CPointer<JavaVMVar> = globalVM.value ?: error("cannot access global VM")
    return jvmVar.getEnv()
}

internal class GlobalRef(
    val jobject: jobject
) {
    fun dispose(env: CPointer<JNIEnvVar> = obtainEnv()) {
        env.nativeInterface().NewGlobalRef!!(env, jobject)
    }
}

internal class JvmAuthorizerParams(
    private val jclass: GlobalRef,
    private val initMethodId: jmethodID
) {
    fun createJvmInstance(env: CPointer<JNIEnvVar>, authorizationParams: AuthorizationParams): jobject {
        val init = env.nativeInterface().NewObject!!.reinterpret<AuthParamsConstructor>()
        return init.invoke(
            env, jclass.jobject, initMethodId,
            authorizationParams.actionCode,
            authorizationParams.param1?.toJString(env),
            authorizationParams.param2?.toJString(env),
            authorizationParams.param3?.toJString(env),
            authorizationParams.param4?.toJString(env)
        )
    }

    companion object {
        private val _instance = AtomicReference<JvmAuthorizerParams?>(null)
        fun setInstance(params: JvmAuthorizerParams) {
            _instance.value = params
        }

        val instance: JvmAuthorizerParams
            get() = checkNotNull(_instance.value) {
                "AuthorizerParams is not initialized"
            }
    }
}

@Suppress("FunctionName")
@CName("JNI_OnLoad")
fun jniOnload(vm: CPointer<JavaVMVar>, @Suppress("UNUSED_PARAMETER") reserved: CValuesRef<*>?): jint {
    globalVM.value = vm
    val env = vm.getEnv()
    val authorizerParams = memScoped {
        val jmClassRef = obtainGlobalReference(env, "com/birbit/sqlite3/internal/AuthorizationParams")
        JvmAuthorizerParams(
            jclass = jmClassRef,
            initMethodId = getMethodId(
                env, jmClassRef.jobject, "<init>",
                "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
            )
        )
    }
    authorizerParams.freeze()
    JvmAuthorizerParams.setInstance(authorizerParams)
    return JNI_VERSION_1_2
}

internal fun MemScope.obtainGlobalReference(env: CPointer<JNIEnvVar>, name: String): GlobalRef {
    val localClass = env.nativeInterface().FindClass!!(env, name.cstr.ptr) ?: error("cannot find class $name")
    val globalRef: jobject = env.nativeInterface().NewGlobalRef!!(env, localClass)
        ?: error("cannot obtain global reference to $name")
    env.nativeInterface().DeleteLocalRef!!(env, localClass)
    return GlobalRef(globalRef)
}

internal fun MemScope.findClass(env: CPointer<JNIEnvVar>, name: String): jclass {
    return env.nativeInterface().FindClass!!(env, name.cstr.ptr) ?: error("cannot find class $name")
}

internal fun MemScope.getMethodId(env: CPointer<JNIEnvVar>, klass: jclass, name: String, signature: String): jmethodID {
    return env.nativeInterface().GetMethodID!!(env, klass, name.cstr.ptr, signature.cstr.ptr)
        ?: error("cannot find method id $name / $signature")
}

class JvmClass()