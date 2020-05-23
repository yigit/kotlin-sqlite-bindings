@file:Suppress("unused", "UNUSED_PARAMETER")

package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jclass
import com.birbit.jni.jint
import com.birbit.jni.jlong
import com.birbit.jni.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8

@Suppress("NOTHING_TO_INLINE")
internal inline fun log(msg: Any) {
    println("LOG:$msg")
}

private fun initPlatform() {
    initRuntimeIfNeeded()
    Platform.isMemoryLeakCheckerActive = false
}

// TODO this is probably better not be in this module so we don't ship it if not needed. Maybe astract all sqlite
//  stuff into another module
//  or else try to make them private or internal.
@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeOpenConnection")
fun openDb(env: CPointer<JNIEnvVar>, clazz: jclass, path: jstring): jlong {
    initPlatform()
    log("start open native db")
    val dbRef = SqliteApi.openConnection(checkNotNull(path.toKString(env)))
    return dbRef.toJni()
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativePrepareStmt")
fun prepareStmt(env: CPointer<JNIEnvVar>, clazz: jclass, dbPtr:jlong, stmt:jstring):jlong {
    log("prepare stmt")
    val dbRef = DbRef.fromJni(dbPtr)
    return SqliteApi.prepareStmt(dbRef, checkNotNull(stmt.toKString(env))).toJni()
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeStep")
fun step(env:CPointer<JNIEnvVar>, clazz: jclass, stmtPtr: jlong): ResultCode {
    return SqliteApi.step(StmtRef.fromJni(stmtPtr))
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnText")
fun step(env:CPointer<JNIEnvVar>, clazz: jclass, stmtPtr: jlong, index:jint): jstring? {
    val value = SqliteApi.columnText(StmtRef.fromJni(stmtPtr), index)
    return value?.toJString(env)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun CPointer<JNIEnvVar>.nativeInterface() = checkNotNull(this.pointed.pointed)

@Suppress("NOTHING_TO_INLINE")
private inline fun jstring.toKString(env: CPointer<JNIEnvVar>): String? {
    return env.nativeInterface().GetStringUTFChars!!.invoke(env, this, null)?.toKStringFromUtf8()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun String.toJString(env: CPointer<JNIEnvVar>): jstring = memScoped {
    env.nativeInterface().NewStringUTF!!.invoke(env, this@toJString.cstr.ptr)!!
}