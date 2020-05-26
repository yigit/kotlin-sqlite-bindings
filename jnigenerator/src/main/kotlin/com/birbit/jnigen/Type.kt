/*
 * Copyright 2020 Google, Inc.
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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

typealias ToJniFun = (type: Type, envParam: ParameterSpec, inVar: String, outVar: String) -> (CodeBlock)
typealias FronJniFun = (type: Type, envParam: ParameterSpec, inParam: ParameterSpec, outVar: String) -> (CodeBlock)

open class Type(
    val kotlinDecl: String,
    val kotlinClass: TypeName,
    val nativeClass: TypeName,
    private val convertToJni: ToJniFun? = null,
    private val convertFromJni: FronJniFun? = null,
    val nullable: Boolean,
    private val defaultValue: String
) {
    constructor(
        kotlinClass: ClassName,
        nativeClass: TypeName,
        convertToJni: ToJniFun? = null,
        convertFromJni: FronJniFun? = null,
        defaultValue: String
    ) : this(
        kotlinDecl = kotlinClass.simpleName,
        kotlinClass = kotlinClass,
        nativeClass = nativeClass,
        convertToJni = convertToJni,
        convertFromJni = convertFromJni,
        nullable = false,
        defaultValue = defaultValue
    )

    fun defaultValue() = if (nullable) "null" else defaultValue

    fun hasConvertToJni() = convertToJni != null
    fun hasConvertFromJni() = convertFromJni != null

    fun convertToJni(envParam: ParameterSpec, inVar: String, outVar: String) =
        convertToJni!!(this, envParam, inVar, outVar)

    fun convertFromJni(envParam: ParameterSpec, inParam: ParameterSpec, outVar: String) =
        convertFromJni!!(this, envParam, inParam, outVar)

    fun copy(nullable: Boolean) = Type(
        kotlinDecl = kotlinDecl,
        kotlinClass = kotlinClass.copy(nullable = nullable),
        nativeClass = nativeClass.copy(nullable = nullable),
        convertFromJni = convertFromJni,
        convertToJni = convertToJni,
        nullable = nullable,
        defaultValue = defaultValue
    )

    class BridgeType(kotlinClass: ClassName) : Type(
        kotlinClass = kotlinClass,
        nativeClass = ClassNames.JLONG,
        convertFromJni = { type, envParam, inParam, outVar ->
            CodeBlock.builder().apply {
                addStatement("val %L = %T.fromJni(%N)", outVar, kotlinClass, inParam)
            }.build()
        },
        convertToJni = { type, envParam, inVar, outVar ->
            CodeBlock.builder().apply {
                addStatement("val %L = %L.toJni()", outVar, inVar)
            }.build()
        },
        defaultValue = "0"
    )

    class BytesBackedType(
        kotlinClass: ClassName,
        nativeClass: ClassName,
        toKMethod: String,
        toJMethod: String
    ) : Type(
        kotlinClass = kotlinClass,
        nativeClass = nativeClass,
        convertToJni = { type, envParam, inVar, outVar ->
            CodeBlock.builder().apply {
                if (type.nullable) {
                    addStatement("val %L = %L?.$toJMethod(%N)", outVar, inVar, envParam)
                } else {
                    addStatement("val %L = checkNotNull(%L.$toJMethod(%N))", outVar, inVar, envParam)
                }
            }.build()
        },
        convertFromJni = { type, envParam, inParam, outVar ->
            CodeBlock.builder().apply {
                if (type.nullable) {
                    addStatement("val %L = %N.$toKMethod(%N)", outVar, inParam, envParam)
                } else {
                    addStatement("val %L = checkNotNull(%N.$toKMethod(%N))", outVar, inParam, envParam)
                }
            }.build()
        },
        defaultValue = "null" // TODO this is probably NOT null. fix when we need it
    )

    companion object {
        private val types: List<Type> by lazy {
            val self = this
            this::class.java.methods.filter {
                Type::class.java.isAssignableFrom(it.returnType) && it.name.startsWith("get")
            }.map {
                it.invoke(self) as Type
            }
        }

        fun resolve(kotlinType: String, nullable: Boolean) = types.firstOrNull {
            it.kotlinDecl == kotlinType
        }?.let {
            it.copy(nullable = nullable)
        } ?: error("cannot resolve type $kotlinType")

        val INT =
            Type(Int::class.asClassName(), ClassNames.JINT, defaultValue = "0")
        val STRING = BytesBackedType(
            kotlinClass = String::class.asClassName(),
            nativeClass = ClassNames.JSTRING,
            toKMethod = "toKString",
            toJMethod = "toJString"
        )
        val DBREF = BridgeType(ClassNames.DB_REF)
        val STMTREF = BridgeType(ClassNames.STMT_REF)
        val LONG =
            Type(Long::class.asClassName(), ClassNames.JLONG, defaultValue = "0L")
        val DOUBLE = Type(
            Double::class.asClassName(),
            ClassNames.JDOUBLE,
            defaultValue = "0.0"
        )
        val BOOLEAN = Type(Boolean::class.asClassName(),
            ClassNames.JBOOLEAN,
            defaultValue = "JFALSE",
            convertFromJni = { type, envParam, inParam, outVar ->
                buildCodeBlock {
                    addStatement("val %L = %N.toKBoolean()", outVar, inParam)
                }
            },
            convertToJni = { type, envParam, inVar, outVar ->
                buildCodeBlock {
                    addStatement("val %L = %L.toJBoolean()", outVar, inVar)
                }
            })
        val RESULT_CODE = Type(
            ClassNames.RESULT_CODE,
            ClassNames.RESULT_CODE,
            defaultValue = "ResultCode(-1)"
        )
        val BYTE_ARRAY = BytesBackedType(
            kotlinClass = ByteArray::class.asClassName(),
            nativeClass = ClassNames.JBYTEARRAY,
            toKMethod = "toKByteArray",
            toJMethod = "toJByteArray"
        )
    }
}

fun String.resolveType(nullable: Boolean) = Type.resolve(this, nullable)
