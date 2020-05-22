package com.birbit.jni

import java.io.File

private fun loadNativeLib() {
    val nativeLib = NativeHost::class.java.getResourceAsStream("/libmyjni.so")
        ?: throw IllegalStateException("cannot find native library")
    val tmpDir = System.getProperty("java.io.tmpdir") ?: throw IllegalStateException("cannot find tmp dir")
    val nativeFile = File(tmpDir, "native.so")
    nativeFile.writeBytes(
        nativeLib.readBytes()
    )
    Runtime.getRuntime().load(nativeFile.canonicalFile.absolutePath)
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