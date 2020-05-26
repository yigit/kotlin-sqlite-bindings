plugins {
    id("com.diffplug.gradle.spotless") version "4.0.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "com.diffplug.gradle.spotless")
    this.extensions.getByType(com.diffplug.gradle.spotless.SpotlessExtension::class).apply {
        kotlin {
            ktlint()
        }
        kotlinGradle {
            ktlint()
        }
    }
}