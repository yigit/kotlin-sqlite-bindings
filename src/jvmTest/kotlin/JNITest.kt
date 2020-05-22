package com.birbit.jni

import com.birbit.sqlite3.openConnection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JNITest {
    @Test
    fun blah() {
        println("blah")
        val native = openConnection(":memory:")
        assertEquals("3.31.1", native.version(), "couldn't get version")
    }
}