package net.fallingangel.jimmerdto.project.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

val JimmerDtoDirsKey = Key.create(JimmerDtoRoots::class.java, ProjectKeys.CONTENT_ROOT.processingWeight + 1)

class DtoSourceRootDataService : AbstractProjectDataService<JimmerDtoRoots, Module>() {
    override fun getTargetDataKey(): Key<JimmerDtoRoots> {
        return JimmerDtoDirsKey
    }

    override fun importData(
        toImport: Collection<DataNode<JimmerDtoRoots>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        toImport.forEach { node ->
            val moduleData = node.parent?.data as? ModuleData ?: return@forEach
            val module = modelsProvider.findIdeModule(moduleData) ?: return@forEach
            val rootModel = modelsProvider.getModifiableRootModel(module)
            val basePath = moduleData.linkedExternalProjectPath
            val (dirs, type) = node.data

            dirs.forEach { dir ->
                val url = "file://$basePath/$dir"
                val entry = rootModel.contentEntries.find { url == it.url || url.startsWith("${it.url}/") } ?: return@forEach
                entry.addSourceFolder(url, type, type.createDefaultProperties())
            }
        }
    }
}