package com.birbit.ksqlite.build

import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.get
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun KotlinMultiplatformExtension.setupNative(
    gradle:Gradle,
    configure: KotlinNativeTarget.() -> Unit
) {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    when {
        os.isLinux -> {
            linuxX64(configure = configure)
            if (!gradle.startParameter.systemPropertiesArgs.containsKey("idea.active")) {
                linuxArm32Hfp(configure = configure)
            }
        }
        os.isWindows -> {
            mingwX64(configure = configure)
        }
        os.isMacOsX -> {
            macosX64(configure = configure)
        }
        else -> error("OS $os is not supported")
    }
}

fun KotlinMultiplatformExtension.setupCommon(
    gradle: Gradle,
    configure: KotlinNativeTarget.() -> Unit
) {
    setupNative(gradle) {
        compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain")
        }
        compilations["test"].defaultSourceSet {
            kotlin.srcDir("src/nativeTest")
        }
        this.configure()
    }
}