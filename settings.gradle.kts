pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.0.20"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "jitwatch4i"
include("core")
include("nasm")
