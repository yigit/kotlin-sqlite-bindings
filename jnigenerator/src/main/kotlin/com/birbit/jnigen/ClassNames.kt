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
package com.birbit.jnigen

import com.squareup.kotlinpoet.ClassName

private inline fun jniType(value: String) = ClassName("com.birbit.jni", value)
private inline fun internalType(value: String) = ClassName("com.birbit.sqlite3.internal", value)
private inline fun bindingApiType(value: String) = ClassName("com.birbit.sqlite3", value)

private inline fun interopType(value: String) = ClassName("kotlinx.cinterop", value)

object ClassNames {
    val JBYTEARRAY = jniType("jbyteArray")
    val RESULT_CODE = bindingApiType("ResultCode")
    val JINT = jniType("jint")
    val JLONG = jniType("jlong")
    val JDOUBLE = jniType("jdouble")
    val JBOOLEAN = jniType("jboolean")
    val JSTRING = jniType("jstring")
    val JOBJECT = jniType("jobject")
    val CNAME = ClassName("kotlin.native", "CName")
    val CPOINTER = interopType("CPointer")
    val CPOINTER_VGAR_OF = interopType("CPointerVarOf")
    val JNI_ENV_VAR = jniType("JNIEnvVar")
    val JCLASS = jniType("jclass")
    val SQLITE_API = internalType("SqliteApi")
    val DB_REF = internalType("DbRef")
    val STMT_REF = internalType("StmtRef")
    val AUTHORIZER = bindingApiType("Authorizer")
    val COLUMN_TYPE = bindingApiType("ColumnType")
}
