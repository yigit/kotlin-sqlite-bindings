package com.birbit.jni

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jclass
import com.birbit.jni.jint
import kotlinx.cinterop.*
import platform.posix.size_t
import sqlite3.*
@CName("Java_com_birbit_jni_NativeHost_callInt")
fun callInt(env: CPointer<JNIEnvVar>, clazz: jclass, it: jint): jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: $it")
    val ptr = nativeHeap.allocPointerTo<sqlite3>()
    val pointer = platform.posix.malloc(CPointerVar.size.toULong())
    sqlite3_open(":memory:", cValue())
    return it + 1
}

@CName("Java_com_birbit_jni_NativeHost_getSqliteVersion")
fun getSqliteVersion(env: CPointer<JNIEnvVar>, clazz: jclass) : jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: ${sqlite3_libversion()}")

    return 0
}