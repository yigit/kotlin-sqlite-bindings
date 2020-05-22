import com.birbit.ksqlite.build.Dependencies
import com.birbit.ksqlite.build.SqliteCompilationConfig
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG

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
    kotlin("multiplatform") //version "1.3.72"
}

group = "com.birbit"
version = "0.1-SNAPSHOT"

repositories {
    maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")
    mavenCentral()
}

kotlin {
    val native = setupNative("native") {
        binaries {
            sharedLib(namePrefix = "myjni")
        }
        compilations["main"].cinterops.create("jni") {
            // JDK is required here, JRE is not enough
            val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
            println("java home:$javaHome")
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
//
//    targets.withType<KotlinNativeTarget> {
//        println("have native target $this")
//        val nativeTarget = this
//
//        binaries.findSharedLib(namePrefix = "myjni", buildType = NativeBuildType.DEBUG)!!.let { sharedLib ->
//            println("have shared lib $sharedLib")
//            val compilation = jvm().compilations["main"]
//            compilation.defaultSourceSet {
//                resources.srcDir(sharedLib.outputDirectory)
//            }
//            compilation.compileKotlinTask.dependsOn(sharedLib.linkTask)
//        }
//    }

    //val nativeLib = native.binaries.findSharedLib(namePrefix = "myjni", buildType = "debug")!!
//
//    jvm {
//        this.compilations.forEach { compilation ->
//            // TODO get correct build type per compilation
//            compilation.compileKotlinTask.dependsOn(
//                nativeLib.linkTask
//            )
//        }
//    }


    val combinedSharedLibsFolder = project.buildDir.resolve("combinedSharedLibs")
    val combineSharedLibsTask = com.birbit.ksqlite.build.CollectNativeLibrariesTask.Companion.create(project, "myjni", combinedSharedLibsFolder)
    jvm().compilations["main"].compileKotlinTask.dependsOn(combineSharedLibsTask)
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
        native.compilations["main"].defaultSourceSet {
//            dependencies {
//                implementation(files(File(project.projectDir, "sqlite/libsqlite3.a")))
//            }
        }
        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(Dependencies.NATIVE_LIB_LOADER)
            }
            // TODO get correct build type, join this w/ task dependency (gradle does not seem to discover via this)
//            val nativeLib = native.binaries.findSharedLib(namePrefix = "myjni", buildType = "debug")!!
            //this.resources.srcDir(nativeLib.outputDirectory)
//            this.resources.srcDir(sharedLibOutput)
            resources.srcDir(combinedSharedLibsFolder)
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
com.birbit.ksqlite.build.SqliteCompilation.setup(project,
SqliteCompilationConfig(
    version = "3.31.1"
))
