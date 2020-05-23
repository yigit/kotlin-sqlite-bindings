package com.birbit.jni

import org.scijava.nativelib.NativeLoader

private fun loadNativeLib() {
    NativeLoader.loadLibrary("myjni")
}

fun main() {

}

object NativeHost {

    external fun prepareStmt(dbPtr: Long) : Long?
    external fun openDb(path: String): Long
    external fun callInt(input: Int): Int
    external fun getSqliteVersion(dbPtr: Long): String?
}