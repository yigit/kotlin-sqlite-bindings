package com.birbit.sqlite3.internal

import com.birbit.jni.JNIEnvVar
import com.birbit.jni.jclass
import com.birbit.jni.jint
import com.birbit.jni.jlong
import com.birbit.jni.jstring
import com.birbit.sqlite3.StmtPtr
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf8

internal inline fun log(msg: Any) {
    println("LOG:$msg")
}

// TODO this is probably better not be in this module so we don't ship it if not needed. Maybe astract all sqlite
//  stuff into another module
//  or else try to make them private or internal.
@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeOpenConnection")
fun openDb(env: CPointer<JNIEnvVar>, clazz: jclass, path: jstring): jlong {
    initRuntimeIfNeeded()
    log("start open native db")
    val dbRef = SqliteApi.openConnection(checkNotNull(path.toKString(env)))
    return dbRef.toJni()
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativePrepareStmt")
fun prepareStmt(env: CPointer<JNIEnvVar>, clazz: jclass, dbPtr:jlong, stmt:jstring):jlong {
    initRuntimeIfNeeded()
    log("prepare stmt")
    val dbRef = DbRef.fromJni(dbPtr)
    val stmt = SqliteApi.prepareStmt(dbRef, checkNotNull(stmt.toKString(env)))
    return stmt.toJni()
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

private val CPointer<JNIEnvVar>.nativeInterface
    get() = checkNotNull(this.pointed.pointed)

private inline fun jstring.toKString(env: CPointer<JNIEnvVar>): String? {
    return env.nativeInterface.GetStringUTFChars!!.invoke(env, this, null)?.toKStringFromUtf8()
}

private inline fun String.toJString(env: CPointer<JNIEnvVar>): jstring = memScoped {
    env.nativeInterface.NewStringUTF!!.invoke(env, this@toJString.cstr.ptr)!!
}