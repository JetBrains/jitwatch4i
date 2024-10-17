plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.intellij.sdk"
version = "2.0.0"

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
    modules("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")  // JUnit 4 dependency
    testImplementation("org.junit.vintage:junit-vintage-engine")  // JUnit 4 engine in JUnit 5
    implementation(project(":core"))
}

tasks.test {
    useJUnitPlatform()
}