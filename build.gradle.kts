import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "org.intellij.sdk"
version = "0.3.8"


repositories {
    mavenCentral()
    intellijPlatform {           // adds the JetBrains repositories the plugin needs
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.6")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
    implementation(project(":core"))
    implementation(project(":nasm"))
    implementation("com.github.zhkl0228:capstone:3.1.8") {
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.scijava", module = "native-lib-loader")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("252.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        jvmArgs("-Xmx14G", "-Xms2G", "-XX:+AllowEnhancedClassRedefinition", "-Djna.debug_load=true", "-Djna.debug_load.jna=true")
    }
}
