package com.birbit.jni

import org.scijava.nativelib.NativeLoader

private fun loadNativeLib() {
    NativeLoader.loadLibrary("myjni")
}

fun main() {

}

object NativeHost {
    init {

        loadNativeLib()

    }

    external fun openDb(path: String): ULong
    external fun callInt(input: Int): Int
    external fun getSqliteVersion(): Int
}