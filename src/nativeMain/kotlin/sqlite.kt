package com.birbit.jni

import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import sqlite3.*

class KSqliteDb {
    companion object {
        fun create(): Int {
            val ptr = nativeHeap.allocPointerTo<sqlite3>()
            val openResult = sqlite3_open(":memory:", ptr.ptr)
            return openResult
        }
    }
}