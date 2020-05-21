import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

fun KotlinMultiplatformExtension.setupNative(
    name: String,
    configure: KotlinNativeTargetWithTests<*>.() -> Unit
): KotlinNativeTargetWithTests<*> {
    val os = getCurrentOperatingSystem()
    return when {
        os.isLinux -> linuxX64(name, configure)
        os.isWindows -> mingwX64(name, configure)
        os.isMacOsX -> macosX64(name, configure)
        else -> error("OS $os is not supported")
    }
}
plugins {
    kotlin("multiplatform") version "1.3.72"
}

group = "com.birbit"
version = "0.1-SNAPSHOT"

repositories {
    maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")
    mavenCentral()
}

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    val native = setupNative("native") {
        binaries {
            //sharedLib()
            sharedLib(namePrefix = "myjni")
//            staticLib(namePrefix= "myjni")
        }
        compilations["main"].cinterops.create("jni") {
            // JDK is required here, JRE is not enough
            val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
            var include = File(javaHome, "include")
            if (!include.exists()) {
                // look upper
                include = File(javaHome, "../include")
            }
            if (!include.exists()) {
                throw GradleException("cannot find include")
            }
            packageName = "com.birbit.jni"
            includeDirs(
                Callable { include },
                Callable { File(include, "darwin") },
                Callable { File(include, "linux") },
                Callable { File(include, "win32") }
            )
        }
    }
    jvm {
        this.compilations.forEach { compilation ->
            // TODO get correct build type per compilation
            val nativeLib = native.binaries.findSharedLib(namePrefix = "myjni", buildType = "debug")!!
            compilation.compileKotlinTask.dependsOn(
                nativeLib.linkTask
            )
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
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
            // TODO get correct build type, join this w/ task dependency (gradle does not seem to discover via this)
            val nativeLib = native.binaries.findSharedLib(namePrefix = "myjni", buildType = "debug")!!
            this.resources.srcDir(nativeLib.outputDirectory)
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

