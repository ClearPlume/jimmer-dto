plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.intellij.gradle.tooling.extension.api) {
        isTransitive = false
    }
}

tasks.withType<JavaCompile> {
    options.release = 17
}