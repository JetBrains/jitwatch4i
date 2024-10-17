plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.17.4"
  id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.adoptopenjdk.jitwatch"
version = "0.0.1"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
}

intellij {
  version.set("2023.3.7")
}

javafx {
  version = "19"
  modules("javafx.controls", "javafx.swing")
}

dependencies {
  implementation(project("ui"))
  implementation(project("core"))
}

tasks {
  buildSearchableOptions {
    enabled = false
  }

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set("233")
    untilBuild.set("242.*")
  }

  runIde {
    jvmArgs("-Xmx8G", "-Xms2G", "-XX:+AllowEnhancedClassRedefinition")
  }
}
