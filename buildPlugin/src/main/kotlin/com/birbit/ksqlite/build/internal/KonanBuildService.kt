package com.birbit.ksqlite.build.internal

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

abstract internal class KonanBuildService
    @Inject constructor(
        private val execOperations: ExecOperations
    )
    : BuildService<KonanBuildService.Params> {
    interface Params : BuildServiceParameters {
        val compilerPath: Property<File>
    }

    fun obtainWrapper(
        konanTarget: KonanTarget
    ) = KonanUtil.KonanCompilerWrapper(
        execOperations = execOperations,
        konanTarget = konanTarget
    ).also {
        KonanUtil.KonanCompilerWrapper.ensureSupportForTarget(
            parameters.compilerPath.get(),
            execOperations,
            konanTarget
        )
    }
    companion object {
        const val KEY = "konanBuildService"
        fun register(
            project: Project
        ): Provider<KonanBuildService> {
            val compilerPath = KonanUtil.KonanCompilerWrapper.obtainNativeCompiler(
                project
            )
            return project.gradle.sharedServices.registerIfAbsent(
                KEY,
                KonanBuildService::class.java
            ) {
                it.parameters.compilerPath.set(compilerPath)
            }
        }

        private fun findSupportedKonanTargets(): List<KonanTarget> {
            val os = DefaultNativePlatform.getCurrentOperatingSystem()
            return KonanTarget.predefinedTargets.values.filter {
                when {
                    it.family == Family.OSX -> os.isMacOsX
                    it.family == Family.IOS -> os.isMacOsX
                    it.family== Family.MINGW -> true
                    it.family == Family.ANDROID -> true
                    it.family == Family.LINUX -> true
                    else -> false
                }
            }
        }
    }
}