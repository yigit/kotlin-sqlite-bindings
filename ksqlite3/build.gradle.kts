
import com.birbit.ksqlite.build.setupCommon
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun KotlinMultiplatformExtension.setupNative(
    configure: KotlinNativeTarget.() -> Unit
) {
    val os = getCurrentOperatingSystem()
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
plugins {
    kotlin("multiplatform") // version "1.3.72"
}

group = "com.birbit"
version = "0.1-SNAPSHOT"

repositories {
    maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")
    mavenCentral()
}

kotlin {
    setupCommon(gradle) {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":sqlitebindings"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
