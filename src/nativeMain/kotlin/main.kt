package com.birbit.jni

import kotlinx.cinterop.CPointer
import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jclass
import com.birbit.jni.jint
@CName("Java_com_birbit_jni_NativeHost_callInt")
fun callInt(env: CPointer<JNIEnvVar>, clazz: jclass, it: jint): jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: $it")
    return it + 1
}