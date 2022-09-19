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

import org.jetbrains.kotlin.spec.grammar.tools.KotlinParseTree
object Declarations {
    const val FUN_DECLARATION = "functionDeclaration"
    const val OBJ_DECLARATION = "objectDeclaration"
    const val SIMPLE_IDENTIFIER = "simpleIdentifier"
    const val IDENTIFIER = "Identifier"
    const val MODIFIERS = "modifiers"
    const val MODIFIER = "modifier"
    const val FUN = "FUN"
    const val FUN_VALUE_PARAMETERS = "functionValueParameters"
    const val FUN_VALUE_PARAMETER = "functionValueParameter"
    const val TYPE_REFERENCE = "typeReference"
    const val TYPE = "type"
    const val NULLABLE_TYPE = "nullableType"
}

object Modifiers {
    const val EXTERNAL = "external"
    const val ACTUAL = "actual"
}

val nameFiled = KotlinParseTree::class.java.declaredFields.first {
    it.name == "name"
}.also {
    it.isAccessible = true
}

val textField = KotlinParseTree::class.java.declaredFields.first {
    it.name == "text"
}.also {
    it.isAccessible = true
}

fun KotlinParseTree.name() = nameFiled.get(this) as? String

fun KotlinParseTree.text() = textField.get(this) as? String

fun KotlinParseTree.asSequence(): Sequence<KotlinParseTree> = sequence<KotlinParseTree> {
    yield(this@asSequence)
    children.forEach {
        it.asSequence().forEach {
            yield(it)
        }
    }
}

fun KotlinParseTree.findFirst(filter: (KotlinParseTree) -> Boolean): KotlinParseTree? {
    return this.asSequence().firstOrNull(filter)
}

fun KotlinParseTree.objectDeclarations() = asSequence().filter {
    it.name() == Declarations.OBJ_DECLARATION
}.map {
    ObjectDeclaration(it)
}

fun KotlinParseTree.skipFindPath(target: String, sections: List<String>): List<KotlinParseTree> {
    val index = children.indexOfFirst {
        it.name() == target
    }
    if (index < 0) {
        error("cannot find $target")
    }
    return children.subList(index + 1, children.size).flatMap {
        it.findPath(sections)
    }
}
fun KotlinParseTree.findPath(sections: List<String>): List<KotlinParseTree> {
    if (sections.isEmpty()) {
        return listOf(this)
    }
    val section = sections.first()
    val subSections = if (name() == section) {
        sections.subList(1, sections.size)
    } else {
        sections
    }
    if (subSections.isEmpty()) {
        return listOf(this)
    }
    val nextSection = subSections.first()
    return children.sortedBy {
        if (it.name() == nextSection) {
            0
        } else {
            1
        }
    }.flatMap {
        it.findPath(subSections)
    }
}

class ObjectDeclaration(
    val parseTree: KotlinParseTree
) {
    val name by lazy {
        parseTree.findPath(
            listOf(
                Declarations.SIMPLE_IDENTIFIER,
                Declarations.IDENTIFIER
            )
        ).first().text()
    }

    val functions by lazy {
        parseTree.findPath(listOf(Declarations.FUN_DECLARATION))
            .map {
                FunctionDeclaration(it)
            }
    }
}

class FunctionDeclaration(
    val parseTree: KotlinParseTree
) {
    val name by lazy {
        checkNotNull(
            parseTree.skipFindPath(
                Declarations.FUN,
                listOf(
                    Declarations.SIMPLE_IDENTIFIER,
                    Declarations.IDENTIFIER
                )
            ).firstOrNull()?.text()
        ) {
            "cannot find name for $parseTree"
        }
    }
    val modifiers by lazy {
        parseTree.findPath(
            listOf(
                Declarations.MODIFIERS,
                Declarations.MODIFIER
            )
        )
            .map {
                it.children[0].children[0].text()
            }
    }
    val paramTypes by lazy {
        val parameters = parseTree.findPath(
            listOf(
                Declarations.FUN_VALUE_PARAMETERS,
                Declarations.FUN_VALUE_PARAMETER
            )
        )
        parameters.map {
            val name = it.findPath(
                listOf(
                    Declarations.TYPE_REFERENCE,
                    Declarations.SIMPLE_IDENTIFIER,
                    Declarations.IDENTIFIER
                )
            ).first().text()
            val nullable = it.findPath(
                listOf(
                    Declarations.TYPE,
                    Declarations.NULLABLE_TYPE
                )
            ).isNotEmpty()
            checkNotNull(name) {
                "name cannot be null"
            }
            name.resolveType(nullable)
        }
    }
    val returnType by lazy {
        val typeTree = parseTree.children.firstOrNull {
            it.name() == Declarations.TYPE
        }
        checkNotNull(typeTree) {
            "cannot find type tree ${parseTree.children.map { it.name() }}"
        }
        val nullable = typeTree.findPath(
            listOf(
                Declarations.TYPE,
                Declarations.NULLABLE_TYPE
            )
        ).isNotEmpty()
        checkNotNull(
            typeTree.findPath(
                listOf(
                    Declarations.TYPE_REFERENCE,
                    Declarations.SIMPLE_IDENTIFIER,
                    Declarations.IDENTIFIER
                )
            ).first().text()
        ) {
            "cannot find return type for $parseTree"
        }.resolveType(nullable)
    }
    val external: Boolean
        get() = modifiers.contains(Modifiers.EXTERNAL)

    override fun toString(): String {
        return "$name(${paramTypes.joinToString()}):$returnType"
    }
}
