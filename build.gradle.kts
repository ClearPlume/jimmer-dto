import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "net.fallingangel"
version = "0.0.7.31"

val sinceVersion by extra("2022.3")
val untilVersion by extra("2024.3")
val jimmerVersion by extra("0.8.150")

val certificateChainValue = findProperty("certificateChainValue") as String?
val privateKeyValue = findProperty("privateKeyValue") as String?
val passwordValue = findProperty("passwordValue") as String?
val tokenValue = findProperty("tokenValue") as String?

sourceSets["main"].java.srcDir("src/main/gen")

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", sinceVersion)
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testCompileOnly("org.babyfish.jimmer:jimmer-sql-kotlin:$jimmerVersion")
}

intellijPlatform {
    pluginConfiguration {
        id = "net.fallingangel.jimmer-dto"
        name = "JimmerDTO"

        vendor {
            name = "the_FallenAngel"
            email = "the.fallenangel.965@gmail.com"
            url = "https://fallingangel.net"
        }

        ideaVersion {
            sinceBuild = "223"
            untilBuild = "243.*"
        }
    }

    pluginVerification {
        ides {
            ide("IC", sinceVersion)
        }
    }

    publishing {
        token = tokenValue
        channels.add("Stable")
    }

    signing {
        certificateChain = certificateChainValue
        privateKey = privateKeyValue
        password = passwordValue
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }

    test {
        systemProperty("idea.home.path", intellijPlatform.sandboxContainer.get().toString())
    }

    runIde {
        jvmArgs("-Xms128m", "-Xmx4096m", "-XX:ReservedCodeCacheSize=512m")
    }
}
