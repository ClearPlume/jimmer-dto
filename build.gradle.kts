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
version = "0.0.7.50"

// 构建依赖
val ideVersion = "2024.3"

// 插件兼容范围
val sinceVersion = "243"
val untilVersion = "262.*"

val userHome: String = System.getProperty("user.home")
val certificateChainFileValue = file("$userHome/.gradle/chain.crt")
val privateKeyFileValue = file("$userHome/.gradle/private.pem")

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(ideVersion)
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle", "org.jetbrains.idea.maven")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Bundled)
    }

    antlr(libs.antlr4) {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation(libs.antlr4.runtime)
    implementation(libs.antlr4.intellij.adaptor)
    implementation(project(":gradle-tooling"))

    testImplementation(libs.junit)
    testImplementation(libs.jimmer.sql.kotlin)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xexplicit-context-arguments")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
            sinceBuild = sinceVersion
            untilBuild = untilVersion
        }
    }

    pluginVerification {
        ides.create {
            version = ideVersion
        }
    }

    signing {
        certificateChainFile = certificateChainFileValue
        privateKeyFile = privateKeyFileValue
        password = providers.gradleProperty("intellijPlatformSigningPassword")
    }

    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
        channels.add("Stable")
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
            "-Didea.kotlin.plugin.use.k2=true",
        )
    }

    test {
        systemProperty("idea.kotlin.plugin.use.k2", "true")
    }

    buildSearchableOptions {
        jvmArgs("-Didea.kotlin.plugin.use.k2=true")
    }
}
