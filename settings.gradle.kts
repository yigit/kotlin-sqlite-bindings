pluginManagement {
    repositories {
        maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")

        mavenCentral()

        maven("https://plugins.gradle.org/m2/")
    }
}
include("sqlitebindings", "jnigenerator")

