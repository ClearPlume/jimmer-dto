package net.fallingangel.jimmerdto.project.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import net.fallingangel.jimmerdto.project.sourceroot.DtoSourceRootType
import net.fallingangel.jimmerdto.project.sourceroot.JimmerDtoDirs
import net.fallingangel.jimmerdto.tooling.JimmerBuildModel
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class JimmerGradleResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(JimmerBuildModel::class.java)

    override fun getToolingExtensionsClasses() = setOf(JimmerBuildModel::class.java)

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        super.populateModuleContentRoots(gradleModule, ideModule)

        val model = resolverCtx.getExtraProject(gradleModule, JimmerBuildModel::class.java)
        val dirs = JimmerDtoDirs(model?.options() ?: emptyMap())

        ideModule.children
            .filter { it.data is GradleSourceSetData }
            .forEach { node ->
                val sourceSet = (node.data as GradleSourceSetData).moduleName
                val payload = when (sourceSet) {
                    "main" -> JimmerDtoRoots(dirs.mainDirs, DtoSourceRootType.SOURCE.typeId)
                    "test" -> JimmerDtoRoots(dirs.testDirs, DtoSourceRootType.TEST_SOURCE.typeId)
                    else -> return@forEach
                }
                node.createChild(JimmerDtoDirsKey, payload)
            }
    }
}