/*
 * Copyright 2021 Google, LLC.
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
package com.birbit.ksqlite.build.internal

import java.util.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object KonanPropLoader {
    private val konanProps = Properties()
    val regex = "\\\$([a-zA-Z_.0-9])+".toRegex()
    init {
        KonanUtil::class.java.getResourceAsStream("/konan-props-copy.properties")?.reader(Charsets.UTF_8).use {
            konanProps.load(it)
        }
    }

    // visible for testing
    fun loadPropertyWithKeySubstitution(key: String): String {
        val value = require(key)
        // see if it has any substitution
        return value.replace(regex) { match ->
            loadPropertyWithKeySubstitution(match.value.substringAfter('$'))
        }
    }

    fun require(key: String): String {
        // use user insteaad of dev, see commit 8866ab5916c5a7dc2d2ee3579e32e95de660f3ad
        // in kotlin repo
        @Suppress("NAME_SHADOWING") // intentional
        val key = key.replace(".dev", ".user")
        val value = konanProps[key] ?: error("cannot find required property: $key")
        check(value is String) {
            "expected String, found $value (${value::class.java}"
        }
        return value
    }

    fun targetTriple(target: KonanTarget): String {
        return loadPropertyWithKeySubstitution("targetTriple.${target.name}")
    }

    fun sysroot(target: KonanTarget): String {
        return loadPropertyWithKeySubstitution("targetSysRoot.${target.name}")
    }

    fun clangFlags(target: KonanTarget): String {
        return loadPropertyWithKeySubstitution("clangFlags.${target.name}")
    }

    fun linkerKonanFlags(target: KonanTarget): String {
        return loadPropertyWithKeySubstitution("linkerKonanFlags.${target.name}")
    }

    fun llvmHome(target: KonanTarget): String {
        return loadPropertyWithKeySubstitution("llvmHome.${target.name}")
    }
}
