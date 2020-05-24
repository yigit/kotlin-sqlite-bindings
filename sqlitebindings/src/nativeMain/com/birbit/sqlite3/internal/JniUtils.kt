@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jboolean
import com.birbit.jni.jbyteArray
import com.birbit.jni.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.usePinned

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

internal inline fun ByteArray.toJByteArray(env: CPointer<JNIEnvVar>) : jbyteArray = memScoped {
    // TODO there is a double copy here from both sqlite to knative and then knative to java, we should probably
    //  avoid it in the future
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