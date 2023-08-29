plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.14.1"
}

group = "net.fallingangel"
version = "0.0.1"

val sinceVersion by extra("203")
val untilVersion by extra("232.*")

val certificateChainValue: String = findProperty("certificateChainValue") as String
val privateKeyValue: String = findProperty("privateKeyValue") as String
val passwordValue: String = findProperty("passwordValue") as String
val tokenValue: String = findProperty("tokenValue") as String

sourceSets["main"].java.srcDir("src/main/gen")

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("JimmerDTO")
    version.set("2020.3.4")
    type.set("IC") // Target IDE Platform
    plugins.set(
        listOf(
            // "com.intellij.database",
            "com.intellij.java"
        )
    )
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set(sinceVersion)
        untilBuild.set(untilVersion)
    }

    runIde {
        jvmArgs("-Xms128m", "-Xmx4096m", "-XX:ReservedCodeCacheSize=512m")
    }

    signPlugin {
        certificateChain.set(certificateChainValue)
        privateKey.set(privateKeyValue)
        password.set(passwordValue)
    }

    publishPlugin {
        channels.add("Stable")
        token.set(tokenValue)
    }
}