package com.birbit.jni

import com.birbit.sqlite3.Sqlite3Db
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
    external fun prepareStmt(dbPtr: Long) : Long?
    external fun openDb(path: String): Long
    external fun callInt(input: Int): Int
    external fun getSqliteVersion(dbPtr: Long): String?
}