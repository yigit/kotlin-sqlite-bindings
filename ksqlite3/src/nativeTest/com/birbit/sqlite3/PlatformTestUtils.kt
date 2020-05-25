package com.birbit.sqlite3

import kotlinx.cinterop.memScoped
import platform.posix.F_OK
import platform.posix.access
import platform.posix.mkdir
import platform.posix.system
import kotlin.random.Random

actual object PlatformTestUtils {
    private fun randomFolderName(): String {
        return (0..20).map {
            'a' + Random.nextInt(0, 26)
        }.joinToString("")
    }

    actual fun getTmpDir(): String {
        val tmpDir = memScoped {
            val tmpName = "ksqlite_tmp${randomFolderName()}"
            // for some reason, mkdtemp does not work on command line tests :/
            // second param to mkdir is UShort on mac and UInt on linux :/
            mkdir(tmpName, 0b0111111111.toUShort())
            tmpName
        }
        return checkNotNull(tmpDir) {
            "mkdtemp failed environment variable"
        }
    }

    actual fun fileExists(path: String): Boolean {
        return access(path, F_OK) != -1
    }

    actual fun fileSeparator(): Char {
        return when (Platform.osFamily) {
            OsFamily.WINDOWS -> '\\'
            else -> '/'
        }
    }

    actual fun deleteDir(tmpDir: String) {
        when (Platform.osFamily) {
            OsFamily.WINDOWS -> {
                system("rmdir $tmpDir /s /q")
            }
            else -> system("rm -rf $tmpDir")
        }
    }
}