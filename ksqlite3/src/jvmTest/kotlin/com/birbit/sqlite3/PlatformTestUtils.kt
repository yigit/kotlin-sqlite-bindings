package com.birbit.sqlite3

import java.io.File
import java.nio.file.Paths
import java.util.*

actual object PlatformTestUtils {
    actual fun getTmpDir(): String {
        val tmpDirPath = System.getProperty("java.io.tmpdir") ?: error("cannot find java tmp dir")
        val fullPath = Paths.get(tmpDirPath, "ksqlite", UUID.randomUUID().toString().substring(0, 20))
        val file = fullPath.toFile()
        file.mkdirs()
        return file.absolutePath
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun fileSeparator(): Char {
        return File.separatorChar
    }

    actual fun deleteDir(tmpDir: String) {
        val file = File(tmpDir)
        if (!file.exists()) return
        file.deleteRecursively()
    }
}
