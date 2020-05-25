@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.birbit.sqlite3.internal

import com.birbit.jni.*
import kotlinx.cinterop.*
import kotlinx.cinterop.reinterpret
import kotlin.native.concurrent.AtomicReference

internal inline fun log(msg: Any) {
    println("LOG:$msg")
}

internal fun initPlatform() {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false
}

internal class CachedJniRef<T : Any>(
    private val doGet : (CPointer<JNIEnvVar>) -> T
) {
    private var value : AtomicReference<T?> = AtomicReference(null)
    fun get(env: CPointer<JNIEnvVar>) : T {
        return value.value ?: doGet(env).also {
            value.compareAndSet(null, it)
        }
    }
}

internal object JvmReferences {
    // TODO this should actually try to support all kotlin classes and have a fallback for non-kotlin
    private var jniHelperClass = CachedJniRef {
        memScoped {
            it.nativeInterface().FindClass!!(it, "com/birbit/sqlite3/internal/JniHelper".cstr.ptr)  ?: error("cannot find JniHelper class from native")
        }
    }
    private var createSqliteExceptionMethod = CachedJniRef {
        memScoped {
            it.nativeInterface().GetStaticMethodID!!(it, jniHelperClass.get(it), "createSqliteException".cstr.ptr,
                "(ILjava/lang/String;)Ljava/lang/Object;".cstr.ptr) ?: error("cannot find build exception method")
        }
    }
    private var buildExceptionMethod = CachedJniRef {
        it.nativeInterface()::CallStaticObjectMethod.get()?.reinterpret<BuildExceptionMethod>() ?: error("cannot cast build sqlite exception method")
    }
    fun throwJvmSqliteException(
        env: CPointer<JNIEnvVar>,
        sqliteException:SqliteException
    ) {
        val nativeInterface = env.nativeInterface()
        val exception = buildExceptionMethod.get(env).invoke(env,
            jniHelperClass.get(env),
            createSqliteExceptionMethod.get(env),
            sqliteException.resultCode.value,
            sqliteException.msg.toJString(env))
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

typealias BuildExceptionMethod = CFunction<(CPointer<JNIEnvVar>, jclass, jmethodID, jint, jstring?) -> jobject>
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