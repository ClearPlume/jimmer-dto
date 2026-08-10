package net.fallingangel.jimmerdto.project.gradle

import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import net.fallingangel.jimmerdto.project.ProjectSyncTracker

class GradleSyncListener(private val project: Project) : ProjectDataImportListener {
    override fun onImportFinished(projectPath: String?) {
        ProjectSyncTracker.getInstance(project).incModificationCount()
    }
}