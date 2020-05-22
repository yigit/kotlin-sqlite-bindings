package com.birbit.jni

import org.scijava.nativelib.NativeLoader

private fun loadNativeLib() {
    NativeLoader.loadLibrary("myjni")
}

fun main() {

}

class NativeHost {
    companion object {
        init {
            loadNativeLib()
        }
    }

    external fun callInt(input: Int): Int
    external fun getSqliteVersion() : Int
}