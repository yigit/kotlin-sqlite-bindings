package com.birbit.jni

import cnames.structs.sqlite3
import com.birbit.sqlite3.DbPtr
import com.birbit.sqlite3.SqliteConnection
import com.birbit.sqlite3.StmtPtr
import com.birbit.sqlite3.openConnection
import kotlinx.cinterop.*

@CName("Java_com_birbit_jni_NativeHost_callInt")
fun callInt(env: CPointer<JNIEnvVar>, clazz: jclass, it: jint): jint {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false

    println("Native function is executed with: $it")
    return it + 1
}

@CName("Java_com_birbit_jni_NativeHost_getSqliteVersion")
fun getSqliteVersion(env: CPointer<JNIEnvVar>, clazz: jclass, connPtr: jlong): jstring? {
    Platform.isMemoryLeakCheckerActive = false
    initRuntimeIfNeeded()
    println("running getSqliteVersion")
    return env.inScope {
        Platform.isMemoryLeakCheckerActive = false
        val conn = SqliteConnection.fromJni(connPtr)
        val readVersion = checkNotNull(conn.versionViaQuery()) {
            "empty version"
        }
        println("read version in native function and it gave: ${readVersion}")
        readVersion.toJString()
    }

}

@CName("Java_com_birbit_jni_NativeHost_openDb")
fun openDb(env: CPointer<JNIEnvVar>, clazz: jclass, path: jstring): jlong {
    initRuntimeIfNeeded()
    println("start open native db")
    val db = env.inScope {
        openConnection(checkNotNull(path.toKString()) {
            "invalid path: $path"
        })
    }
//    Platform.isMemoryLeakCheckerActive = false
    println("created db ptr: ${db.ptr}")
    println("Native openDb is executed")

//    return db.ptr.rawPtr.asStableRef<DbPtr>().asCPointer().rawValue.toLong()
    // TODO should make this stable reference somehow
    return db.toJni()
}

@CName("Java_com_birbit_jni_NativeHost_prepareStmt")
fun prepareStmt(env: CPointer<JNIEnvVar>, clazz: jclass, dbPtr: jlong, stmt: jstring): jlong {
    initRuntimeIfNeeded()
    val ptr = DbPtr(dbPtr.toCPointer<sqlite3>()!!)
    val stmt = env.inScope {
        val stmt = checkNotNull(stmt.toKString()) {
            "statement cannot be null"
        }
        val conn = SqliteConnection(ptr)
        conn.prepareStmt(stmt)
    }
    return stmt.stmtPtr.rawPtr.asStableRef<StmtPtr>().asCPointer().rawValue.toLong()
}

private fun <T> CPointer<JNIEnvVar>.inScope(
    block: EnvScope.() -> T
): T {
    return EnvScope(this).block()
}

class EnvScope(
    private val envPtr: CPointer<JNIEnvVar>
) {
    val jni: JNINativeInterface_ = checkNotNull(envPtr.pointed.value).pointed
    fun jstring.toKString() = jni.GetStringUTFChars!!.invoke(envPtr, this, null)?.toKStringFromUtf8()
    fun String.toJString(): jstring = memScoped {
        jni.NewStringUTF!!.invoke(envPtr, this@toJString.cstr.ptr)!!
    }
}