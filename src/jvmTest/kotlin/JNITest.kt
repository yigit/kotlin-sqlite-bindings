package com.birbit.jni

import org.junit.Test

class JNITest {
    @Test
    fun blah() {
        println("blah")
        val native = NativeHost.openDb(":memory:")
        println("OK")
    }
}