plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.intellij.sdk"
version = "0.3.2"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.6")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("java", "org.jetbrains.kotlin"))
}

dependencies {
    implementation(project("core"))
    implementation(project("nasm"))
    implementation("com.github.zhkl0228:capstone:3.1.8") {
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.scijava", module = "native-lib-loader")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("243.*")
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
