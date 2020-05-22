package com.birbit.jni

import kotlinx.cinterop.*
import sqlite3.*
@CName("Java_com_birbit_jni_NativeHost_callInt")
fun callInt(env: CPointer<JNIEnvVar>, clazz: jclass, it: jint): jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: $it")
    return it + 1
}

@CName("Java_com_birbit_jni_NativeHost_getSqliteVersion")
fun getSqliteVersion(env: CPointer<JNIEnvVar>, clazz: jclass) : jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: ${sqlite3_libversion()}")

    return 0
}

@CName("Java_com_birbit_jni_NativeHost_openDb")
fun openDb(env: CPointer<JNIEnvVar>, clazz: jclass, path : jstring) : jlong {
    initRuntimeIfNeeded()
    val tmp : JNIEnvVar = env.pointed
    val value : JNIEnv = checkNotNull(tmp.value)
    val chars = value.pointed.GetStringUTFChars?.invoke(env, path, null)
    val db = com.birbit.sqlite3.openDb(path = chars!!.toKStringFromUtf8())
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: ${sqlite3_libversion()}")

    return db.ptr.rawValue.toLong()
}