import com.birbit.ksqlite.build.Dependencies.KOTLIN_POET

plugins {
    kotlin("jvm") //version "1.3.72"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project.fileTree("libs") {
        include("*.jar")
    })
    implementation(KOTLIN_POET)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}