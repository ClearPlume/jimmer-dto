import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    antlr

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.changelog)
}

group = "net.fallingangel"
version = "0.0.7.48-k1"

val since by extra("241.14494.240")
val until by extra("241.*")
val jimmerVersion by extra("0.10.6")
val antlrVersion by extra("4.13.2")

val certificateChainValue = findProperty("certificateChainValue") as String?
val privateKeyValue = findProperty("privateKeyValue") as String?
val passwordValue = findProperty("passwordValue") as String?
val tokenValue = findProperty("tokenValue") as String?

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(since) {
            useInstaller = false
        }
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")

        testFramework(TestFrameworkType.Platform)
    }

    antlr(libs.antlr4) {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation(libs.antlr4.runtime)
    implementation(libs.antlr4.intellij.adaptor)
    implementation(libs.jimmer.core)

    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

changelog {
    keepUnreleasedSection = false
    unreleasedTerm = "Unreleased"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed")
    headerParserRegex =
        """^((0|[1-9]\d*)(\.(0|[1-9]\d*)){2,3}(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)$"""
}

intellijPlatform {
    pluginConfiguration {
        id = "net.fallingangel.jimmer-dto"
        name = "JimmerDTO"

        description = markdownToHTML(File(projectDir, "README.md").readText())
        changeNotes = changelog.render(Changelog.OutputType.HTML)

        vendor {
            name = "the_FallenAngel"
            email = "the.fallenangel.965@gmail.com"
            url = "https://fallingangel.net"
        }

        ideaVersion {
            sinceBuild = since
            untilBuild = until
        }
    }

    pluginVerification {
        ides.recommended()
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
    withType<KotlinCompile> {
        dependsOn("generateGrammarSource")
    }

    runIde {
        jvmArgs(
            "-Xms128m",
            "-Xmx4096m",
            "-Didea.ProcessCanceledException=disabled",
        )
    }

    test {
        systemProperty("idea.home.path", intellijPlatform.sandboxContainer.get().toString())
    }
}
