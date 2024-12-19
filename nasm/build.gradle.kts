plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.intellij.sdk"
version = "0.6.0"

intellij {
    version.set("2023.2.6")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java", "gen"))
        }
        resources {
            setSrcDirs(listOf("resources"))
        }
    }
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}