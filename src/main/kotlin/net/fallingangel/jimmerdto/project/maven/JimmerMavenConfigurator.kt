package net.fallingangel.jimmerdto.project.maven

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import net.fallingangel.jimmerdto.project.sourceroot.DtoSourceRootType
import net.fallingangel.jimmerdto.project.sourceroot.JimmerDtoDirs
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator
import org.jetbrains.idea.maven.importing.StandardMavenModuleType
import org.jetbrains.idea.maven.model.MavenPlugin

@Suppress("UnstableApiUsage")
class JimmerMavenConfigurator : MavenWorkspaceConfigurator {
    override fun configureMavenProject(context: MavenWorkspaceConfigurator.MutableMavenProjectContext) {
        val projectWithModules = context.mavenProjectWithModules
        val mavenProject = projectWithModules.mavenProject
        val projectDir = mavenProject.directory
        val plugin = mavenProject.findPlugin("org.apache.maven.plugins", "maven-compiler-plugin") ?: return
        val (main, test) = JimmerDtoDirs(parseJSR269Args(plugin.compilerArgs()))

        for (moduleWithType in projectWithModules.modules) {
            val (mainDirs, testDirs) = when (moduleWithType.type) {
                StandardMavenModuleType.SINGLE_MODULE -> main to test
                StandardMavenModuleType.MAIN_ONLY -> main to emptyList()
                StandardMavenModuleType.TEST_ONLY -> emptyList<String>() to test
                StandardMavenModuleType.AGGREGATOR, StandardMavenModuleType.COMPOUND_MODULE -> continue
            }
            val root = moduleWithType.module.contentRoots.find { "$projectDir/src".startsWith(it.url.presentableUrl) } ?: continue
            context.storage.modifyContentRootEntity(root) {
                sourceRoots += mainDirs.map { root.sourceRoot(it, DtoSourceRootType.SOURCE.typeId) } +
                        testDirs.map { root.sourceRoot(it, DtoSourceRootType.TEST_SOURCE.typeId) }
            }
        }
    }

    private fun MavenPlugin.compilerArgs(): List<String> {
        return configurationElement
            ?.getChild("compilerArgs")
            ?.getChildren("arg")
            ?.map { it.value }
            .orEmpty()
    }

    private fun parseJSR269Args(args: List<String>): Map<String, String> {
        return args
            .filter { it.startsWith("-A") }
            .mapNotNull {
                val parts = it.removePrefix("-A").split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun ContentRootEntity.sourceRoot(dir: String, typeId: String): SourceRootEntity.Builder {
        return SourceRootEntity(
            url.append(dir),
            SourceRootTypeId(typeId),
            entitySource,
        )
    }
}