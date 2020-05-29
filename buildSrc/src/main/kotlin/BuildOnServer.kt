package com.birbit.ksqlite.build

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

object BuildOnServer {
    private lateinit var rootProject : Project
    private lateinit var distDir : File
    val TASK_NAME = "buildOnServer"
    fun init(project: Project) {
        rootProject = project.rootProject
        distDir = rootProject.buildDir.resolve("dist")
        val buildOnServerTask = rootProject.tasks.register(TASK_NAME)
        rootProject.subprojects { subProject ->
            if (subProject.name != "jnigenerator") {
                buildOnServerTask.configure {
                    it.dependsOn(subProject.tasks.named("spotlessCheck"))
                    it.dependsOn(subProject.tasks.named("allTests"))
                    if (subProject.pluginManager.hasPlugin("maven-publish")) {
                        it.dependsOn(subProject.tasks.named("publish"))
                    }
                }
            }
        }
        configureCopyNativeLibraries()
        Publishing.createCombinedRepoTaskIfPossible(rootProject)
    }
    fun getOutRepo(): File {
        return distDir.resolve("repo")
    }

    private fun configureCopyNativeLibraries() {
        val copyNativeLibsTask = rootProject.tasks.register("copyNativeLibs", Copy::class.java) { copyTask ->
            rootProject.childProjects["sqlitebindings"]!!.tasks.withType(CollectNativeLibrariesTask::class.java)
                .all {
                    copyTask.from(it.outputDir)
                    copyTask.dependsOn(it)
                }
            copyTask.destinationDir = distDir.resolve("nativeLibs")
        }
        rootProject.tasks.named(TASK_NAME).configure {
            it.dependsOn(copyNativeLibsTask)
        }
    }
}