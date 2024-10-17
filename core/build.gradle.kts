plugins {
    id("java")
}

group = "org.intellij.sdk"
version = "2.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")  // JUnit 4 dependency
    testImplementation("org.junit.vintage:junit-vintage-engine")  // JUnit 4 engine in JUnit 5
}

tasks.test {
    useJUnitPlatform()
}