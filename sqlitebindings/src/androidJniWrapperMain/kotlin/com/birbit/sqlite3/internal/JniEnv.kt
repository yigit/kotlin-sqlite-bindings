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
package com.birbit.sqlite3.internal

import com.birbit.sqlite3.AuthResult
import com.birbit.sqlite3.AuthorizationParams
import com.birbit.sqlite3.Authorizer
import com.birbit.sqlite3.SqliteException
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
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
import platform.android.JNIEnvVar
import platform.android.JNI_OK
import platform.android.JNI_VERSION_1_2
import platform.android.JavaVMVar
import platform.android.jclass
import platform.android.jint
import platform.android.jmethodID
import platform.android.jobject
import platform.android.jstring

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

typealias AuthCallbackMethod = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jobject) -> jint>
typealias DisposeMethod = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID) -> Unit>
typealias SqliteExceptionConstructor = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jint, jstring?) -> jobject>
typealias NewGlobalRefMethod = CFunction<(CPointer<JNIEnvVar>, jobject) -> jobject>

val globalVM = AtomicReference<CPointer<JavaVMVar>?>(null)

internal inline fun CPointer<JNIEnvVar>.nativeInterface() = checkNotNull(this.pointed.pointed) {
    "there is no JNINativeInterface_ in $this environment"
}

internal interface JniCache : JniDisposable {
    fun init(scope: MemScope, env: CPointer<JNIEnvVar>)
}

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

internal interface JniDisposable {
    fun dispose(env: CPointer<JNIEnvVar>)
}

internal class GlobalRef(
    val jobject: jobject
) : JniDisposable {
    override fun dispose(env: CPointer<JNIEnvVar>) {
        env.nativeInterface().DeleteGlobalRef!!(env, jobject)
    }
}

internal class DisposableAtomicRef<T : JniDisposable> : JniDisposable {
    private val instance: AtomicReference<T?> = AtomicReference(null)
    fun value() = checkNotNull(instance.value) {
        "disposable atomic value is not initialized or disposed"
    }

    fun set(env: CPointer<JNIEnvVar>, value: T?) {
        dispose(env)
        instance.value = value
    }

    override fun dispose(env: CPointer<JNIEnvVar>) {
        var current = instance.value
        while (current != null) {
            if (instance.compareAndSet(current, null)) {
                current.dispose(env)
                // this does not actually mean much since we are not locking.
                current = instance.value
            }
        }
    }
}

internal class JvmAuthorizerCallback private constructor(
    private val classRef: GlobalRef,
    private val invokeMethodId: jmethodID,
    private val disposeMethodId: jmethodID
) : JniDisposable {
    override fun dispose(env: CPointer<JNIEnvVar>) {
        classRef.dispose(env)
    }

    companion object : JniCache {
        private val _instance = DisposableAtomicRef<JvmAuthorizerCallback>()

        override fun init(scope: MemScope, env: CPointer<JNIEnvVar>) {
            scope.run {
                val classRef = obtainGlobalReference(env, "com/birbit/sqlite3/Authorizer")
                val jvmAuthCallback = JvmAuthorizerCallback(
                    classRef = classRef,
                    invokeMethodId = getMethodId(
                        env,
                        classRef.jobject,
                        "invoke-LECIP-g",
                        "(Lcom/birbit/sqlite3/AuthorizationParams;)I"
                    ),
                    disposeMethodId = getMethodId(
                        env,
                        classRef.jobject,
                        "dispose",
                        "()V"
                    )
                )
                jvmAuthCallback.freeze()
                _instance.set(env, jvmAuthCallback)
            }
        }

        override fun dispose(env: CPointer<JNIEnvVar>) {
            _instance.dispose(env)
        }

        fun createFromJvmInstance(env: CPointer<JNIEnvVar>, jobject: jobject): Authorizer {
            val globalRef = JvmReferences.getGlobalRef(env, jobject)
            return JvmAuthCallbackWrapper(
                globalRef = globalRef
            )
        }

        fun invokeCallback(
            env: CPointer<JNIEnvVar> = obtainEnv(),
            target: jobject,
            params: AuthorizationParams
        ): AuthResult {
            val callIntMethod = env.nativeInterface().CallIntMethod?.reinterpret<AuthCallbackMethod>()
                ?: error("cannot get call int method")
            val jvmParams = JvmAuthorizerParams.createJvmInstance(env, params)
            val authCode = callIntMethod.invoke(env, target, _instance.value().invokeMethodId, jvmParams)
            return AuthResult(authCode)
        }

        fun invokeDispose(
            env: CPointer<JNIEnvVar> = obtainEnv(),
            target: jobject
        ) {
            val disposeMethod = env.nativeInterface().CallVoidMethod!!.reinterpret<DisposeMethod>()
            disposeMethod.invoke(env, target, _instance.value().disposeMethodId)
        }

        private class JvmAuthCallbackWrapper(
            private val globalRef: jobject
        ) : Authorizer {

            override fun invoke(params: AuthorizationParams): AuthResult {
                return invokeCallback(target = globalRef, params = params)
            }

            override fun dispose() {
                // first dispose jvm instance
                invokeDispose(obtainEnv(), globalRef)
                JvmReferences.dispose(obtainEnv(), globalRef)
            }
        }
    }
}

