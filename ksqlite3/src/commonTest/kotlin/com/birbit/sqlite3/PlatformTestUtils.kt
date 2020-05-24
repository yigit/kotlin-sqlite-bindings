package com.birbit.sqlite3

expect object PlatformTestUtils {
    fun getTmpDir(): String
    fun fileExists(path: String): Boolean
    fun fileSeparator(): Char
    fun deleteDir(tmpDir: String)
}

fun <T> withTmpFolder(block : TmpFolderScope.() -> T) {
    val tmpDir = PlatformTestUtils.getTmpDir()
    val scope = object : TmpFolderScope {
        override fun getFilePath(name: String) = tmpDir + PlatformTestUtils.fileSeparator() + name
    }
    try {
        scope.block()
    } finally {
        PlatformTestUtils.deleteDir(tmpDir)
    }
}

interface TmpFolderScope {
    fun getFilePath(name : String) : String
}
