@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jboolean
import com.birbit.jni.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8

internal inline fun log(msg: Any) {
    println("LOG:$msg")
}

internal fun initPlatform() {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false
}

internal inline fun CPointer<JNIEnvVar>.nativeInterface() = checkNotNull(this.pointed.pointed)

internal inline fun jstring?.toKString(env: CPointer<JNIEnvVar>): String? {
    return env.nativeInterface().GetStringUTFChars!!.invoke(env, this, null)?.toKStringFromUtf8()
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