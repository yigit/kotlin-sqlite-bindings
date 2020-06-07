/*
 * Copyright 2020 Google, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "UNUSED_PARAMETER", "UnnecessaryVariable")

package com.birbit.sqlite3.internal

import com.birbit.sqlite3.ColumnType
import com.birbit.sqlite3.ResultCode
import kotlin.Suppress
import kotlin.native.CName
import kotlinx.cinterop.CPointer
import platform.android.JNIEnvVar
import platform.android.jboolean
import platform.android.jbyteArray
import platform.android.jclass
import platform.android.jdouble
import platform.android.jint
import platform.android.jlong
import platform.android.jobject
import platform.android.jstring

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeOpenConnection")
fun openConnection(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jstring
): jlong {
    initPlatform()
    return runWithJniExceptionConversion(env, 0L) {
        val localP0 = checkNotNull(p0.toKString(env))
        val callResult = SqliteApi.openConnection(localP0)
        val localCallResult = callResult.toJni()
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativePrepareStmt")
fun prepareStmt(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jstring
): jlong {
    initPlatform()
    return runWithJniExceptionConversion(env, 0L) {
        val localP0 = DbRef.fromJni(p0)
        val localP1 = checkNotNull(p1.toKString(env))
        val callResult = SqliteApi.prepareStmt(localP0, localP1)
        val localCallResult = callResult.toJni()
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeStep")
fun step(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.step(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnText")
fun columnText(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnText(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnInt")
fun columnInt(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jint {
    initPlatform()
    return runWithJniExceptionConversion(env, 0) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnInt(localP0, p1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnIsNull")
fun columnIsNull(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jboolean {
    initPlatform()
    return runWithJniExceptionConversion(env, JFALSE) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnIsNull(localP0, p1)
        val localCallResult = callResult.toJBoolean()
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeReset")
fun reset(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.reset(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeClose")
fun close(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = DbRef.fromJni(p0)
        val callResult = SqliteApi.close(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeFinalize")
fun finalize(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.finalize(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnBlob")
fun columnBlob(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jbyteArray? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnBlob(localP0, p1)
        val localCallResult = callResult?.toJByteArray(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnDouble")
fun columnDouble(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jdouble {
    initPlatform()
    return runWithJniExceptionConversion(env, 0.0) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnDouble(localP0, p1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnLong")
fun columnLong(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jlong {
    initPlatform()
    return runWithJniExceptionConversion(env, 0L) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnLong(localP0, p1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindBlob")
fun bindBlob(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint,
    p2: jbyteArray
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val localP2 = checkNotNull(p2.toKByteArray(env))
        val callResult = SqliteApi.bindBlob(localP0, p1, localP2)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindText")
fun bindText(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint,
    p2: jstring
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val localP2 = checkNotNull(p2.toKString(env))
        val callResult = SqliteApi.bindText(localP0, p1, localP2)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindInt")
fun bindInt(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint,
    p2: jint
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindInt(localP0, p1, p2)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindLong")
fun bindLong(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint,
    p2: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindLong(localP0, p1, p2)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindNull")
fun bindNull(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindNull(localP0, p1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeErrorMsg")
fun errorMsg(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = DbRef.fromJni(p0)
        val callResult = SqliteApi.errorMsg(localP0)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeErrorCode")
fun errorCode(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = DbRef.fromJni(p0)
        val callResult = SqliteApi.errorCode(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeErrorString")
fun errorString(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: ResultCode
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val callResult = SqliteApi.errorString(p0)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindDouble")
fun bindDouble(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint,
    p2: jdouble
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindDouble(localP0, p1, p2)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeSetAuthorizer")
fun setAuthorizer(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jobject?
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = DbRef.fromJni(p0)
        val localP1 = p1.toKAuthorizer(env)
        val callResult = SqliteApi.setAuthorizer(localP0, localP1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnType")
fun columnType(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): ColumnType {
    initPlatform()
    return runWithJniExceptionConversion(env, ColumnType(-1)) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnType(localP0, p1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeExec")
fun exec(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jstring
): ResultCode {
    initPlatform()
    return runWithJniExceptionConversion(env, ResultCode(-1)) {
        val localP0 = DbRef.fromJni(p0)
        val localP1 = checkNotNull(p1.toKString(env))
        val callResult = SqliteApi.exec(localP0, localP1)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnDeclType")
fun columnDeclType(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnDeclType(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnDatabaseName")
fun columnDatabaseName(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnDatabaseName(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnTableName")
fun columnTableName(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnTableName(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnOriginName")
fun columnOriginName(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnOriginName(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnCount")
fun columnCount(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jint {
    initPlatform()
    return runWithJniExceptionConversion(env, 0) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnCount(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeColumnName")
fun columnName(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.columnName(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeExpandedSql")
fun expandedSql(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jstring {
    initPlatform()
    return runWithJniExceptionConversion(env, "<no value>".toJString(env)!!) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.expandedSql(localP0)
        val localCallResult = checkNotNull(callResult.toJString(env))
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeNormalizedSql")
fun normalizedSql(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jstring {
    initPlatform()
    return runWithJniExceptionConversion(env, "<no value>".toJString(env)!!) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.normalizedSql(localP0)
        val localCallResult = checkNotNull(callResult.toJString(env))
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeSql")
fun sql(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jstring {
    initPlatform()
    return runWithJniExceptionConversion(env, "<no value>".toJString(env)!!) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.sql(localP0)
        val localCallResult = checkNotNull(callResult.toJString(env))
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindParameterCount")
fun bindParameterCount(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong
): jint {
    initPlatform()
    return runWithJniExceptionConversion(env, 0) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindParameterCount(localP0)
        callResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindParameterName")
fun bindParameterName(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jint
): jstring? {
    initPlatform()
    return runWithJniExceptionConversion(env, null) {
        val localP0 = StmtRef.fromJni(p0)
        val callResult = SqliteApi.bindParameterName(localP0, p1)
        val localCallResult = callResult?.toJString(env)
        localCallResult
    }
}

@CName("Java_com_birbit_sqlite3_internal_SqliteApi_nativeBindParameterIndex")
fun bindParameterIndex(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    p0: jlong,
    p1: jstring
): jint {
    initPlatform()
    return runWithJniExceptionConversion(env, 0) {
        val localP0 = StmtRef.fromJni(p0)
        val localP1 = checkNotNull(p1.toKString(env))
        val callResult = SqliteApi.bindParameterIndex(localP0, localP1)
        callResult
    }
}
