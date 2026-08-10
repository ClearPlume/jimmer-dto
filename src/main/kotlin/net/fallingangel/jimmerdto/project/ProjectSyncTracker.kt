package net.fallingangel.jimmerdto.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker

@Service(Service.Level.PROJECT)
class ProjectSyncTracker : SimpleModificationTracker() {
    companion object {
        fun getInstance(project: Project): ProjectSyncTracker = project.service()
    }
}