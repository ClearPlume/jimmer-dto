package net.fallingangel.jimmerdto.project.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import net.fallingangel.jimmerdto.project.JimmerOptionsHolder

val JimmerOptionsKey = Key.create(JimmerOptionsPayload::class.java, ProjectKeys.CONTENT_ROOT.processingWeight + 1)

class JimmerOptionsDataService : AbstractProjectDataService<JimmerOptionsPayload, Module>() {
    override fun getTargetDataKey(): Key<JimmerOptionsPayload> {
        return JimmerOptionsKey
    }

    override fun importData(
        toImport: Collection<DataNode<JimmerOptionsPayload>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        toImport.forEach { node ->
            val moduleData = node.parent?.data as? ModuleData ?: return@forEach
            val module = modelsProvider.findIdeModule(moduleData) ?: return@forEach
            module.service<JimmerOptionsHolder>().raw = node.data.raw
        }
    }
}