internal class JvmAuthorizerParams private constructor(
    private val classRef: GlobalRef,
    private val initMethodId: jmethodID
) : JniDisposable {
    override fun dispose(env: CPointer<JNIEnvVar>) {
        classRef.dispose(env)
    }

    companion object : JniCache {
        private val _instance = DisposableAtomicRef<JvmAuthorizerParams>()

        override fun init(scope: MemScope, env: CPointer<JNIEnvVar>) {
            scope.run {
                val classRef = obtainGlobalReference(env, "com/birbit/sqlite3/AuthorizationParams")
                val jvmAuthorizerParams = JvmAuthorizerParams(
                    classRef = classRef,
                    initMethodId = getMethodId(
                        env, classRef.jobject, "<init>",
                        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
                    )
                )
                jvmAuthorizerParams.freeze()
                _instance.set(env, jvmAuthorizerParams)
            }
        }

        override fun dispose(env: CPointer<JNIEnvVar>) {
            _instance.dispose(env)
        }

        fun createJvmInstance(env: CPointer<JNIEnvVar>, authorizationParams: AuthorizationParams): jobject {
            val init = env.nativeInterface().NewObject!!.reinterpret<AuthParamsConstructor>()
            val instance = _instance.value()
            return init.invoke(
                env, instance.classRef.jobject, instance.initMethodId,
                authorizationParams.actionCode,
                authorizationParams.param1?.toJString(env),
                authorizationParams.param2?.toJString(env),
                authorizationParams.param3?.toJString(env),
                authorizationParams.param4?.toJString(env)
            )
        }
    }
}

internal class JvmSqliteException private constructor(
    private val classRef: GlobalRef,
    private val initMethodId: jmethodID
) : JniDisposable {
    companion object : JniCache {
        val _instance = DisposableAtomicRef<JvmSqliteException>()
        override fun init(scope: MemScope, env: CPointer<JNIEnvVar>) {
            val jvmSqliteException = scope.run {
                val classRef = obtainGlobalReference(env, "com/birbit/sqlite3/SqliteException")
                JvmSqliteException(
                    classRef = classRef,
                    initMethodId = getMethodId(env, classRef.jobject, "<init>", "(ILjava/lang/String;)V")
                )
            }
            jvmSqliteException.freeze()
            _instance.set(env, jvmSqliteException)
        }

        override fun dispose(env: CPointer<JNIEnvVar>) {
            _instance.dispose(env)
        }

        fun doThrow(env: CPointer<JNIEnvVar>, exception: SqliteException) {
            val instance = _instance.value()
            val jvmObject = env.nativeInterface().NewObject!!.reinterpret<SqliteExceptionConstructor>().invoke(
                env,
                instance.classRef.jobject,
                instance.initMethodId,
                exception.resultCode.value,
                exception.msg.toJString(env)
            )
            env.nativeInterface().Throw!!(env, jvmObject)
        }
    }

    override fun dispose(env: CPointer<JNIEnvVar>) {
        classRef.dispose(env)
    }
}

private val jniCache = listOf<JniCache>(
    JvmAuthorizerParams.Companion,
    JvmAuthorizerCallback.Companion,
    JvmSqliteException.Companion
)

@Suppress("unused")
@CName("JNI_OnLoad")
fun jniOnload(vm: CPointer<JavaVMVar>, @Suppress("UNUSED_PARAMETER") reserved: CValuesRef<*>?): jint {
    globalVM.value = vm
    val env = vm.getEnv()
    memScoped {
        jniCache.forEach {
            it.init(this, env)
        }
    }
    return JNI_VERSION_1_2
}

@Suppress("unused")
@CName("JNI_OnUnload")
fun jniOnUnload(vm: CPointer<JavaVMVar>, @Suppress("UNUSED_PARAMETER") reserved: CValuesRef<*>?): jint {
    globalVM.value = vm
    val env = vm.getEnv()
    jniCache.forEach {
        it.dispose(env)
    }
    return JNI_VERSION_1_2
}

internal fun MemScope.obtainGlobalReference(env: CPointer<JNIEnvVar>, name: String): GlobalRef {
    val localClass = findClass(env, name)
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

internal object JvmReferences {
    fun getGlobalRef(env: CPointer<JNIEnvVar>, obj: jobject): jobject {
        return env.nativeInterface().NewGlobalRef!!.reinterpret<NewGlobalRefMethod>().invoke(
            env,
            obj
        )
    }

    fun dispose(env: CPointer<JNIEnvVar>, globalRef: jobject) {
        env.nativeInterface().DeleteGlobalRef!!(env, globalRef)
    }
}
