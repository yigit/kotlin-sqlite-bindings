package com.birbit.jni

import org.junit.Test

class NativeTest {
    @Test
    fun blah() {
        println("blah")
        val native = NativeHost()
        println(native.callInt(3))
    }
}